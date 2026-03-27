package org.github.keepasscompose.core.browser

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.time.ExperimentalTime

/**
 * Matches database entries against URLs for browser credential autofill.
 *
 * Supports multiple matching strategies:
 * - **Exact**: URL must match the entry URL exactly
 * - **Domain**: Domain (including subdomains) must match
 * - **Host**: Hostname must match exactly (no subdomain flexibility)
 * - **StartsWith**: Entry URL must be a prefix of the request URL
 *
 * The default strategy is [MatchStrategy.DOMAIN] which provides a good
 * balance between security and usability — matching `login.example.com`
 * against an entry stored for `example.com`.
 */
class CredentialMatcher(private val strategy: MatchStrategy = MatchStrategy.DOMAIN) {

    enum class MatchStrategy {
        EXACT,
        DOMAIN,
        HOST,
        STARTS_WITH,
    }

    /**
     * A matched credential with relevance score for sorting.
     */
    data class MatchResult(val entry: KdbxEntry, val groupPath: List<String>, val score: Int) : Comparable<MatchResult> {
        override fun compareTo(other: MatchResult): Int = other.score.compareTo(score)
    }

    /**
     * Find all entries matching the given URL, sorted by relevance.
     *
     * @param rootGroup The root group to search recursively.
     * @param url The URL to match against entry URLs.
     * @param includeExpired Whether to include expired entries.
     * @return Sorted list of matching entries (best match first).
     */
    fun findMatches(rootGroup: KdbxGroup, url: String, includeExpired: Boolean = false): List<MatchResult> {
        val parsed = parseUrl(url) ?: return emptyList()
        val results = mutableListOf<MatchResult>()
        findMatchesInGroup(rootGroup, parsed, includeExpired, emptyList(), results)
        return results.sorted()
    }

    /**
     * Convert matched entries to browser protocol format.
     */
    fun toBrowserEntries(matches: List<MatchResult>): List<BrowserEntry> = matches.map { match ->
        BrowserEntry(
            login = match.entry.userName,
            password = match.entry.password,
            name = match.entry.title,
            uuid = match.entry.uuid,
            group = match.groupPath.lastOrNull(),
            expired = if (match.entry.expires) isExpired(match.entry) else null,
            stringFields = match.entry.fields
                .filter { it.key !in STANDARD_FIELDS && !it.isProtected }
                .map { BrowserStringField(key = it.key, value = it.value) }
                .ifEmpty { null },
        )
    }

    private fun findMatchesInGroup(
        group: KdbxGroup,
        targetUrl: ParsedUrl,
        includeExpired: Boolean,
        path: List<String>,
        results: MutableList<MatchResult>,
    ) {
        val currentPath = path + group.name
        for (entry in group.entries) {
            if (!includeExpired && entry.expires && isExpired(entry)) continue

            val entryUrl = entry.url
            if (entryUrl.isBlank()) continue

            val score = calculateMatchScore(entryUrl, targetUrl)
            if (score > 0) {
                results.add(MatchResult(entry, currentPath, score))
            }
        }
        for (subgroup in group.groups) {
            findMatchesInGroup(subgroup, targetUrl, includeExpired, currentPath, results)
        }
    }

    internal fun calculateMatchScore(entryUrl: String, targetUrl: ParsedUrl): Int {
        val entryParsed = parseUrl(entryUrl) ?: return 0

        return when (strategy) {
            MatchStrategy.EXACT -> {
                if (normalizeUrl(entryUrl) == normalizeUrl(targetUrl.original)) SCORE_EXACT else 0
            }

            MatchStrategy.HOST -> {
                if (entryParsed.host.equals(targetUrl.host, ignoreCase = true)) {
                    scoreByPathMatch(entryParsed, targetUrl)
                } else {
                    0
                }
            }

            MatchStrategy.DOMAIN -> {
                val entryDomain = extractDomain(entryParsed.host)
                val targetDomain = extractDomain(targetUrl.host)
                if (entryDomain.equals(targetDomain, ignoreCase = true)) {
                    scoreByPathMatch(entryParsed, targetUrl)
                } else {
                    0
                }
            }

            MatchStrategy.STARTS_WITH -> {
                val normalizedEntry = normalizeUrl(entryUrl)
                val normalizedTarget = normalizeUrl(targetUrl.original)
                if (normalizedTarget.startsWith(normalizedEntry)) {
                    SCORE_STARTS_WITH + normalizedEntry.length
                } else {
                    0
                }
            }
        }
    }

    private fun scoreByPathMatch(entryParsed: ParsedUrl, targetUrl: ParsedUrl): Int {
        var score = SCORE_DOMAIN

        // Exact host match is better than domain-only match
        if (entryParsed.host.equals(targetUrl.host, ignoreCase = true)) {
            score += SCORE_HOST_BONUS
        }

        // Scheme match bonus
        if (entryParsed.scheme.equals(targetUrl.scheme, ignoreCase = true)) {
            score += SCORE_SCHEME_BONUS
        }

        // Port match bonus
        if (entryParsed.port == targetUrl.port) {
            score += SCORE_PORT_BONUS
        }

        // Path prefix match bonus
        if (entryParsed.path.isNotEmpty() && entryParsed.path != "/") {
            if (targetUrl.path.startsWith(entryParsed.path, ignoreCase = true)) {
                score += SCORE_PATH_BONUS + entryParsed.path.length
            }
        }

        return score
    }

    companion object {
        private const val SCORE_EXACT = 1000
        private const val SCORE_DOMAIN = 100
        private const val SCORE_HOST_BONUS = 50
        private const val SCORE_SCHEME_BONUS = 10
        private const val SCORE_PORT_BONUS = 5
        private const val SCORE_PATH_BONUS = 20
        private const val SCORE_STARTS_WITH = 80

        private val STANDARD_FIELDS = setOf("Title", "UserName", "Password", "URL", "Notes")
    }

    /**
     * Parsed URL components.
     */
    internal data class ParsedUrl(val original: String, val scheme: String, val host: String, val port: Int?, val path: String)

    internal fun parseUrl(url: String): ParsedUrl? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        // Handle URLs without scheme
        val withScheme = if (!trimmed.contains("://")) "https://$trimmed" else trimmed

        return try {
            val schemeEnd = withScheme.indexOf("://")
            val scheme = withScheme.substring(0, schemeEnd)
            val rest = withScheme.substring(schemeEnd + 3)

            val pathStart = rest.indexOf('/')
            val hostPort = if (pathStart >= 0) rest.substring(0, pathStart) else rest
            val path = if (pathStart >= 0) rest.substring(pathStart) else "/"

            // Strip query string and fragment from path
            val cleanPath = path.substringBefore('?').substringBefore('#')

            val portSep = hostPort.lastIndexOf(':')
            val host: String
            val port: Int?
            if (portSep > 0 && portSep < hostPort.length - 1) {
                host = hostPort.substring(0, portSep)
                port = hostPort.substring(portSep + 1).toIntOrNull()
            } else {
                host = hostPort
                port = null
            }

            if (host.isBlank()) return null

            ParsedUrl(
                original = trimmed,
                scheme = scheme,
                host = host.lowercase(),
                port = port,
                path = cleanPath,
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun extractDomain(host: String): String {
        val parts = host.lowercase().split('.')
        if (parts.size <= 2) return host.lowercase()

        // Handle common TLDs with second-level domains (e.g., co.uk, com.au)
        val lastTwo = "${parts[parts.size - 2]}.${parts.last()}"
        return if (lastTwo in COMPOUND_TLDS) {
            if (parts.size >= 3) "${parts[parts.size - 3]}.$lastTwo" else host.lowercase()
        } else {
            "${parts[parts.size - 2]}.${parts.last()}"
        }
    }

    private fun normalizeUrl(url: String): String = url.trim().lowercase().removeSuffix("/")

    @OptIn(ExperimentalTime::class)
    private fun isExpired(entry: KdbxEntry): Boolean {
        if (!entry.expires || entry.expiryTime == null) return false
        return entry.expiryTime < kotlin.time.Clock.System.now()
    }
}

/**
 * Common compound TLDs where the "domain" needs an extra level.
 */
private val COMPOUND_TLDS = setOf(
    "co.uk", "co.jp", "co.kr", "co.in", "co.nz", "co.za",
    "com.au", "com.br", "com.cn", "com.mx", "com.tw", "com.sg",
    "org.uk", "org.au",
    "net.au", "net.uk",
    "ac.uk", "gov.uk",
)
