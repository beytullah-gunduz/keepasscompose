package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExporterTest {

    private fun entry(
        title: String = "",
        userName: String = "",
        password: String = "",
        url: String = "",
        notes: String = "",
    ) = KdbxEntry(
        uuid = title,
        fields = listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", userName),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", url),
            KdbxEntryField("Notes", notes),
        ),
    )

    @Test
    fun export_basicEntries() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("GitHub", "john", "pass123", "https://github.com", "Dev")),
        )
        val csv = CsvExporter.export(root)
        val lines = csv.trim().lines()
        assertEquals("Title,Username,Password,URL,Notes", lines[0])
        assertEquals("GitHub,john,pass123,https://github.com,Dev", lines[1])
    }

    @Test
    fun export_multipleEntries() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(
                entry("Site1", "user1", "pass1"),
                entry("Site2", "user2", "pass2"),
            ),
        )
        val csv = CsvExporter.export(root)
        assertEquals(3, csv.trim().lines().size) // header + 2 data rows
    }

    @Test
    fun export_emptyDatabase() {
        val root = KdbxGroup(uuid = "root", name = "Root")
        val csv = CsvExporter.export(root)
        assertEquals(1, csv.trim().lines().size) // header only
    }

    @Test
    fun export_withGroupColumn() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            groups = listOf(
                KdbxGroup(uuid = "email", name = "Email",
                    entries = listOf(entry("Gmail", "john"))),
            ),
        )
        val config = CsvExporter.Config(includeGroup = true)
        val csv = CsvExporter.export(root, config)
        val lines = csv.trim().lines()
        assertTrue(lines[0].startsWith("Group,"))
        assertTrue(lines[1].startsWith("Root/Email,"))
    }

    @Test
    fun export_excludePassword() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("Site", "user", "secret")),
        )
        val config = CsvExporter.Config(includePassword = false)
        val csv = CsvExporter.export(root, config)
        val header = csv.trim().lines()[0]
        assertTrue("Password" !in header)
    }

    @Test
    fun export_semicolonDelimiter() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("Site", "user", "pass")),
        )
        val config = CsvExporter.Config(delimiter = ';')
        val csv = CsvExporter.export(root, config)
        assertTrue(csv.contains(";"))
    }

    // --- Escaping ---

    @Test
    fun escapeField_noSpecialChars() {
        assertEquals("hello", CsvExporter.escapeField("hello", ','))
    }

    @Test
    fun escapeField_containsComma() {
        assertEquals("\"hello, world\"", CsvExporter.escapeField("hello, world", ','))
    }

    @Test
    fun escapeField_containsQuote() {
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escapeField("say \"hi\"", ','))
    }

    @Test
    fun escapeField_containsNewline() {
        assertEquals("\"line1\nline2\"", CsvExporter.escapeField("line1\nline2", ','))
    }

    @Test
    fun export_fieldWithComma_isQuoted() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("Site, Inc.", "user")),
        )
        val csv = CsvExporter.export(root)
        assertTrue(csv.contains("\"Site, Inc.\""))
    }

    // --- Subgroups ---

    @Test
    fun export_nestedSubgroups() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(entry("TopEntry")),
            groups = listOf(
                KdbxGroup(uuid = "sub", name = "Sub",
                    entries = listOf(entry("SubEntry"))),
            ),
        )
        val csv = CsvExporter.export(root)
        assertEquals(3, csv.trim().lines().size) // header + 2 entries
    }

    // --- Round-trip with CsvImporter ---

    @Test
    fun export_import_roundTrip() {
        val root = KdbxGroup(
            uuid = "root", name = "Root",
            entries = listOf(
                entry("GitHub", "john", "pass123", "https://github.com", "Dev account"),
                entry("Gmail", "john@gmail.com", "secret456", "https://gmail.com", ""),
            ),
        )
        val csv = CsvExporter.export(root)
        val imported = CsvImporter.parse(csv)
        assertEquals(2, imported.entries.size)
        assertEquals("GitHub", imported.entries[0].title)
        assertEquals("john", imported.entries[0].userName)
        assertEquals("pass123", imported.entries[0].password)
        assertEquals("Gmail", imported.entries[1].title)
    }
}
