package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HibpCheckerTest {

    private val mockResponses = mutableMapOf<String, String>()

    private val mockFetcher = HibpChecker.HttpFetcher { url ->
        mockResponses[url] ?: throw RuntimeException("No mock response for $url")
    }

    private val checker = HibpChecker(mockFetcher)

    private fun entry(title: String, password: String): KdbxEntry = KdbxEntry(
        uuid = title,
        fields = listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", "user"),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", ""),
            KdbxEntryField("Notes", ""),
        ),
    )

    // --- Response parsing ---

    @Test
    fun parseResponse_found() {
        val response = "1D72CD07550416C216D8AD296BF5C0AE:10\r\n" +
            "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n" +
            "2DC183F740EE76F27B78EB39C8AD972A757:2\r\n"
        val result = checker.parseResponse(response, "1E4C9B93F3F0682250B6CF8331B7EE68FD8")
        assertTrue(result.isBreached)
        assertEquals(3861493, result.breachCount)
    }

    @Test
    fun parseResponse_notFound() {
        val response = "1D72CD07550416C216D8AD296BF5C0AE:10\r\n" +
            "2DC183F740EE76F27B78EB39C8AD972A757:2\r\n"
        val result = checker.parseResponse(response, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        assertFalse(result.isBreached)
        assertEquals(0, result.breachCount)
    }

    @Test
    fun parseResponse_caseInsensitive() {
        val response = "1e4c9b93f3f0682250b6cf8331b7ee68fd8:100\r\n"
        val result = checker.parseResponse(response, "1E4C9B93F3F0682250B6CF8331B7EE68FD8")
        assertTrue(result.isBreached)
        assertEquals(100, result.breachCount)
    }

    @Test
    fun parseResponse_emptyResponse() {
        val result = checker.parseResponse("", "ANYTHING")
        assertFalse(result.isBreached)
    }

    // --- Single password check ---

    @Test
    fun checkPassword_breached() {
        mockResponses["https://api.pwnedpasswords.com/range/5BAA6"] =
            "1D2DA4053E34E76F6576ED1DA63134B5E2A:2\r\n" +
            "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n" +
            "1E4649B934CA495991B7852B855:3\r\n"
        val result = checker.checkPassword("password")
        assertTrue(result.isBreached)
        assertEquals(3861493, result.breachCount)
    }

    @Test
    fun checkPassword_notBreached() {
        mockResponses["https://api.pwnedpasswords.com/range/5BAA6"] =
            "1D2DA4053E34E76F6576ED1DA63134B5E2A:2\r\n" +
            "1E4649B934CA495991B7852B855:3\r\n"
        val result = checker.checkPassword("password")
        assertFalse(result.isBreached)
    }

    @Test
    fun checkPassword_emptyPassword() {
        val result = checker.checkPassword("")
        assertFalse(result.isBreached)
        assertEquals(0, result.breachCount)
    }

    // --- Batch check ---

    @Test
    fun checkAllEntries_batchCheck() {
        val fetcher = HibpChecker.HttpFetcher { url ->
            val prefix = url.substringAfterLast("/")
            if (prefix == "5BAA6") {
                "1E4C9B93F3F0682250B6CF8331B7EE68FD8:100\r\n"
            } else {
                "0000000000000000000000000000000000000:0\r\n"
            }
        }

        val batchChecker = HibpChecker(fetcher)
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(
                entry("Site1", "password"),
                entry("Site2", "password"),
                entry("Site3", "uniquepass123!"),
            ),
        )

        var progressCalls = 0
        val results = batchChecker.checkAllEntries(root) { _, _ -> progressCalls++ }

        assertEquals(3, results.size)
        assertTrue(results.first { it.entry.title == "Site1" }.result.isBreached)
        assertTrue(results.first { it.entry.title == "Site2" }.result.isBreached)
        assertFalse(results.first { it.entry.title == "Site3" }.result.isBreached)
        assertEquals(2, progressCalls) // 2 unique passwords
    }

    @Test
    fun checkAllEntries_skipsEmptyPasswords() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("NoPass", "")),
        )
        val results = checker.checkAllEntries(root)
        assertEquals(0, results.size)
    }

    // --- SHA-1 correctness ---

    @Test
    fun sha1_knownVector_passwordPrefix() {
        // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        // checkPassword should request prefix 5BAA6
        var requestedUrl: String? = null
        val trackingFetcher = HibpChecker.HttpFetcher { url ->
            requestedUrl = url
            ""
        }
        val trackingChecker = HibpChecker(trackingFetcher)
        trackingChecker.checkPassword("password")
        assertEquals("https://api.pwnedpasswords.com/range/5BAA6", requestedUrl)
    }
}
