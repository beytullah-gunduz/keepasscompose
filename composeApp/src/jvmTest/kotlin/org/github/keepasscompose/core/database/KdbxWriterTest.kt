package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxMeta
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for KdbxWriter using real cryptographic operations.
 *
 * Each test writes a KdbxDatabase and reads it back with KdbxReader, verifying
 * that the round-trip preserves all database content.
 */
class KdbxWriterTest {

    private val crypto = PlatformCryptoProvider()
    private val writer = KdbxWriter(crypto)
    private val reader = KdbxReader(crypto)

    // -- KDBX 4.x round-trip tests --

    @Test
    fun roundTrip_v4_aes_gzip_chacha20Inner() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "test-password")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    @Test
    fun roundTrip_v4_chacha20Cipher_noCompression() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.CHACHA20,
            compression = CompressionAlgorithm.NONE,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { (it + 1).toByte() }),
        )
        val key = CompositeKey(password = "chacha20-password")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    @Test
    fun roundTrip_v4_twofish_gzip() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.TWOFISH,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { (it + 2).toByte() }),
        )
        val key = CompositeKey(password = "twofish-password")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    // -- KDBX 3.1 round-trip tests --

    @Test
    fun roundTrip_v3_aes_gzip_salsa20Inner() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { (it + 5).toByte() },
            innerStreamKey = ByteArray(32) { (it + 10).toByte() },
        )
        val key = CompositeKey(password = "v3-password")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    @Test
    fun roundTrip_v3_aes_noCompression() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.NONE,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { (it + 3).toByte() }),
            streamStartBytes = ByteArray(32) { 0xAB.toByte() },
            innerStreamKey = ByteArray(32) { 0xCD.toByte() },
        )
        val key = CompositeKey(password = "v3-no-compression")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    // -- Entry field protection tests --

    @Test
    fun roundTrip_v4_protectedFields_surviveCipher() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            withProtectedPassword = true,
        )
        val key = CompositeKey(password = "protected-fields")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        val originalEntry = database.rootGroup.entries.first()
        val restoredEntry = result.rootGroup.entries.first()

        assertEquals(originalEntry.password, restoredEntry.password)
    }

    @Test
    fun roundTrip_v3_protectedFields_salsa20() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { 0x11.toByte() },
            innerStreamKey = ByteArray(32) { 0x22.toByte() },
            withProtectedPassword = true,
        )
        val key = CompositeKey(password = "v3-protected")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        val originalEntry = database.rootGroup.entries.first()
        val restoredEntry = result.rootGroup.entries.first()

        assertEquals(originalEntry.password, restoredEntry.password)
    }

    // -- Attachment round-trip tests --

    @Test
    fun roundTrip_v4_withAttachment() {
        val attachmentData = "Hello, attachment!".encodeToByteArray()
        val database = buildDatabaseWithAttachment(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            attachmentData = attachmentData,
        )
        val key = CompositeKey(password = "attachment-test")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        val restoredEntry = result.rootGroup.entries.first()
        assertEquals(1, restoredEntry.attachments.size)
        assertTrue(attachmentData.contentEquals(restoredEntry.attachments.first().data))
        assertEquals("file.txt", restoredEntry.attachments.first().name)
    }

    @Test
    fun roundTrip_v3_withAttachment() {
        val attachmentData = "Binary content for V3".encodeToByteArray()
        val database = buildDatabaseWithAttachment(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { 0x33.toByte() },
            innerStreamKey = ByteArray(32) { 0x44.toByte() },
            attachmentData = attachmentData,
        )
        val key = CompositeKey(password = "v3-attachment")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        val restoredEntry = result.rootGroup.entries.first()
        assertEquals(1, restoredEntry.attachments.size)
        assertTrue(attachmentData.contentEquals(restoredEntry.attachments.first().data))
    }

    // -- Meta preservation tests --

    @Test
    fun roundTrip_v4_metaPreserved() {
        val meta = KdbxMeta(
            databaseName = "My Vault",
            description = "Test database",
            defaultUserName = "admin",
            recycleBinEnabled = true,
        )
        val database = KdbxDatabase(
            meta = meta,
            header = buildHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                innerStreamCipher = InnerStreamCipher.CHACHA20,
                kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            ),
            rootGroup = KdbxGroup(uuid = "root-uuid", name = "Root", entries = emptyList()),
        )
        val key = CompositeKey(password = "meta-test")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertEquals(meta.databaseName, result.meta.databaseName)
        assertEquals(meta.description, result.meta.description)
        assertEquals(meta.defaultUserName, result.meta.defaultUserName)
        assertEquals(meta.recycleBinEnabled, result.meta.recycleBinEnabled)
    }

    // -- Nested group tests --

    @Test
    fun roundTrip_v4_nestedGroups() {
        val childEntry = KdbxEntry(
            uuid = "child-entry-uuid",
            fields = listOf(
                KdbxEntryField("Title", "Child Entry", isProtected = false),
                KdbxEntryField("UserName", "child-user", isProtected = false),
            ),
        )
        val childGroup = KdbxGroup(
            uuid = "child-group-uuid",
            name = "Child Group",
            entries = listOf(childEntry),
        )
        val rootEntry = KdbxEntry(
            uuid = "root-entry-uuid",
            fields = listOf(KdbxEntryField("Title", "Root Entry", isProtected = false)),
        )
        val rootGroup = KdbxGroup(
            uuid = "root-uuid",
            name = "Root",
            entries = listOf(rootEntry),
            groups = listOf(childGroup),
        )
        val database = KdbxDatabase(
            header = buildHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                innerStreamCipher = InnerStreamCipher.CHACHA20,
                kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            ),
            rootGroup = rootGroup,
        )
        val key = CompositeKey(password = "nested-groups")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertEquals(1, result.rootGroup.entries.size)
        assertEquals("Root Entry", result.rootGroup.entries.first().title)
        assertEquals(1, result.rootGroup.groups.size)
        assertEquals("Child Group", result.rootGroup.groups.first().name)
        assertEquals(1, result.rootGroup.groups.first().entries.size)
        assertEquals("Child Entry", result.rootGroup.groups.first().entries.first().title)
    }

    // -- Composite key tests --

    @Test
    fun roundTrip_v4_wrongPassword_throwsException() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val correctKey = CompositeKey(password = "correct")
        val wrongKey = CompositeKey(password = "wrong")

        val bytes = writer.writeDatabase(database, correctKey)

        var threw = false
        try {
            reader.readDatabase(bytes, wrongKey)
        } catch (e: KdbxParseException) {
            threw = true
        }
        assertTrue(threw, "Expected KdbxParseException for wrong password")
    }

    @Test
    fun roundTrip_v4_withKeyFile() {
        val keyFileData = ByteArray(32) { (it + 7).toByte() }
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "password-plus-keyfile", keyFileData = keyFileData)

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    // -- Output format tests --

    @Test
    fun output_startsWithKdbxSignature() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "sig-test")

        val bytes = writer.writeDatabase(database, key)

        // KDBX signature: 0x9AA2D903 0xB54BFB67
        assertEquals(0x03.toByte(), bytes[0])
        assertEquals(0xD9.toByte(), bytes[1])
        assertEquals(0xA2.toByte(), bytes[2])
        assertEquals(0x9A.toByte(), bytes[3])
        assertEquals(0x67.toByte(), bytes[4])
        assertEquals(0xFB.toByte(), bytes[5])
        assertEquals(0x4B.toByte(), bytes[6])
        assertEquals(0xB5.toByte(), bytes[7])
    }

    // -- Helpers --

    private fun buildHeader(
        version: KdbxVersion,
        cipher: CipherId,
        compression: CompressionAlgorithm,
        innerStreamCipher: InnerStreamCipher,
        kdf: KdfParameters,
        masterSeed: ByteArray = ByteArray(32) { (it + 1).toByte() },
        encryptionIv: ByteArray = if (cipher == CipherId.CHACHA20) ByteArray(12) { (it + 2).toByte() } else ByteArray(16) { (it + 2).toByte() },
        innerStreamKey: ByteArray = ByteArray(64) { (it + 3).toByte() },
        streamStartBytes: ByteArray = ByteArray(32) { (it + 4).toByte() },
    ): KdbxHeader = KdbxHeader(
        version = version,
        cipher = cipher,
        compression = compression,
        masterSeed = masterSeed,
        encryptionIv = encryptionIv,
        kdfParameters = kdf,
        innerRandomStreamId = innerStreamCipher,
        innerRandomStreamKey = innerStreamKey,
        streamStartBytes = streamStartBytes,
    )

    private fun buildDatabase(
        version: KdbxVersion,
        cipher: CipherId,
        compression: CompressionAlgorithm,
        innerStreamCipher: InnerStreamCipher,
        kdf: KdfParameters,
        masterSeed: ByteArray = ByteArray(32) { (it + 1).toByte() },
        encryptionIv: ByteArray = if (cipher == CipherId.CHACHA20) ByteArray(12) { (it + 2).toByte() } else ByteArray(16) { (it + 2).toByte() },
        innerStreamKey: ByteArray = ByteArray(64) { (it + 3).toByte() },
        streamStartBytes: ByteArray = ByteArray(32) { (it + 4).toByte() },
        withProtectedPassword: Boolean = false,
    ): KdbxDatabase {
        val entry = KdbxEntry(
            uuid = "test-entry-uuid",
            fields = listOf(
                KdbxEntryField("Title", "Test Entry", isProtected = false),
                KdbxEntryField("UserName", "user@example.com", isProtected = false),
                KdbxEntryField("Password", "s3cr3t!", isProtected = withProtectedPassword),
                KdbxEntryField("URL", "https://example.com", isProtected = false),
                KdbxEntryField("Notes", "Some notes here", isProtected = false),
            ),
        )
        return KdbxDatabase(
            header = buildHeader(version, cipher, compression, innerStreamCipher, kdf, masterSeed, encryptionIv, innerStreamKey, streamStartBytes),
            rootGroup = KdbxGroup(
                uuid = "root-group-uuid",
                name = "Root",
                entries = listOf(entry),
            ),
        )
    }

    private fun buildDatabaseWithAttachment(
        version: KdbxVersion,
        cipher: CipherId,
        compression: CompressionAlgorithm,
        innerStreamCipher: InnerStreamCipher,
        kdf: KdfParameters,
        attachmentData: ByteArray,
        masterSeed: ByteArray = ByteArray(32) { (it + 1).toByte() },
        encryptionIv: ByteArray = if (cipher == CipherId.CHACHA20) ByteArray(12) { (it + 2).toByte() } else ByteArray(16) { (it + 2).toByte() },
        innerStreamKey: ByteArray = ByteArray(64) { (it + 3).toByte() },
        streamStartBytes: ByteArray = ByteArray(32) { (it + 4).toByte() },
    ): KdbxDatabase {
        val entry = KdbxEntry(
            uuid = "entry-with-attachment",
            fields = listOf(KdbxEntryField("Title", "Entry With File", isProtected = false)),
            attachments = listOf(KdbxAttachment(id = 0, name = "file.txt", data = attachmentData)),
        )
        return KdbxDatabase(
            header = buildHeader(version, cipher, compression, innerStreamCipher, kdf, masterSeed, encryptionIv, innerStreamKey, streamStartBytes),
            rootGroup = KdbxGroup(uuid = "root-uuid", name = "Root", entries = listOf(entry)),
        )
    }

    private fun assertDatabaseEquals(expected: KdbxDatabase, actual: KdbxDatabase) {
        assertEquals(expected.meta.databaseName, actual.meta.databaseName)
        assertGroupEquals(expected.rootGroup, actual.rootGroup)
    }

    private fun assertGroupEquals(expected: KdbxGroup, actual: KdbxGroup) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.entries.size, actual.entries.size)
        for (i in expected.entries.indices) {
            assertEntryEquals(expected.entries[i], actual.entries[i])
        }
        assertEquals(expected.groups.size, actual.groups.size)
        for (i in expected.groups.indices) {
            assertGroupEquals(expected.groups[i], actual.groups[i])
        }
    }

    private fun assertEntryEquals(expected: KdbxEntry, actual: KdbxEntry) {
        assertEquals(expected.fields.size, actual.fields.size)
        for (expectedField in expected.fields) {
            val actualField = actual.fields.firstOrNull { it.key == expectedField.key }
            assertEquals(expectedField.value, actualField?.value, "Field '${expectedField.key}' mismatch")
        }
    }
}
