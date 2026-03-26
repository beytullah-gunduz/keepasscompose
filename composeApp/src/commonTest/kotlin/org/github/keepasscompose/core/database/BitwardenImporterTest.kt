package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BitwardenImporterTest {

    private val sampleJson = """
    {
      "folders": [
        {"id": "f1", "name": "Email"},
        {"id": "f2", "name": "Social"}
      ],
      "items": [
        {
          "type": 1,
          "name": "Gmail",
          "notes": "Personal email",
          "folderId": "f1",
          "login": {
            "username": "john@gmail.com",
            "password": "mailpass",
            "totp": "otpauth://totp/Gmail?secret=ABC",
            "uris": [{"uri": "https://gmail.com"}]
          }
        },
        {
          "type": 1,
          "name": "Twitter",
          "folderId": "f2",
          "login": {
            "username": "john",
            "password": "tweetpass",
            "uris": [{"uri": "https://twitter.com"}]
          },
          "fields": [
            {"name": "Recovery Email", "value": "backup@email.com", "type": 0},
            {"name": "PIN", "value": "1234", "type": 1}
          ]
        },
        {
          "type": 2,
          "name": "Secret Note",
          "notes": "Very secret"
        },
        {
          "type": 3,
          "name": "Visa Card"
        },
        {
          "type": 4,
          "name": "My Identity"
        }
      ]
    }
    """.trimIndent()

    @Test
    fun import_parsesLoginItems() {
        val result = BitwardenImporter.import(sampleJson)
        assertEquals(3, result.entries.size) // 2 logins + 1 secure note (cards/identities skipped)
        assertEquals(5, result.totalItems)
    }

    @Test
    fun import_mapsFieldsCorrectly() {
        val result = BitwardenImporter.import(sampleJson)
        val gmail = result.entries[0]
        assertEquals("Gmail", gmail.title)
        assertEquals("john@gmail.com", gmail.userName)
        assertEquals("mailpass", gmail.password)
        assertEquals("https://gmail.com", gmail.url)
        assertEquals("Personal email", gmail.notes)
    }

    @Test
    fun import_handlesTotp() {
        val result = BitwardenImporter.import(sampleJson)
        val gmail = result.entries[0]
        val otpField = gmail.fields.firstOrNull { it.key == "otp" }
        assertTrue(otpField != null)
        assertTrue(otpField.value.startsWith("otpauth://"))
    }

    @Test
    fun import_customFields() {
        val result = BitwardenImporter.import(sampleJson)
        val twitter = result.entries[1]
        val recoveryField = twitter.fields.firstOrNull { it.key == "Recovery Email" }
        assertTrue(recoveryField != null)
        assertEquals("backup@email.com", recoveryField.value)

        val pinField = twitter.fields.firstOrNull { it.key == "PIN" }
        assertTrue(pinField != null)
        assertTrue(pinField.isProtected) // type=1 is hidden
    }

    @Test
    fun import_countsByType() {
        val result = BitwardenImporter.import(sampleJson)
        assertEquals(1, result.secureNotes)
        assertEquals(1, result.cards)
        assertEquals(1, result.identities)
    }

    @Test
    fun import_folderMapping() {
        val result = BitwardenImporter.import(sampleJson)
        val groupNames = result.groupTree.groups.map { it.name }.toSet()
        assertTrue("Email" in groupNames)
        assertTrue("Social" in groupNames)
    }

    @Test
    fun import_noFolderGoesToRoot() {
        val result = BitwardenImporter.import(sampleJson)
        // Secret note has no folderId → "No Folder" → root entries
        val rootEntries = result.groupTree.entries
        assertTrue(rootEntries.any { it.title == "Secret Note" })
    }

    @Test
    fun import_emptyJson() {
        val result = BitwardenImporter.import("""{"items": []}""")
        assertEquals(0, result.entries.size)
    }

    @Test
    fun import_minimalJson() {
        val result = BitwardenImporter.import("""{}""")
        assertEquals(0, result.entries.size)
    }
}
