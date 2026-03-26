package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchEngineTest {

    private fun entry(
        title: String = "",
        userName: String = "",
        url: String = "",
        notes: String = "",
        tags: List<String> = emptyList(),
        extraFields: List<KdbxEntryField> = emptyList(),
    ): KdbxEntry {
        val fields = mutableListOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", userName),
            KdbxEntryField("Password", "secret", isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", notes),
        ) + extraFields
        return KdbxEntry(uuid = title.hashCode().toString(), fields = fields, tags = tags)
    }

    private val rootGroup = KdbxGroup(
        uuid = "root", name = "Root",
        entries = listOf(
            entry(title = "GitHub", userName = "john@github.com", url = "https://github.com", tags = listOf("dev", "code")),
            entry(title = "Gmail", userName = "john@gmail.com", url = "https://mail.google.com", notes = "Personal email"),
            entry(title = "Bank of America", userName = "john_doe", url = "https://bankofamerica.com", tags = listOf("finance")),
        ),
        groups = listOf(
            KdbxGroup(
                uuid = "work", name = "Work",
                entries = listOf(
                    entry(title = "Slack", userName = "john@work.com", url = "https://slack.com"),
                    entry(
                        title = "AWS Console", userName = "admin",
                        extraFields = listOf(KdbxEntryField("AccountId", "123456789")),
                    ),
                ),
            ),
        ),
    )

    // --- Basic text search ---

    @Test
    fun search_byTitle() {
        val results = SearchEngine.search(rootGroup, "GitHub")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    @Test
    fun search_caseInsensitive() {
        val results = SearchEngine.search(rootGroup, "github")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    @Test
    fun search_byUserName() {
        val results = SearchEngine.search(rootGroup, "john@gmail")
        assertEquals(1, results.size)
        assertEquals("Gmail", results[0].entry.title)
    }

    @Test
    fun search_byUrl() {
        val results = SearchEngine.search(rootGroup, "bankofamerica")
        assertEquals(1, results.size)
        assertEquals("Bank of America", results[0].entry.title)
    }

    @Test
    fun search_byNotes() {
        val results = SearchEngine.search(rootGroup, "Personal email")
        assertEquals(1, results.size)
        assertEquals("Gmail", results[0].entry.title)
    }

    @Test
    fun search_byTag() {
        val results = SearchEngine.search(rootGroup, "finance")
        assertEquals(1, results.size)
        assertEquals("Bank of America", results[0].entry.title)
    }

    @Test
    fun search_byCustomField() {
        val results = SearchEngine.search(rootGroup, "123456789")
        assertEquals(1, results.size)
        assertEquals("AWS Console", results[0].entry.title)
    }

    @Test
    fun search_inSubgroups() {
        val results = SearchEngine.search(rootGroup, "Slack")
        assertEquals(1, results.size)
        assertEquals(listOf("Root", "Work"), results[0].groupPath)
    }

    @Test
    fun search_multipleResults() {
        val results = SearchEngine.search(rootGroup, "john")
        assertEquals(4, results.size) // GitHub, Gmail, Bank, Slack
    }

    @Test
    fun search_noResults() {
        val results = SearchEngine.search(rootGroup, "nonexistent")
        assertEquals(0, results.size)
    }

    @Test
    fun search_emptyQuery() {
        val results = SearchEngine.search(rootGroup, "")
        assertEquals(0, results.size)
    }

    // --- Protected fields ---

    @Test
    fun search_protectedFieldsExcludedByDefault() {
        val results = SearchEngine.search(rootGroup, "secret")
        assertEquals(0, results.size)
    }

    @Test
    fun search_protectedFieldsIncludedWhenRequested() {
        val results = SearchEngine.search(rootGroup, "secret", includeProtected = true)
        assertEquals(5, results.size) // all entries have "secret" password
    }

    // --- AND (multiple terms) ---

    @Test
    fun search_multipleTerms_andLogic() {
        val results = SearchEngine.search(rootGroup, "john github")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    // --- Negation ---

    @Test
    fun search_negation() {
        val results = SearchEngine.search(rootGroup, "john -gmail")
        assertTrue(results.none { it.entry.title == "Gmail" })
        assertTrue(results.any { it.entry.title == "GitHub" })
    }

    // --- Field-specific search ---

    @Test
    fun search_fieldSpecific_title() {
        val results = SearchEngine.search(rootGroup, "title:bank")
        assertEquals(1, results.size)
        assertEquals("Bank of America", results[0].entry.title)
    }

    @Test
    fun search_fieldSpecific_user() {
        val results = SearchEngine.search(rootGroup, "user:admin")
        assertEquals(1, results.size)
        assertEquals("AWS Console", results[0].entry.title)
    }

    @Test
    fun search_fieldSpecific_url() {
        val results = SearchEngine.search(rootGroup, "url:slack")
        assertEquals(1, results.size)
        assertEquals("Slack", results[0].entry.title)
    }

    @Test
    fun search_fieldSpecific_tag() {
        val results = SearchEngine.search(rootGroup, "tag:dev")
        assertEquals(1, results.size)
        assertEquals("GitHub", results[0].entry.title)
    }

    @Test
    fun search_fieldSpecific_combined() {
        val results = SearchEngine.search(rootGroup, "title:g user:john")
        assertEquals(2, results.size) // GitHub and Gmail both have title starting with G and user john
    }

    // --- Regex search ---

    @Test
    fun search_regex() {
        val results = SearchEngine.search(rootGroup, "regex:G(it|ma).*")
        assertEquals(2, results.size)
        val titles = results.map { it.entry.title }.toSet()
        assertTrue("GitHub" in titles)
        assertTrue("Gmail" in titles)
    }

    @Test
    fun search_regex_url() {
        val results = SearchEngine.search(rootGroup, "regex:https://.*\\.com")
        assertEquals(4, results.size) // 4 entries have .com URLs (AWS Console has no URL)
    }

    // --- Quoted strings ---

    @Test
    fun search_quotedString() {
        val results = SearchEngine.search(rootGroup, "\"Bank of America\"")
        assertEquals(1, results.size)
        assertEquals("Bank of America", results[0].entry.title)
    }

    @Test
    fun search_quotedString_noPartialMatch() {
        val results = SearchEngine.search(rootGroup, "\"Bank of\"")
        assertEquals(1, results.size)
    }

    // --- allEntries ---

    @Test
    fun allEntries_collectsAll() {
        val all = SearchEngine.allEntries(rootGroup)
        assertEquals(5, all.size)
    }

    @Test
    fun allEntries_groupPaths() {
        val all = SearchEngine.allEntries(rootGroup)
        val slackResult = all.first { it.entry.title == "Slack" }
        assertEquals(listOf("Root", "Work"), slackResult.groupPath)
    }
}
