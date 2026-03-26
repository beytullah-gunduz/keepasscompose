package org.github.keepasscompose.core.database

import okio.Buffer
import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KdbxReaderTest {

    private val crypto = PlatformCryptoProvider()
    private val reader = KdbxReader(crypto)

    // -- KDBX 4.x round-trip tests --

    @Test
    fun readDatabase_v4_aes_roundTrip() {
        val compositeKey = CompositeKey(password = "testPassword")
        val meta = KdbxMeta(databaseName = "TestDB", description = "A test database")
        val rootGroup = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            entries = listOf(
                KdbxEntry(
                    uuid = "ZW50cnk=",
                    fields = listOf(
                        KdbxEntryField("Title", "My Entry"),
                        KdbxEntryField("UserName", "user@example.com"),
                    ),
                ),
            ),
        )

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("TestDB", result.meta.databaseName)
        assertEquals("A test database", result.meta.description)
        assertEquals("Root", result.rootGroup.name)
        assertEquals(1, result.rootGroup.entries.size)
        assertEquals("My Entry", result.rootGroup.entries[0].title)
        assertEquals("user@example.com", result.rootGroup.entries[0].userName)
    }

    @Test
    fun readDatabase_v4_chacha20_roundTrip() {
        val compositeKey = CompositeKey(password = "chacha20test")
        val meta = KdbxMeta(databaseName = "ChaCha20 DB")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.CHACHA20,
            ivSize = 12,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("ChaCha20 DB", result.meta.databaseName)
        assertEquals(CipherId.CHACHA20, result.header.cipher)
    }

    @Test
    fun readDatabase_v4_twofish_roundTrip() {
        val compositeKey = CompositeKey(password = "twofishtest")
        val meta = KdbxMeta(databaseName = "Twofish DB")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.TWOFISH,
            ivSize = 16,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("Twofish DB", result.meta.databaseName)
        assertEquals(CipherId.TWOFISH, result.header.cipher)
    }

    @Test
    fun readDatabase_v4_noCompression_roundTrip() {
        val compositeKey = CompositeKey(password = "noCompress")
        val meta = KdbxMeta(databaseName = "Uncompressed DB")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
            compression = CompressionAlgorithm.NONE,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("Uncompressed DB", result.meta.databaseName)
    }

    // -- KDBX 3.1 round-trip tests --

    @Test
    fun readDatabase_v3_aes_roundTrip() {
        val compositeKey = CompositeKey(password = "v3password")
        val meta = KdbxMeta(databaseName = "V3 Database")
        val rootGroup = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            entries = listOf(
                KdbxEntry(
                    uuid = "ZW50cnk=",
                    fields = listOf(
                        KdbxEntryField("Title", "V3 Entry"),
                    ),
                ),
            ),
        )

        val fileBytes = buildKdbx3File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("V3 Database", result.meta.databaseName)
        assertEquals("Root", result.rootGroup.name)
        assertEquals("V3 Entry", result.rootGroup.entries[0].title)
        assertTrue(result.header.version.isV3)
    }

    // -- Error handling tests --

    @Test
    fun readDatabase_v4_wrongPassword_throwsDescriptiveError() {
        val compositeKey = CompositeKey(password = "correctPassword")
        val meta = KdbxMeta(databaseName = "Test")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
        )

        val wrongKey = CompositeKey(password = "wrongPassword")
        val exception = assertFailsWith<KdbxParseException> {
            reader.readDatabase(fileBytes, wrongKey)
        }
        assertTrue(exception.message!!.contains("Invalid credentials"))
    }

    @Test
    fun readDatabase_v3_wrongPassword_throwsDescriptiveError() {
        val compositeKey = CompositeKey(password = "correctPassword")
        val meta = KdbxMeta(databaseName = "Test")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx3File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
        )

        val wrongKey = CompositeKey(password = "wrongPassword")
        val exception = assertFailsWith<Exception> {
            reader.readDatabase(fileBytes, wrongKey)
        }
        // Either "Invalid credentials" from stream start bytes check,
        // or a decryption error from padding
        assertTrue(exception.message != null)
    }

    @Test
    fun readDatabase_emptyCompositeKey_throwsError() {
        val fileBytes = ByteArray(100) // dummy
        val emptyKey = CompositeKey()

        assertFailsWith<KdbxParseException> {
            reader.readDatabase(fileBytes, emptyKey)
        }
    }

    @Test
    fun readDatabase_v4_protectedValues_decrypted() {
        val compositeKey = CompositeKey(password = "protectedTest")
        val meta = KdbxMeta(databaseName = "Protected DB")
        val rootGroup = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            entries = listOf(
                KdbxEntry(
                    uuid = "ZW50cnk=",
                    fields = listOf(
                        KdbxEntryField("Title", "Secret Entry"),
                        KdbxEntryField("Password", "s3cretP@ss!", isProtected = true),
                    ),
                ),
            ),
        )

        val innerStreamKey = ByteArray(32) { (it + 0x40).toByte() }
        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
            innerStreamKey = innerStreamKey,
            useInnerStreamEncryption = true,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        val entry = result.rootGroup.entries[0]
        assertEquals("Secret Entry", entry.title)
        assertEquals("s3cretP@ss!", entry.password)
    }

    @Test
    fun readDatabase_v4_keyFile_roundTrip() {
        val keyFileData = "this-is-key-file-content".encodeToByteArray()
        val compositeKey = CompositeKey(password = "withKeyFile", keyFileData = keyFileData)
        val meta = KdbxMeta(databaseName = "KeyFile DB")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
        )

        val result = reader.readDatabase(fileBytes, compositeKey)

        assertEquals("KeyFile DB", result.meta.databaseName)
    }

    @Test
    fun readDatabase_acceptsBufferedSource() {
        val compositeKey = CompositeKey(password = "sourceTest")
        val meta = KdbxMeta(databaseName = "Source DB")
        val rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root")

        val fileBytes = buildKdbx4File(
            compositeKey = compositeKey,
            meta = meta,
            rootGroup = rootGroup,
            cipher = CipherId.AES_256,
            ivSize = 16,
        )

        val source = Buffer().apply { write(fileBytes) }
        val result = reader.readDatabase(source, compositeKey)

        assertEquals("Source DB", result.meta.databaseName)
    }

    // -- Test file builders --

    private fun buildKdbx4File(
        compositeKey: CompositeKey,
        meta: KdbxMeta,
        rootGroup: KdbxGroup,
        cipher: CipherId,
        ivSize: Int,
        compression: CompressionAlgorithm = CompressionAlgorithm.GZIP,
        innerStreamKey: ByteArray = ByteArray(32) { (it + 0x20).toByte() },
        useInnerStreamEncryption: Boolean = false,
    ): ByteArray {
        val masterSeed = ByteArray(32) { (it + 1).toByte() }
        val encryptionIv = ByteArray(ivSize) { (it + 0x10).toByte() }
        val kdfParams = KdfParameters.AesKdf(
            rounds = 2,
            seed = ByteArray(32) { (it + 0x30).toByte() },
        )

        val header = KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = cipher,
            compression = compression,
            masterSeed = masterSeed,
            encryptionIv = encryptionIv,
            kdfParameters = kdfParams,
        )

        // Derive keys
        val compositeKeyHash = deriveCompositeKeyHash(compositeKey)
        val transformedKey = deriveTransformedKey(compositeKeyHash, kdfParams)
        val masterKey = crypto.sha256(masterSeed + transformedKey)
        val hmacBaseKey = crypto.sha512(masterSeed + transformedKey + byteArrayOf(0x01))

        val fileBuf = Buffer()

        // 1. Write outer header
        val headerWriteResult = KdbxHeaderWriter().writeHeader(fileBuf, header)

        // 2. Write header hash + HMAC
        fileBuf.write(crypto.sha256(headerWriteResult.rawHeaderBytes))
        val headerBlockKey = computeHmacBlockKey(hmacBaseKey, -1L)
        fileBuf.write(crypto.hmacSha256(headerBlockKey, headerWriteResult.rawHeaderBytes))

        // 3. Build inner payload
        val innerPayloadBuf = Buffer()
        val innerStreamId = 3 // ChaCha20
        KdbxInnerHeaderWriter().writeInnerHeader(
            sink = innerPayloadBuf,
            innerRandomStreamId = innerStreamId,
            innerRandomStreamKey = innerStreamKey,
            binaries = KdbxBinaryPool(),
        )

        // Write XML (with optional inner stream encryption)
        val encryptor = if (useInnerStreamEncryption) {
            createInnerStreamEncryptor(innerStreamKey)
        } else {
            null
        }
        val xmlResult = KdbxXmlWriter(isV4 = true, innerStreamEncryptor = encryptor)
            .writeXml(meta, rootGroup)
        innerPayloadBuf.writeUtf8(xmlResult.xml)

        val innerPayload = innerPayloadBuf.readByteArray()

        // 4. Compress
        val payload = when (compression) {
            CompressionAlgorithm.GZIP -> KdbxBinaryPool.compressGzip(innerPayload)
            CompressionAlgorithm.NONE -> innerPayload
        }

        // 5. Encrypt
        val encrypted = encrypt(payload, masterKey, header)

        // 6. Write HMAC data block
        writeHmacBlock(fileBuf, hmacBaseKey, 0L, encrypted)

        // 7. Write terminal block
        writeHmacBlock(fileBuf, hmacBaseKey, 1L, ByteArray(0))

        return fileBuf.readByteArray()
    }

    private fun buildKdbx3File(
        compositeKey: CompositeKey,
        meta: KdbxMeta,
        rootGroup: KdbxGroup,
    ): ByteArray {
        val masterSeed = ByteArray(32) { (it + 1).toByte() }
        val encryptionIv = ByteArray(16) { (it + 0x10).toByte() }
        val innerStreamKey = ByteArray(32) { (it + 0x20).toByte() }
        val streamStartBytes = ByteArray(32) { (it + 0x50).toByte() }
        val kdfParams = KdfParameters.AesKdf(
            rounds = 2,
            seed = ByteArray(32) { (it + 0x30).toByte() },
        )

        val header = KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = masterSeed,
            encryptionIv = encryptionIv,
            kdfParameters = kdfParams,
            innerRandomStreamKey = innerStreamKey,
            innerRandomStreamId = org.github.keepasscompose.core.model.InnerStreamCipher.SALSA20,
            streamStartBytes = streamStartBytes,
        )

        // Derive keys
        val compositeKeyHash = deriveCompositeKeyHash(compositeKey)
        val transformedKey = deriveTransformedKey(compositeKeyHash, kdfParams)
        val masterKey = crypto.sha256(masterSeed + transformedKey)

        val fileBuf = Buffer()

        // 1. Write outer header
        KdbxHeaderWriter().writeHeader(fileBuf, header)

        // 2. Build XML payload
        val xmlResult = KdbxXmlWriter(isV4 = false).writeXml(meta, rootGroup)
        val xmlBytes = xmlResult.xml.encodeToByteArray()

        // 3. Compress
        val compressed = KdbxBinaryPool.compressGzip(xmlBytes)

        // 4. Build hashed blocks
        val hashedBlocksBuf = Buffer()
        // Single data block
        val blockHash = crypto.sha256(compressed)
        hashedBlocksBuf.writeIntLe(0) // block ID
        hashedBlocksBuf.write(blockHash)
        hashedBlocksBuf.writeIntLe(compressed.size)
        hashedBlocksBuf.write(compressed)
        // Terminal block
        hashedBlocksBuf.writeIntLe(1) // block ID
        hashedBlocksBuf.write(ByteArray(32)) // zero hash
        hashedBlocksBuf.writeIntLe(0) // zero size

        // 5. Prepend stream start bytes
        val decryptedPayload = Buffer().apply {
            write(streamStartBytes)
            write(hashedBlocksBuf.readByteArray())
        }.readByteArray()

        // 6. Encrypt
        val encrypted = crypto.aesEncrypt(decryptedPayload, masterKey, encryptionIv)

        fileBuf.write(encrypted)

        return fileBuf.readByteArray()
    }

    // -- Key derivation helpers (mirrors KdbxReader logic) --

    private fun deriveCompositeKeyHash(compositeKey: CompositeKey): ByteArray {
        val parts = mutableListOf<ByteArray>()
        compositeKey.password?.let { parts.add(crypto.sha256(it.encodeToByteArray())) }
        compositeKey.keyFileData?.let { parts.add(crypto.sha256(it)) }
        compositeKey.hardwareKeyChallenge?.let { parts.add(it) }
        val concatenated = parts.fold(ByteArray(0)) { acc, part -> acc + part }
        return crypto.sha256(concatenated)
    }

    private fun deriveTransformedKey(
        compositeKeyHash: ByteArray,
        kdfParams: KdfParameters,
    ): ByteArray = when (kdfParams) {
        is KdfParameters.AesKdf -> {
            val transformed = crypto.aesKdf(compositeKeyHash, kdfParams.seed, kdfParams.rounds)
            crypto.sha256(transformed)
        }
        is KdfParameters.Argon2 -> crypto.argon2(
            password = compositeKeyHash,
            salt = kdfParams.salt,
            variant = kdfParams.variant,
            version = kdfParams.version,
            memory = kdfParams.memory,
            iterations = kdfParams.iterations,
            parallelism = kdfParams.parallelism,
        )
    }

    private fun computeHmacBlockKey(hmacBaseKey: ByteArray, blockIndex: Long): ByteArray {
        val indexBytes = Buffer().apply { writeLongLe(blockIndex) }.readByteArray()
        return crypto.sha512(indexBytes + hmacBaseKey)
    }

    private fun encrypt(
        data: ByteArray,
        masterKey: ByteArray,
        header: KdbxHeader,
    ): ByteArray = when (header.cipher) {
        CipherId.AES_256 -> crypto.aesEncrypt(data, masterKey, header.encryptionIv)
        CipherId.TWOFISH -> crypto.twofishEncrypt(data, masterKey, header.encryptionIv)
        CipherId.CHACHA20 -> crypto.chaCha20(data, masterKey, header.encryptionIv)
    }

    private fun writeHmacBlock(
        sink: Buffer,
        hmacBaseKey: ByteArray,
        blockIndex: Long,
        blockData: ByteArray,
    ) {
        val blockKey = computeHmacBlockKey(hmacBaseKey, blockIndex)
        val hmacData = Buffer().apply {
            writeLongLe(blockIndex)
            writeIntLe(blockData.size)
            write(blockData)
        }.readByteArray()
        sink.write(crypto.hmacSha256(blockKey, hmacData))
        sink.writeIntLe(blockData.size)
        sink.write(blockData)
    }

    private fun createInnerStreamEncryptor(innerStreamKey: ByteArray): InnerStreamEncryptor {
        // ChaCha20 inner stream: SHA-512(key) -> first 32 bytes = key, next 12 bytes = nonce
        val hash = crypto.sha512(innerStreamKey)
        val cipherKey = hash.copyOfRange(0, 32)
        val nonce = hash.copyOfRange(32, 44)

        var keystream = ByteArray(0)
        var position = 0

        return InnerStreamEncryptor { plaintext ->
            val needed = position + plaintext.size
            if (keystream.size < needed) {
                keystream = crypto.chaCha20(ByteArray(needed), cipherKey, nonce)
            }
            val result = ByteArray(plaintext.size)
            for (i in plaintext.indices) {
                result[i] = (plaintext[i].toInt() xor keystream[position + i].toInt()).toByte()
            }
            position += plaintext.size
            result
        }
    }
}
