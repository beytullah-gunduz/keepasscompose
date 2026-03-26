package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LastPassImporterTest {

    private val sampleCsv = """
        url,username,password,totp,extra,name,grouping,fav
        https://github.com,john@example.com,pass123,,Dev account,GitHub,Development,0
        https://gmail.com,john@gmail.com,mailpass,otpauth://totp/Gmail?secret=ABC,Personal email,Gmail,Email,1
        http://sn,,,,Secret note content,My Secure Note,Notes,0
    """.trimIndent()

    @Test
    fun import_parsesEntries() {
        val result = LastPassImporter.import(sampleCsv)
        assertEquals(3, result.entries.size)
        assertEquals(3, result.totalRows)
    }

    @Test
    fun import_mapsFieldsCorrectly() {
        val result = LastPassImporter.import(sampleCsv)
        val github = result.entries[0]
        assertEquals("GitHub", github.title)
        assertEquals("john@example.com", github.userName)
        assertEquals("pass123", github.password)
        assertEquals("https://github.com", github.url)
        assertEquals("Dev account", github.notes)
    }

    @Test
    fun import_handlesTotp() {
        val result = LastPassImporter.import(sampleCsv)
        val gmail = result.entries[1]
        val otpField = gmail.fields.firstOrNull { it.key == "otp" }
        assertTrue(otpField != null)
        assertTrue(otpField.value.startsWith("otpauth://"))
    }

    @Test
    fun import_detectsSecureNotes() {
        val result = LastPassImporter.import(sampleCsv)
        assertEquals(1, result.secureNotes)
        val secureNote = result.entries[2]
        assertEquals("My Secure Note", secureNote.title)
        assertEquals("", secureNote.url) // http://sn is stripped
        assertEquals("Secret note content", secureNote.notes)
    }

    @Test
    fun import_groupsByFolder() {
        val result = LastPassImporter.import(sampleCsv)
        val groupNames = result.groupTree.groups.map { it.name }.toSet()
        assertTrue("Development" in groupNames)
        assertTrue("Email" in groupNames)
        assertTrue("Notes" in groupNames)
    }

    @Test
    fun import_emptyCSV() {
        val result = LastPassImporter.import("")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun import_headerOnly() {
        val result = LastPassImporter.import("url,username,password,totp,extra,name,grouping,fav")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun import_skipsBlankRows() {
        val csv = "url,username,password,totp,extra,name,grouping,fav\n,,,,,,,\nhttps://x.com,user,pass,,,Site,,0"
        val result = LastPassImporter.import(csv)
        assertEquals(1, result.entries.size)
    }
}
