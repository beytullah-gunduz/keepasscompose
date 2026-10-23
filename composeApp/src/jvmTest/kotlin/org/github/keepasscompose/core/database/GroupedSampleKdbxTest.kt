package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdbxDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests that open and verify the sample-groups.kdbx fixture from commonTest/resources.
 *
 * Password: sample — see [GenerateGroupedSampleFixture] for the full structure.
 */
class GroupedSampleKdbxTest {
    private val crypto = PlatformCryptoProvider()
    private val reader = KdbxReader(crypto)

    private fun loadSample(): KdbxDatabase {
        val bytes = javaClass.classLoader!!.getResourceAsStream("sample-groups.kdbx")!!.readBytes()
        return reader.readDatabase(bytes, CompositeKey(password = "sample"))
    }

    @Test
    fun openGroupedSample_succeeds() {
        val db = loadSample()

        assertEquals("Grouped Sample Database", db.meta.databaseName)
        assertEquals(0, db.rootGroup.entries.size)
    }

    @Test
    fun groupedSample_hasFourTopLevelFolders() {
        val db = loadSample()

        assertEquals(listOf("Personal", "Work", "Finance", "Social"), db.rootGroup.groups.map { it.name })
    }

    @Test
    fun groupedSample_eachFolderHasSeveralEntries() {
        val db = loadSample()

        db.rootGroup.groups.forEach { folder ->
            assertEquals(3, folder.entries.size, "Folder ${folder.name}")
        }
    }

    @Test
    fun groupedSample_workHasServersSubFolder() {
        val work = loadSample().rootGroup.groups.single { it.name == "Work" }

        val servers = work.groups.single()
        assertEquals("Servers", servers.name)
        assertEquals(listOf("Production Server", "Staging Server"), servers.entries.map { it.title })
        assertEquals("root", servers.entries[0].userName)
        assertEquals("pr0d-r00t-x9!", servers.entries[0].password)
    }

    @Test
    fun groupedSample_entries_haveCorrectData() {
        val folders = loadSample().rootGroup.groups.associateBy { it.name }

        val email = folders.getValue("Personal").entries[0]
        assertEquals("Personal Email", email.title)
        assertEquals("jane.doe@example.com", email.userName)
        assertEquals("m@il-p3rs0nal!", email.password)

        assertEquals(
            listOf("Online Banking", "Credit Card", "Tax Portal"),
            folders.getValue("Finance").entries.map { it.title },
        )
        assertEquals("pr0-netw0rk#", folders.getValue("Social").entries[2].password)
    }

    @Test
    fun groupedSample_wrongPassword_throws() {
        val bytes = javaClass.classLoader!!.getResourceAsStream("sample-groups.kdbx")!!.readBytes()

        assertFailsWith<KdbxParseException> {
            reader.readDatabase(bytes, CompositeKey(password = "wrong"))
        }
    }
}
