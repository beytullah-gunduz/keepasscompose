package org.github.keepasscompose.core.browser

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class CredentialMatcherTest {
    private fun entry(title: String, url: String, userName: String = "user", password: String = "pass", uuid: String = title): KdbxEntry = KdbxEntry(
        uuid = uuid,
        fields = listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", userName),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", ""),
        ),
    )

    private fun group(name: String, entries: List<KdbxEntry> = emptyList(), groups: List<KdbxGroup> = emptyList()): KdbxGroup =
        KdbxGroup(uuid = name, name = name, entries = entries, groups = groups)

    // --- URL Parsing ---

    @Test
    fun parseUrlWithFullUrl() {
        val matcher = CredentialMatcher()
        val parsed = matcher.parseUrl("https://example.com/path?q=1#frag")
        assertNotNull(parsed)
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertNull(parsed.port)
        assertEquals("/path", parsed.path)
    }

    @Test
    fun parseUrlWithPort() {
        val matcher = CredentialMatcher()
        val parsed = matcher.parseUrl("http://localhost:8080/api")
        assertNotNull(parsed)
        assertEquals("localhost", parsed.host)
        assertEquals(8080, parsed.port)
        assertEquals("/api", parsed.path)
    }

    @Test
    fun parseUrlWithoutScheme() {
        val matcher = CredentialMatcher()
        val parsed = matcher.parseUrl("example.com")
        assertNotNull(parsed)
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
    }

    @Test
    fun parseUrlWithBlankReturnsNull() {
        val matcher = CredentialMatcher()
        assertNull(matcher.parseUrl(""))
        assertNull(matcher.parseUrl("   "))
    }

    // --- Domain Extraction ---

    @Test
    fun extractDomainSimple() {
        val matcher = CredentialMatcher()
        assertEquals("example.com", matcher.extractDomain("example.com"))
    }

    @Test
    fun extractDomainWithSubdomain() {
        val matcher = CredentialMatcher()
        assertEquals("example.com", matcher.extractDomain("login.example.com"))
        assertEquals("example.com", matcher.extractDomain("www.login.example.com"))
    }

    @Test
    fun extractDomainCompoundTld() {
        val matcher = CredentialMatcher()
        assertEquals("example.co.uk", matcher.extractDomain("www.example.co.uk"))
        assertEquals("example.com.au", matcher.extractDomain("login.example.com.au"))
    }

    // --- Domain Strategy Matching ---

    @Test
    fun domainStrategyMatchesSameDomain() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(entry("GitHub", "https://github.com/login")),
        )

        val results = matcher.findMatches(root, "https://github.com")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    @Test
    fun domainStrategyMatchesSubdomain() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(entry("GitLab", "https://gitlab.com")),
        )

        val results = matcher.findMatches(root, "https://projects.gitlab.com")
        assertEquals(1, results.size)
    }

    @Test
    fun domainStrategyDoesNotMatchDifferentDomain() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(entry("GitHub", "https://github.com")),
        )

        val results = matcher.findMatches(root, "https://gitlab.com")
        assertEquals(0, results.size)
    }

    // --- Exact Strategy ---

    @Test
    fun exactStrategyMatchesExactUrl() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.EXACT)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://example.com/path")),
        )

        val results = matcher.findMatches(root, "https://example.com/path")
        assertEquals(1, results.size)
    }

    @Test
    fun exactStrategyDoesNotMatchDifferentPath() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.EXACT)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://example.com/path1")),
        )

        val results = matcher.findMatches(root, "https://example.com/path2")
        assertEquals(0, results.size)
    }

    // --- Host Strategy ---

    @Test
    fun hostStrategyMatchesExactHost() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.HOST)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://login.example.com")),
        )

        val results = matcher.findMatches(root, "https://login.example.com/auth")
        assertEquals(1, results.size)
    }

    @Test
    fun hostStrategyDoesNotMatchDifferentSubdomain() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.HOST)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://www.example.com")),
        )

        val results = matcher.findMatches(root, "https://login.example.com")
        assertEquals(0, results.size)
    }

    // --- StartsWith Strategy ---

    @Test
    fun startsWithStrategyMatchesPrefix() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.STARTS_WITH)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://example.com/app")),
        )

        val results = matcher.findMatches(root, "https://example.com/app/dashboard")
        assertEquals(1, results.size)
    }

    @Test
    fun startsWithStrategyDoesNotMatchDifferentPrefix() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.STARTS_WITH)
        val root = group(
            "Root",
            entries = listOf(entry("Test", "https://example.com/api")),
        )

        val results = matcher.findMatches(root, "https://example.com/app")
        assertEquals(0, results.size)
    }

    // --- Scoring ---

    @Test
    fun exactHostScoresHigherThanSubdomainMatch() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(
                entry("Subdomain", "https://login.example.com"),
                entry("Exact", "https://example.com"),
            ),
        )

        val results = matcher.findMatches(root, "https://example.com/login")
        assertEquals(2, results.size)
        // Exact host match should score higher
        assertEquals("Exact", results[0].entry.title)
    }

    @Test
    fun pathMatchScoresHigher() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(
                entry("No Path", "https://example.com"),
                entry("With Path", "https://example.com/app"),
            ),
        )

        val results = matcher.findMatches(root, "https://example.com/app/page")
        assertEquals(2, results.size)
        assertEquals("With Path", results[0].entry.title)
    }

    // --- Recursive Search ---

    @Test
    fun searchesNestedGroups() {
        val matcher = CredentialMatcher()
        val root = group(
            "Root",
            groups = listOf(
                group(
                    "Social",
                    groups = listOf(
                        group("Coding", entries = listOf(entry("GitHub", "https://github.com"))),
                    ),
                ),
            ),
        )

        val results = matcher.findMatches(root, "https://github.com")
        assertEquals(1, results.size)
        assertEquals(listOf("Root", "Social", "Coding"), results[0].groupPath)
    }

    // --- Entries with blank URLs ---

    @Test
    fun skipsEntriesWithBlankUrl() {
        val matcher = CredentialMatcher()
        val root = group(
            "Root",
            entries = listOf(
                entry("No URL", ""),
                entry("Has URL", "https://example.com"),
            ),
        )

        val results = matcher.findMatches(root, "https://example.com")
        assertEquals(1, results.size)
        assertEquals("Has URL", results[0].entry.title)
    }

    // --- Expired entries ---

    @Test
    fun excludesExpiredByDefault() {
        val matcher = CredentialMatcher()
        val expired = KdbxEntry(
            uuid = "expired",
            fields = listOf(
                KdbxEntryField("Title", "Expired"),
                KdbxEntryField("URL", "https://example.com"),
                KdbxEntryField("UserName", "user"),
                KdbxEntryField("Password", "pass", isProtected = true),
            ),
            expires = true,
            expiryTime = kotlin.time.Clock.System.now() - 1.hours,
        )
        val root = group("Root", entries = listOf(expired))

        val results = matcher.findMatches(root, "https://example.com")
        assertEquals(0, results.size)
    }

    @Test
    fun includesExpiredWhenRequested() {
        val matcher = CredentialMatcher()
        val expired = KdbxEntry(
            uuid = "expired",
            fields = listOf(
                KdbxEntryField("Title", "Expired"),
                KdbxEntryField("URL", "https://example.com"),
                KdbxEntryField("UserName", "user"),
                KdbxEntryField("Password", "pass", isProtected = true),
            ),
            expires = true,
            expiryTime = kotlin.time.Clock.System.now() - 1.hours,
        )
        val root = group("Root", entries = listOf(expired))

        val results = matcher.findMatches(root, "https://example.com", includeExpired = true)
        assertEquals(1, results.size)
    }

    // --- toBrowserEntries ---

    @Test
    fun convertsMatchesToBrowserEntries() {
        val matcher = CredentialMatcher()
        val testEntry = entry("Test", "https://example.com", "admin", "secret")
        val root = group("Root", entries = listOf(testEntry))

        val matches = matcher.findMatches(root, "https://example.com")
        val browserEntries = matcher.toBrowserEntries(matches)

        assertEquals(1, browserEntries.size)
        assertEquals("admin", browserEntries[0].login)
        assertEquals("secret", browserEntries[0].password)
        assertEquals("Test", browserEntries[0].name)
    }

    @Test
    fun customFieldsIncludedInBrowserEntries() {
        val matcher = CredentialMatcher()
        val testEntry = KdbxEntry(
            uuid = "custom",
            fields = listOf(
                KdbxEntryField("Title", "Custom"),
                KdbxEntryField("URL", "https://example.com"),
                KdbxEntryField("UserName", "user"),
                KdbxEntryField("Password", "pass", isProtected = true),
                KdbxEntryField("Notes", ""),
                KdbxEntryField("CustomField", "custom-value"),
            ),
        )
        val root = group("Root", entries = listOf(testEntry))

        val matches = matcher.findMatches(root, "https://example.com")
        val browserEntries = matcher.toBrowserEntries(matches)

        assertEquals(1, browserEntries.size)
        assertEquals(1, browserEntries[0].stringFields?.size)
        assertEquals("CustomField", browserEntries[0].stringFields?.first()?.key)
    }

    // --- Case insensitivity ---

    @Test
    fun matchingIsCaseInsensitive() {
        val matcher = CredentialMatcher()
        val root = group(
            "Root",
            entries = listOf(entry("GitHub", "HTTPS://GITHUB.COM")),
        )

        val results = matcher.findMatches(root, "https://github.com")
        assertEquals(1, results.size)
    }

    // --- Multiple matches sorted by score ---

    @Test
    fun multipleMatchesSortedByScore() {
        val matcher = CredentialMatcher(CredentialMatcher.MatchStrategy.DOMAIN)
        val root = group(
            "Root",
            entries = listOf(
                entry("Generic", "example.com"),
                entry("Specific", "https://example.com/app/login"),
                entry("Medium", "https://example.com/app"),
            ),
        )

        val results = matcher.findMatches(root, "https://example.com/app/login")
        assertEquals(3, results.size)
        // Most specific path match should be first
        assertEquals("Specific", results[0].entry.title)
        assertTrue(results[0].score > results[1].score)
    }
}
