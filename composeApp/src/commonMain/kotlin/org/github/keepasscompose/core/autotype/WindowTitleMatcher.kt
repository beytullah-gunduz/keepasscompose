package org.github.keepasscompose.core.autotype

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Matches KeePass entries against the active window title for auto-type.
 *
 * KeePass entries can specify window title matching patterns via the
 * `AutoType-Window` custom field. Patterns support:
 * - `*` wildcard matching (matches any sequence of characters)
 * - `//regex//` for regex matching
 * - Plain text for case-insensitive substring matching
 *
 * If no `AutoType-Window` field is set, the entry's title is used as the
 * match pattern (with implicit wildcards on both sides).
 */
object WindowTitleMatcher {
    private const val AUTO_TYPE_WINDOW_FIELD = "AutoType-Window"

    /**
     * Result of matching an entry against a window title.
     */
    data class MatchResult(val entry: KdbxEntry, val groupPath: List<String>, val sequence: String)

    /**
     * Find all entries matching the given window title.
     *
     * @param rootGroup The root group to search recursively.
     * @param windowTitle The active window's title.
     * @return List of matching entries with their auto-type sequences.
     */
    fun findMatches(rootGroup: KdbxGroup, windowTitle: String): List<MatchResult> {
        val results = mutableListOf<MatchResult>()
        findMatchesInGroup(rootGroup, windowTitle, emptyList(), results)
        return results
    }

    /**
     * Check if a single pattern matches a window title.
     *
     * @param pattern The match pattern (supports *, //regex//).
     * @param windowTitle The window title to match against.
     * @return true if the pattern matches.
     */
    fun matches(pattern: String, windowTitle: String): Boolean {
        if (pattern.isBlank() || windowTitle.isBlank()) return false

        return when {
            // Regex pattern: //pattern//
            pattern.startsWith("//") && pattern.endsWith("//") && pattern.length > 4 -> {
                val regexStr = pattern.substring(2, pattern.length - 2)
                try {
                    Regex(regexStr, RegexOption.IGNORE_CASE).containsMatchIn(windowTitle)
                } catch (_: Exception) {
                    false
                }
            }

            // Wildcard pattern: contains *
            pattern.contains('*') -> {
                wildcardMatch(pattern, windowTitle)
            }

            // Plain text: case-insensitive substring
            else -> {
                windowTitle.contains(pattern, ignoreCase = true)
            }
        }
    }

    private fun findMatchesInGroup(group: KdbxGroup, windowTitle: String, path: List<String>, results: MutableList<MatchResult>) {
        val currentPath = path + group.name
        for (entry in group.entries) {
            val patterns = getWindowPatterns(entry)
            val sequence = AutoTypeSequenceParser.getSequenceForEntry(entry)

            for (pattern in patterns) {
                if (matches(pattern, windowTitle)) {
                    results.add(MatchResult(entry, currentPath, sequence))
                    break // One match per entry is enough
                }
            }
        }
        for (subgroup in group.groups) {
            findMatchesInGroup(subgroup, windowTitle, currentPath, results)
        }
    }

    /**
     * Get the window title matching patterns for an entry.
     * Falls back to the entry title if no AutoType-Window field is set.
     */
    internal fun getWindowPatterns(entry: KdbxEntry): List<String> {
        // Check for AutoType-Window fields (can be multiple)
        val customPatterns = entry.fields
            .filter { it.key == AUTO_TYPE_WINDOW_FIELD }
            .map { it.value }
            .filter { it.isNotBlank() }

        if (customPatterns.isNotEmpty()) return customPatterns

        // Fall back to entry title
        val title = entry.title
        return if (title.isNotBlank()) listOf("*$title*") else emptyList()
    }

    /**
     * Simple wildcard matching where `*` matches any sequence of characters.
     * Case-insensitive.
     */
    internal fun wildcardMatch(pattern: String, text: String): Boolean {
        // Convert wildcard pattern to regex
        val regexStr = buildString {
            append("^")
            for (char in pattern) {
                when (char) {
                    '*' -> append(".*")

                    '?' -> append(".")

                    '.', '\\', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|' -> {
                        append("\\")
                        append(char)
                    }

                    else -> append(char)
                }
            }
            append("$")
        }
        return try {
            Regex(regexStr, RegexOption.IGNORE_CASE).matches(text)
        } catch (_: Exception) {
            false
        }
    }
}
