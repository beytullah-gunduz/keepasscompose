package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvImporterTest {
    // --- Basic CSV parsing ---

    @Test
    fun parse_simpleCSV() {
        val csv = "Title,Username,Password,URL,Notes\nGitHub,john,pass123,https://github.com,Dev account"
        val result = CsvImporter.parse(csv)
        assertEquals(1, result.entries.size)
        assertEquals("GitHub", result.entries[0].title)
        assertEquals("john", result.entries[0].userName)
        assertEquals("pass123", result.entries[0].password)
        assertEquals("https://github.com", result.entries[0].url)
        assertEquals("Dev account", result.entries[0].notes)
    }

    @Test
    fun parse_multipleRows() {
        val csv = "Title,Username,Password\nSite1,user1,pass1\nSite2,user2,pass2\nSite3,user3,pass3"
        val result = CsvImporter.parse(csv)
        assertEquals(3, result.entries.size)
        assertEquals(3, result.totalRows)
        assertEquals(0, result.skippedRows)
    }

    @Test
    fun parse_emptyCSV() {
        val result = CsvImporter.parse("")
        assertEquals(0, result.entries.size)
        assertEquals(0, result.totalRows)
    }

    @Test
    fun parse_headerOnly() {
        val result = CsvImporter.parse("Title,Username,Password")
        assertEquals(0, result.entries.size)
        assertEquals(listOf("Title", "Username", "Password"), result.headers)
    }

    // --- Quoted fields (RFC 4180) ---

    @Test
    fun parse_quotedFields() {
        val csv = "Title,Notes\n\"My Site\",\"Some \"\"quoted\"\" notes\""
        val result = CsvImporter.parse(csv)
        assertEquals(1, result.entries.size)
        assertEquals("My Site", result.entries[0].title)
        assertEquals("Some \"quoted\" notes", result.entries[0].notes)
    }

    @Test
    fun parse_quotedFieldWithComma() {
        val csv = "Title,Notes\nSite,\"Note with, comma\""
        val result = CsvImporter.parse(csv)
        assertEquals("Note with, comma", result.entries[0].notes)
    }

    @Test
    fun parse_quotedFieldWithNewline() {
        val csv = "Title,Notes\nSite,\"Line 1\nLine 2\""
        val result = CsvImporter.parse(csv)
        assertEquals("Line 1\nLine 2", result.entries[0].notes)
    }

    // --- Different delimiters ---

    @Test
    fun parse_semicolonDelimiter() {
        val csv = "Title;Username;Password\nGitHub;john;pass123"
        val config = CsvImporter.Config(delimiter = ';')
        val result = CsvImporter.parse(csv, config)
        assertEquals(1, result.entries.size)
        assertEquals("GitHub", result.entries[0].title)
        assertEquals("john", result.entries[0].userName)
    }

    @Test
    fun parse_tabDelimiter() {
        val csv = "Title\tUsername\tPassword\nGitHub\tjohn\tpass123"
        val config = CsvImporter.Config(delimiter = '\t')
        val result = CsvImporter.parse(csv, config)
        assertEquals("GitHub", result.entries[0].title)
    }

    // --- Auto-detection ---

    @Test
    fun autoDetect_standardHeaders() {
        val headers = listOf("Title", "Username", "Password", "URL", "Notes")
        val mapping = CsvImporter.autoDetectMapping(headers)
        assertEquals(0, mapping.titleColumn)
        assertEquals(1, mapping.userNameColumn)
        assertEquals(2, mapping.passwordColumn)
        assertEquals(3, mapping.urlColumn)
        assertEquals(4, mapping.notesColumn)
    }

    @Test
    fun autoDetect_alternativeHeaders() {
        val headers = listOf("name", "email", "pass", "website", "comments")
        val mapping = CsvImporter.autoDetectMapping(headers)
        assertEquals(0, mapping.titleColumn)
        assertEquals(1, mapping.userNameColumn)
        assertEquals(2, mapping.passwordColumn)
        assertEquals(3, mapping.urlColumn)
        assertEquals(4, mapping.notesColumn)
    }

    @Test
    fun autoDetect_caseInsensitive() {
        val headers = listOf("TITLE", "USERNAME", "PASSWORD")
        val mapping = CsvImporter.autoDetectMapping(headers)
        assertEquals(0, mapping.titleColumn)
        assertEquals(1, mapping.userNameColumn)
        assertEquals(2, mapping.passwordColumn)
    }

    // --- Custom mapping ---

    @Test
    fun parse_customMapping() {
        val csv = "Col1,Col2,Col3\nMyTitle,MyUser,MyPass"
        val config =
            CsvImporter.Config(
                mapping = CsvImporter.ColumnMapping(titleColumn = 0, userNameColumn = 1, passwordColumn = 2),
            )
        val result = CsvImporter.parse(csv, config)
        assertEquals("MyTitle", result.entries[0].title)
        assertEquals("MyUser", result.entries[0].userName)
        assertEquals("MyPass", result.entries[0].password)
    }

    // --- Skip blank rows ---

    @Test
    fun parse_skipsBlankRows() {
        val csv = "Title,Password\nSite1,pass1\n,\nSite2,pass2"
        val result = CsvImporter.parse(csv)
        assertEquals(2, result.entries.size)
        assertEquals(1, result.skippedRows)
    }

    // --- No header mode ---

    @Test
    fun parse_noHeader() {
        val csv = "Site1,user1,pass1\nSite2,user2,pass2"
        val config =
            CsvImporter.Config(
                hasHeader = false,
                mapping = CsvImporter.ColumnMapping(titleColumn = 0, userNameColumn = 1, passwordColumn = 2),
            )
        val result = CsvImporter.parse(csv, config)
        assertEquals(2, result.entries.size)
        assertEquals("Site1", result.entries[0].title)
    }

    // --- Group mapping ---

    @Test
    fun parse_withGroupColumn() {
        val csv = "Group,Title,Password\nEmail,Gmail,pass1\nEmail,Yahoo,pass2\nSocial,Twitter,pass3"
        val config =
            CsvImporter.Config(
                mapping = CsvImporter.ColumnMapping(groupColumn = 0, titleColumn = 1, passwordColumn = 2),
            )
        val result = CsvImporter.parse(csv, config)
        assertEquals(3, result.entries.size)
        assertEquals(2, result.groupTree.groups.size) // Email, Social
        val emailGroup = result.groupTree.groups.first { it.name == "Email" }
        assertEquals(2, emailGroup.entries.size)
    }

    // --- CRLF handling ---

    @Test
    fun parse_crlfLineEndings() {
        val csv = "Title,Password\r\nSite1,pass1\r\nSite2,pass2"
        val result = CsvImporter.parse(csv)
        assertEquals(2, result.entries.size)
    }

    // --- Password is protected ---

    @Test
    fun parse_passwordFieldIsProtected() {
        val csv = "Title,Password\nSite,secret"
        val result = CsvImporter.parse(csv)
        val passwordField = result.entries[0].fields.first { it.key == "Password" }
        assertTrue(passwordField.isProtected)
    }
}
