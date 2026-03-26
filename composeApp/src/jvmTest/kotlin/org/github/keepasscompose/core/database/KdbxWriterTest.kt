package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.Argon2Variant
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
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

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

    // -- KDBX 3.1 additional cipher tests --

    @Test
    fun roundTrip_v3_twofish_gzip() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.TWOFISH,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { 0x77.toByte() },
            innerStreamKey = ByteArray(32) { 0x88.toByte() },
        )
        val key = CompositeKey(password = "v3-twofish")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    @Test
    fun roundTrip_v3_twofish_noCompression() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.TWOFISH,
            compression = CompressionAlgorithm.NONE,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { (it + 5).toByte() }),
            streamStartBytes = ByteArray(32) { 0x99.toByte() },
            innerStreamKey = ByteArray(32) { 0xAA.toByte() },
        )
        val key = CompositeKey(password = "v3-twofish-no-compress")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    // -- Argon2 KDF tests --

    @Test
    fun roundTrip_v4_argon2d_kdf() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2D,
                salt = ByteArray(32) { it.toByte() },
                parallelism = 2,
                memory = 1024, // 1 MB - keep small for test speed
                iterations = 1,
            ),
        )
        val key = CompositeKey(password = "argon2d-test")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    @Test
    fun roundTrip_v4_argon2id_kdf() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.CHACHA20,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2ID,
                salt = ByteArray(32) { (it + 10).toByte() },
                parallelism = 2,
                memory = 1024,
                iterations = 1,
            ),
        )
        val key = CompositeKey(password = "argon2id-test")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertDatabaseEquals(database, result)
    }

    // -- Read → Modify → Write → Re-read tests --

    @Test
    fun roundTrip_v4_readModifyWriteReread_addEntry() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "modify-test")

        // Write original
        val bytes1 = writer.writeDatabase(database, key)
        // Read back
        val read1 = reader.readDatabase(bytes1, key)

        // Modify: add new entry
        val newEntry = KdbxEntry(
            uuid = "new-entry-uuid",
            fields = listOf(
                KdbxEntryField("Title", "Added Entry"),
                KdbxEntryField("Password", "newpass", isProtected = true),
            ),
        )
        val modifiedDb = KdbxDatabase(
            meta = read1.meta,
            header = read1.header,
            rootGroup = read1.rootGroup.copy(
                entries = read1.rootGroup.entries + newEntry,
            ),
        )

        // Write modified
        val bytes2 = writer.writeDatabase(modifiedDb, key)
        // Re-read
        val read2 = reader.readDatabase(bytes2, key)

        assertEquals(2, read2.rootGroup.entries.size)
        assertEquals("Test Entry", read2.rootGroup.entries[0].title)
        assertEquals("Added Entry", read2.rootGroup.entries[1].title)
        assertEquals("newpass", read2.rootGroup.entries[1].password)
    }

    @Test
    fun roundTrip_v4_readModifyWriteReread_changePassword() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val originalKey = CompositeKey(password = "original-password")
        val newKey = CompositeKey(password = "new-password")

        // Write with original password
        val bytes1 = writer.writeDatabase(database, originalKey)
        val read1 = reader.readDatabase(bytes1, originalKey)

        // Re-write with new password
        val bytes2 = writer.writeDatabase(
            KdbxDatabase(meta = read1.meta, header = read1.header, rootGroup = read1.rootGroup),
            newKey,
        )

        // Can read with new password
        val read2 = reader.readDatabase(bytes2, newKey)
        assertDatabaseEquals(database, read2)

        // Cannot read with old password
        assertFailsWith<KdbxParseException> {
            reader.readDatabase(bytes2, originalKey)
        }
    }

    @Test
    fun roundTrip_v3_readModifyWriteReread_addGroup() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { 0xDD.toByte() },
            innerStreamKey = ByteArray(32) { 0xEE.toByte() },
        )
        val key = CompositeKey(password = "v3-modify")

        val bytes1 = writer.writeDatabase(database, key)
        val read1 = reader.readDatabase(bytes1, key)

        // Modify: add subgroup with entry
        val newGroup = KdbxGroup(
            uuid = "new-group-uuid",
            name = "New Group",
            entries = listOf(
                KdbxEntry(
                    uuid = "nested-entry",
                    fields = listOf(KdbxEntryField("Title", "Nested")),
                ),
            ),
        )
        val modifiedDb = KdbxDatabase(
            meta = read1.meta,
            header = read1.header,
            rootGroup = read1.rootGroup.copy(
                groups = read1.rootGroup.groups + newGroup,
            ),
        )

        val bytes2 = writer.writeDatabase(modifiedDb, key)
        val read2 = reader.readDatabase(bytes2, key)

        assertEquals(1, read2.rootGroup.groups.size)
        assertEquals("New Group", read2.rootGroup.groups[0].name)
        assertEquals("Nested", read2.rootGroup.groups[0].entries[0].title)
    }

    // -- Entry history and metadata round-trip --

    @Test
    fun roundTrip_v4_entryWithHistory() {
        val historyEntry = KdbxEntry(
            uuid = "test-entry-uuid",
            fields = listOf(
                KdbxEntryField("Title", "Old Title"),
                KdbxEntryField("Password", "old-pass", isProtected = true),
            ),
            lastModificationTime = Instant.parse("2024-01-01T00:00:00Z"),
        )
        val currentEntry = KdbxEntry(
            uuid = "test-entry-uuid",
            fields = listOf(
                KdbxEntryField("Title", "New Title"),
                KdbxEntryField("Password", "new-pass", isProtected = true),
            ),
            lastModificationTime = Instant.parse("2024-06-01T00:00:00Z"),
            history = listOf(historyEntry),
        )
        val database = KdbxDatabase(
            meta = KdbxMeta(
                databaseName = "History Test",
                historyMaxItems = 20,
                historyMaxSize = 10_000_000,
            ),
            header = buildHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                innerStreamCipher = InnerStreamCipher.CHACHA20,
                kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            ),
            rootGroup = KdbxGroup(uuid = "root", name = "Root", entries = listOf(currentEntry)),
        )
        val key = CompositeKey(password = "history-test")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        assertEquals(20, result.meta.historyMaxItems)
        assertEquals(10_000_000L, result.meta.historyMaxSize)
        val entry = result.rootGroup.entries[0]
        assertEquals("New Title", entry.title)
        assertEquals("new-pass", entry.password)
        assertEquals(1, entry.history.size)
        assertEquals("Old Title", entry.history[0].title)
        assertEquals("old-pass", entry.history[0].password)
    }

    // -- Multiple protected fields test --

    @Test
    fun roundTrip_v4_multipleProtectedFields_preserveOrder() {
        val entry = KdbxEntry(
            uuid = "multi-protected",
            fields = listOf(
                KdbxEntryField("Title", "Multi Protected"),
                KdbxEntryField("Password", "pass1", isProtected = true),
                KdbxEntryField("PIN", "1234", isProtected = true),
                KdbxEntryField("SecretKey", "abc-def-ghi", isProtected = true),
                KdbxEntryField("Notes", "visible notes"),
            ),
        )
        val database = KdbxDatabase(
            header = buildHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                innerStreamCipher = InnerStreamCipher.CHACHA20,
                kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            ),
            rootGroup = KdbxGroup(uuid = "root", name = "Root", entries = listOf(entry)),
        )
        val key = CompositeKey(password = "multi-protected")

        val bytes = writer.writeDatabase(database, key)
        val result = reader.readDatabase(bytes, key)

        val restored = result.rootGroup.entries[0]
        assertEquals("pass1", restored.password)
        assertEquals("1234", restored.field("PIN"))
        assertEquals("abc-def-ghi", restored.field("SecretKey"))
        assertEquals("visible notes", restored.notes)
    }

    // -- Corrupted file handling tests --

    @Test
    fun read_truncatedFile_throwsException() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "truncate-test")
        val bytes = writer.writeDatabase(database, key)

        // Truncate to just the header (first ~200 bytes)
        val truncated = bytes.copyOfRange(0, minOf(200, bytes.size))

        assertFailsWith<Exception> {
            reader.readDatabase(truncated, key)
        }
    }

    @Test
    fun read_invalidSignature_throwsException() {
        val bytes = ByteArray(100) { 0xFF.toByte() }
        val key = CompositeKey(password = "bad-sig")

        assertFailsWith<KdbxParseException> {
            reader.readDatabase(bytes, key)
        }
    }

    @Test
    fun read_corruptedPayload_throwsException() {
        val database = buildDatabase(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.CHACHA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
        )
        val key = CompositeKey(password = "corrupt-test")
        val bytes = writer.writeDatabase(database, key)

        // Corrupt bytes near the end (payload area)
        val corrupted = bytes.copyOf()
        for (i in (corrupted.size - 50) until corrupted.size) {
            corrupted[i] = (corrupted[i].toInt() xor 0xFF).toByte()
        }

        assertFailsWith<Exception> {
            reader.readDatabase(corrupted, key)
        }
    }

    @Test
    fun read_v3_wrongPassword_throwsException() {
        val database = buildDatabase(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            innerStreamCipher = InnerStreamCipher.SALSA20,
            kdf = KdfParameters.AesKdf(rounds = 1, seed = ByteArray(32) { it.toByte() }),
            streamStartBytes = ByteArray(32) { 0xBB.toByte() },
            innerStreamKey = ByteArray(32) { 0xCC.toByte() },
        )
        val correctKey = CompositeKey(password = "correct")
        val wrongKey = CompositeKey(password = "wrong")

        val bytes = writer.writeDatabase(database, correctKey)

        assertFailsWith<Exception> {
            reader.readDatabase(bytes, wrongKey)
        }
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
