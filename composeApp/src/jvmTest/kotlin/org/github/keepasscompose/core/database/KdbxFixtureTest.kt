package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests that use generated KDBX fixture files.
 *
 * These tests generate .kdbx files with known passwords and verify that
 * the reader can correctly parse them. The fixtures can also be opened
 * in KeePass/KeePassXC for manual compatibility verification.
 */
class KdbxFixtureTest {

    private val crypto = PlatformCryptoProvider()
    private val reader = KdbxReader(crypto)

    // -- KDBX 4.x fixture tests --

    @Test
    fun readFixture_v4_aesArgon2d() {
        val bytes = KdbxFixtureGenerator.generateV4AesArgon2d()
        val key = CompositeKey(password = KdbxFixtureGenerator.V4_PASSWORD)

        val result = reader.readDatabase(bytes, key)

        assertFixtureContent(result, "V4 AES Argon2d Fixture")
        assertEquals(CipherId.AES_256, result.header.cipher)
        assertTrue(result.header.version.isV4)
    }

    @Test
    fun readFixture_v4_chacha20() {
        val bytes = KdbxFixtureGenerator.generateV4ChaCha20()
        val key = CompositeKey(password = KdbxFixtureGenerator.V4_PASSWORD)

        val result = reader.readDatabase(bytes, key)

        assertFixtureContent(result, "V4 ChaCha20 Fixture")
        assertEquals(CipherId.CHACHA20, result.header.cipher)
    }

    @Test
    fun readFixture_v4_twofish() {
        val bytes = KdbxFixtureGenerator.generateV4Twofish()
        val key = CompositeKey(password = KdbxFixtureGenerator.V4_PASSWORD)

        val result = reader.readDatabase(bytes, key)

        assertFixtureContent(result, "V4 Twofish Fixture")
        assertEquals(CipherId.TWOFISH, result.header.cipher)
    }

    // -- KDBX 3.1 fixture tests --

    @Test
    fun readFixture_v3_aes() {
        val bytes = KdbxFixtureGenerator.generateV3Aes()
        val key = CompositeKey(password = KdbxFixtureGenerator.V3_PASSWORD)

        val result = reader.readDatabase(bytes, key)

        assertFixtureContent(result, "V3 AES Fixture")
        assertTrue(result.header.version.isV3)
    }

    @Test
    fun readFixture_v3_twofish() {
        val bytes = KdbxFixtureGenerator.generateV3Twofish()
        val key = CompositeKey(password = KdbxFixtureGenerator.V3_PASSWORD)

        val result = reader.readDatabase(bytes, key)

        assertFixtureContent(result, "V3 Twofish Fixture")
        assertEquals(CipherId.TWOFISH, result.header.cipher)
    }

    // -- Fixture wrong password tests --

    @Test
    fun readFixture_v4_wrongPassword_throws() {
        val bytes = KdbxFixtureGenerator.generateV4AesArgon2d()
        val wrongKey = CompositeKey(password = "wrong-password")

        assertFailsWith<Exception> {
            reader.readDatabase(bytes, wrongKey)
        }
    }

    @Test
    fun readFixture_v3_wrongPassword_throws() {
        val bytes = KdbxFixtureGenerator.generateV3Aes()
        val wrongKey = CompositeKey(password = "wrong-password")

        assertFailsWith<Exception> {
            reader.readDatabase(bytes, wrongKey)
        }
    }

    // -- Save fixtures to disk (for manual verification with KeePass) --

    @Test
    fun saveFixtures_toDisk() {
        val fixturesDir = File("build/test-fixtures")
        fixturesDir.mkdirs()

        File(fixturesDir, "v4_aes_argon2d.kdbx").writeBytes(
            KdbxFixtureGenerator.generateV4AesArgon2d()
        )
        File(fixturesDir, "v4_chacha20.kdbx").writeBytes(
            KdbxFixtureGenerator.generateV4ChaCha20()
        )
        File(fixturesDir, "v4_twofish.kdbx").writeBytes(
            KdbxFixtureGenerator.generateV4Twofish()
        )
        File(fixturesDir, "v3_aes.kdbx").writeBytes(
            KdbxFixtureGenerator.generateV3Aes()
        )
        File(fixturesDir, "v3_twofish.kdbx").writeBytes(
            KdbxFixtureGenerator.generateV3Twofish()
        )

        // Verify all files were created
        assertTrue(File(fixturesDir, "v4_aes_argon2d.kdbx").exists())
        assertTrue(File(fixturesDir, "v4_chacha20.kdbx").exists())
        assertTrue(File(fixturesDir, "v4_twofish.kdbx").exists())
        assertTrue(File(fixturesDir, "v3_aes.kdbx").exists())
        assertTrue(File(fixturesDir, "v3_twofish.kdbx").exists())
    }

    // -- Assertion helpers --

    private fun assertFixtureContent(
        result: org.github.keepasscompose.core.model.KdbxDatabase,
        expectedName: String,
    ) {
        // Meta
        assertEquals(expectedName, result.meta.databaseName)
        assertEquals("Test fixture database", result.meta.description)
        assertEquals("testuser", result.meta.defaultUserName)
        assertEquals(10, result.meta.historyMaxItems)
        assertTrue(result.meta.recycleBinEnabled)

        // Root group structure
        assertEquals("Root", result.rootGroup.name)
        assertEquals(1, result.rootGroup.entries.size)
        assertEquals(2, result.rootGroup.groups.size)

        // Root entry
        val email = result.rootGroup.entries[0]
        assertEquals("Email Account", email.title)
        assertEquals("user@example.com", email.userName)
        assertEquals("email-p@ssw0rd!", email.password)
        assertEquals("https://mail.example.com", email.url)

        // Banking group
        val banking = result.rootGroup.groups[0]
        assertEquals("Banking", banking.name)
        assertEquals(1, banking.entries.size)
        assertEquals("Bank Account", banking.entries[0].title)
        assertEquals("b@nk-s3cure!", banking.entries[0].password)

        // Work group
        val work = result.rootGroup.groups[1]
        assertEquals("Work", work.name)
        assertEquals(1, work.entries.size)
        assertEquals("VPN Access", work.entries[0].title)
        assertEquals("vpn-k3y!", work.entries[0].password)
    }
}
