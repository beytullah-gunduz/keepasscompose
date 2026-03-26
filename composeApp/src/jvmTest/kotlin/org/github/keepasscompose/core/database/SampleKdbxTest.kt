package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.CompositeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests that open and verify the sample.kdbx fixture from commonTest/resources.
 *
 * Password: sample123
 * Structure:
 *   Root/
 *     - GitHub (dev@example.com)
 *     - Google Account (user@gmail.com)
 *     - Home WiFi
 *     Admin/
 *       - Production Server (root)
 *       - Database Admin (db_admin)
 *       - AWS Console (admin@company.com)
 */
class SampleKdbxTest {
    private val crypto = PlatformCryptoProvider()
    private val reader = KdbxReader(crypto)

    private fun loadSample(): ByteArray = javaClass.classLoader!!.getResourceAsStream("sample.kdbx")!!.readBytes()

    @Test
    fun openSampleDatabase_succeeds() {
        val bytes = loadSample()
        val key = CompositeKey(password = "sample123")

        val db = reader.readDatabase(bytes, key)

        assertEquals("Sample Database", db.meta.databaseName)
        assertEquals("Sample KDBX for testing", db.meta.description)
    }

    @Test
    fun sampleDatabase_hasThreeRootEntries() {
        val db = reader.readDatabase(loadSample(), CompositeKey(password = "sample123"))

        assertEquals(3, db.rootGroup.entries.size)
        assertEquals("GitHub", db.rootGroup.entries[0].title)
        assertEquals("Google Account", db.rootGroup.entries[1].title)
        assertEquals("Home WiFi", db.rootGroup.entries[2].title)
    }

    @Test
    fun sampleDatabase_hasAdminGroup() {
        val db = reader.readDatabase(loadSample(), CompositeKey(password = "sample123"))

        assertEquals(1, db.rootGroup.groups.size)
        val admin = db.rootGroup.groups[0]
        assertEquals("Admin", admin.name)
        assertEquals(3, admin.entries.size)
    }

    @Test
    fun sampleDatabase_adminEntries_haveCorrectData() {
        val db = reader.readDatabase(loadSample(), CompositeKey(password = "sample123"))
        val admin = db.rootGroup.groups[0]

        assertEquals("Production Server", admin.entries[0].title)
        assertEquals("root", admin.entries[0].userName)
        assertEquals("pr0d-r00t-2024!", admin.entries[0].password)

        assertEquals("Database Admin", admin.entries[1].title)
        assertEquals("db_admin", admin.entries[1].userName)

        assertEquals("AWS Console", admin.entries[2].title)
        assertEquals("admin@company.com", admin.entries[2].userName)
    }

    @Test
    fun sampleDatabase_rootEntries_haveCorrectPasswords() {
        val db = reader.readDatabase(loadSample(), CompositeKey(password = "sample123"))

        assertEquals("gh-t0ken-s3cret!", db.rootGroup.entries[0].password)
        assertEquals("g00gle-p@ss!", db.rootGroup.entries[1].password)
        assertEquals("wifi-netw0rk-key", db.rootGroup.entries[2].password)
    }

    @Test
    fun sampleDatabase_wrongPassword_throws() {
        assertFailsWith<Exception> {
            reader.readDatabase(loadSample(), CompositeKey(password = "wrong"))
        }
    }
}
