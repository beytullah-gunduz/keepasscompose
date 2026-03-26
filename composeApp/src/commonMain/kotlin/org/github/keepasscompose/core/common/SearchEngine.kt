package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Full-text search engine for KeePass entries.
 *
 * Supports:
 * - Case-insensitive matching across title, username, URL, notes, custom fields, and tags
 * - Search operators: terms are AND-ed by default; prefix with `-` to exclude
 * - Field-specific search: `title:bank`, `user:john`, `url:github`, `tag:finance`
 * - Regex search when the query starts with `regex:`
 * - Recursive group traversal
 */
object SearchEngine {
    /**
     * A search result containing the matched entry and its group path.
     */
    data class SearchResult(val entry: KdbxEntry, val groupPath: List<String>)

    /**
     * Search all entries under [rootGroup] matching the [query].
     *
     * @param rootGroup The root group to search recursively.
     * @param query The search query string.
     * @param includeProtected Whether to search protected field values (e.g., passwords).
     * @return List of matching entries with their group paths.
     */
    fun search(rootGroup: KdbxGroup, query: String, includeProtected: Boolean = false): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val matcher = parseQuery(query)
        val results = mutableListOf<SearchResult>()
        searchGroup(rootGroup, matcher, includeProtected, emptyList(), results)
        return results
    }

    /**
     * Flatten all entries from a group tree into a list with group paths.
     */
    fun allEntries(rootGroup: KdbxGroup): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        collectEntries(rootGroup, emptyList(), results)
        return results
    }

    // --- Query parsing ---

    private sealed class Matcher {
        abstract fun matches(entry: KdbxEntry, includeProtected: Boolean): Boolean
    }

    private class AndMatcher(val terms: List<Matcher>) : Matcher() {
        override fun matches(entry: KdbxEntry, includeProtected: Boolean) = terms.all { it.matches(entry, includeProtected) }
    }

    private class NotMatcher(val inner: Matcher) : Matcher() {
        override fun matches(entry: KdbxEntry, includeProtected: Boolean) = !inner.matches(entry, includeProtected)
    }

    private class TextMatcher(val text: String) : Matcher() {
        override fun matches(entry: KdbxEntry, includeProtected: Boolean) = entryContainsText(entry, text, includeProtected)
    }

    private class FieldMatcher(val field: String, val text: String) : Matcher() {
        override fun matches(entry: KdbxEntry, includeProtected: Boolean): Boolean {
            val lower = text.lowercase()
            return when (field) {
                "title" -> {
                    entry.title.lowercase().contains(lower)
                }

                "user", "username" -> {
                    entry.userName.lowercase().contains(lower)
                }

                "url" -> {
                    entry.url.lowercase().contains(lower)
                }

                "notes" -> {
                    entry.notes.lowercase().contains(lower)
                }

                "tag" -> {
                    entry.tags.any { it.lowercase().contains(lower) }
                }

                else -> {
                    // Search custom field by key name
                    entry.fields.any {
                        it.key.equals(field, ignoreCase = true) &&
                            (!it.isProtected || includeProtected) &&
                            it.value.lowercase().contains(lower)
                    }
                }
            }
        }
    }

    private class RegexMatcher(val pattern: Regex) : Matcher() {
        override fun matches(entry: KdbxEntry, includeProtected: Boolean) = entryMatchesRegex(entry, pattern, includeProtected)
    }

    private fun parseQuery(query: String): Matcher {
        // Regex mode
        if (query.startsWith("regex:")) {
            val pattern = query.removePrefix("regex:")
            return RegexMatcher(Regex(pattern, RegexOption.IGNORE_CASE))
        }

        // Tokenize: split by whitespace, respecting quoted strings
        val tokens = tokenize(query)
        val matchers =
            tokens.map { token ->
                when {
                    // Negation
                    token.startsWith("-") && token.length > 1 -> {
                        NotMatcher(parseSingleTerm(token.substring(1)))
                    }

                    else -> {
                        parseSingleTerm(token)
                    }
                }
            }

        return if (matchers.size == 1) matchers[0] else AndMatcher(matchers)
    }

    private fun parseSingleTerm(term: String): Matcher {
        // Field-specific: field:value
        val colonIndex = term.indexOf(':')
        if (colonIndex > 0 && colonIndex < term.length - 1) {
            val field = term.substring(0, colonIndex).lowercase()
            val value = term.substring(colonIndex + 1)
            if (field in KNOWN_FIELDS) {
                return FieldMatcher(field, value)
            }
        }
        return TextMatcher(term)
    }

    private val KNOWN_FIELDS = setOf("title", "user", "username", "url", "notes", "tag")

    private fun tokenize(query: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < query.length) {
            // Skip whitespace
            while (i < query.length && query[i].isWhitespace()) i++
            if (i >= query.length) break

            if (query[i] == '"') {
                // Quoted string
                val end = query.indexOf('"', i + 1)
                if (end == -1) {
                    tokens.add(query.substring(i + 1))
                    break
                } else {
                    tokens.add(query.substring(i + 1, end))
                    i = end + 1
                }
            } else {
                // Unquoted token
                val start = i
                while (i < query.length && !query[i].isWhitespace()) i++
                tokens.add(query.substring(start, i))
            }
        }
        return tokens
    }

    // --- Matching helpers ---

    private fun entryContainsText(entry: KdbxEntry, text: String, includeProtected: Boolean): Boolean {
        val lower = text.lowercase()
        if (entry.title.lowercase().contains(lower)) return true
        if (entry.userName.lowercase().contains(lower)) return true
        if (entry.url.lowercase().contains(lower)) return true
        if (entry.notes.lowercase().contains(lower)) return true
        if (entry.tags.any { it.lowercase().contains(lower) }) return true
        // Search all fields (key and value)
        for (field in entry.fields) {
            if (field.key.lowercase().contains(lower)) return true
            if ((!field.isProtected || includeProtected) && field.value.lowercase().contains(lower)) return true
        }
        return false
    }

    private fun entryMatchesRegex(entry: KdbxEntry, pattern: Regex, includeProtected: Boolean): Boolean {
        if (pattern.containsMatchIn(entry.title)) return true
        if (pattern.containsMatchIn(entry.userName)) return true
        if (pattern.containsMatchIn(entry.url)) return true
        if (pattern.containsMatchIn(entry.notes)) return true
        if (entry.tags.any { pattern.containsMatchIn(it) }) return true
        for (field in entry.fields) {
            if (pattern.containsMatchIn(field.key)) return true
            if ((!field.isProtected || includeProtected) && pattern.containsMatchIn(field.value)) return true
        }
        return false
    }

    // --- Group traversal ---

    private fun searchGroup(group: KdbxGroup, matcher: Matcher, includeProtected: Boolean, path: List<String>, results: MutableList<SearchResult>) {
        val currentPath = path + group.name
        for (entry in group.entries) {
            if (matcher.matches(entry, includeProtected)) {
                results.add(SearchResult(entry, currentPath))
            }
        }
        for (subgroup in group.groups) {
            searchGroup(subgroup, matcher, includeProtected, currentPath, results)
        }
    }

    private fun collectEntries(group: KdbxGroup, path: List<String>, results: MutableList<SearchResult>) {
        val currentPath = path + group.name
        for (entry in group.entries) {
            results.add(SearchResult(entry, currentPath))
        }
        for (subgroup in group.groups) {
            collectEntries(subgroup, currentPath, results)
        }
    }
}
