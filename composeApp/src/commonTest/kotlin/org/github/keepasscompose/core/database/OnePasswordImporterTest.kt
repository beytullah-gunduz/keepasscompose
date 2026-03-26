package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnePasswordImporterTest {

    // --- CSV import ---

    private val sampleCsv = """
        title,website,username,password,notes,type,tags
        GitHub,https://github.com,john@example.com,ghpass123,Dev account,Login,dev
        Gmail,https://gmail.com,john@gmail.com,mailpass,,Login,"email,personal"
        API Key,,,sk-1234567890,Keep safe,Secure Note,dev
    """.trimIndent()

    @Test
    fun importCsv_parsesEntries() {
        val result = OnePasswordImporter.importCsv(sampleCsv)
        assertEquals(3, result.entries.size)
        assertEquals(3, result.totalItems)
    }

    @Test
    fun importCsv_mapsFieldsCorrectly() {
        val result = OnePasswordImporter.importCsv(sampleCsv)
        val github = result.entries[0]
        assertEquals("GitHub", github.title)
        assertEquals("john@example.com", github.userName)
        assertEquals("ghpass123", github.password)
        assertEquals("https://github.com", github.url)
        assertEquals("Dev account", github.notes)
    }

    @Test
    fun importCsv_parsesTags() {
        val result = OnePasswordImporter.importCsv(sampleCsv)
        val gmail = result.entries[1]
        assertEquals(listOf("email", "personal"), gmail.tags)
    }

    @Test
    fun importCsv_countsByType() {
        val result = OnePasswordImporter.importCsv(sampleCsv)
        assertEquals(2, result.logins)
        assertEquals(1, result.secureNotes)
    }

    @Test
    fun importCsv_groupsByFirstTag() {
        val result = OnePasswordImporter.importCsv(sampleCsv)
        val groupNames = result.groupTree.groups.map { it.name }.toSet()
        assertTrue("dev" in groupNames)
        assertTrue("email" in groupNames)
    }

    @Test
    fun importCsv_empty() {
        val result = OnePasswordImporter.importCsv("")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun importCsv_headerOnly() {
        val result = OnePasswordImporter.importCsv("title,website,username,password,notes,type,tags")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun importCsv_skipsBlankRows() {
        val csv = "title,username,password\nSite,user,pass\n,,"
        val result = OnePasswordImporter.importCsv(csv)
        assertEquals(1, result.entries.size)
    }

    // --- 1PIF import ---

    private val samplePif = """
        {"title":"GitHub","typeName":"webforms.WebForm","location":"https://github.com","secureContents":{"notesPlain":"Dev account","fields":[{"designation":"username","value":"john"},{"designation":"password","value":"ghpass"}]}}
        ***5642dee7-9f07-4e28-8547-8e7e1a1a1a1a***
        {"title":"Secret Note","typeName":"securenotes.SecureNote","secureContents":{"notesPlain":"Top secret content"}}
        ***5642dee7-9f07-4e28-8547-8e7e1a1a1a1b***
    """.trimIndent()

    @Test
    fun importOnePif_parsesEntries() {
        val result = OnePasswordImporter.importOnePif(samplePif)
        assertEquals(2, result.entries.size)
    }

    @Test
    fun importOnePif_mapsLoginFields() {
        val result = OnePasswordImporter.importOnePif(samplePif)
        val github = result.entries[0]
        assertEquals("GitHub", github.title)
        assertEquals("john", github.userName)
        assertEquals("ghpass", github.password)
        assertEquals("https://github.com", github.url)
        assertEquals("Dev account", github.notes)
    }

    @Test
    fun importOnePif_handlesSecureNotes() {
        val result = OnePasswordImporter.importOnePif(samplePif)
        assertEquals(1, result.logins)
        assertEquals(1, result.secureNotes)
        val note = result.entries[1]
        assertEquals("Secret Note", note.title)
        assertEquals("Top secret content", note.notes)
    }

    @Test
    fun importOnePif_skipsSeparatorLines() {
        val result = OnePasswordImporter.importOnePif(samplePif)
        assertEquals(2, result.entries.size) // separators filtered out
    }

    @Test
    fun importOnePif_empty() {
        val result = OnePasswordImporter.importOnePif("")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun importOnePif_invalidJson() {
        val result = OnePasswordImporter.importOnePif("not json\n{invalid")
        assertEquals(0, result.entries.size)
    }
}
