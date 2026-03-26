package org.github.keepasscompose.core.database

import okio.Buffer
import okio.BufferedSink
import org.github.keepasscompose.core.crypto.CompositeKeyDerivation
import org.github.keepasscompose.core.crypto.CryptoProvider
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

/**
 * Orchestrates writing a KDBX database by combining XML serialization, compression,
 * encryption, and header writing into a single entry point.
 *
 * Supports both KDBX 3.1 and KDBX 4.x formats.
 *
 * Usage note: for atomic file writes, callers should write to a temporary file first,
 * then rename it to the target path on success.
 *
 * @param crypto Platform-specific cryptographic provider.
 */
class KdbxWriter(private val crypto: CryptoProvider) {

    private val keyDerivation = CompositeKeyDerivation(crypto)

    /**
     * Serializes and encrypts a KDBX database to a byte array.
     *
     * @param database The database to write.
     * @param compositeKey The composite key (password, key file, etc.) to lock the database.
     * @return The encrypted KDBX file bytes.
     */
    fun writeDatabase(database: KdbxDatabase, compositeKey: CompositeKey): ByteArray {
        val sink = Buffer()
        writeDatabase(database, compositeKey, sink)
        return sink.readByteArray()
    }

    /**
     * Serializes and encrypts a KDBX database to a buffered sink.
     *
     * @param database The database to write.
     * @param compositeKey The composite key (password, key file, etc.) to lock the database.
     * @param sink The output sink to write the KDBX file to.
     */
    fun writeDatabase(database: KdbxDatabase, compositeKey: CompositeKey, sink: BufferedSink) {
        val header = database.header

        // 1. Derive keys
        val keys = keyDerivation.deriveKeys(compositeKey, header.masterSeed, header.kdfParameters)
        val masterKey = keys.masterKey
        val transformedKey = keys.transformedKey

        // 2. Create inner stream encryptor for protected field values
        val innerStreamCipher = header.innerRandomStreamId
        val innerStreamKey = header.innerRandomStreamKey
        val encryptor = createInnerStreamEncryptor(innerStreamCipher, innerStreamKey)

        // 3. Collect all binary attachments from the entry tree
        val binaryPool = collectBinaries(database.rootGroup)

        // 4. Serialize XML payload
        //    V3: binaries go in <Meta><Binaries> inside the XML
        //    V4: binaries go in the inner header; XML only holds references by ID
        val xmlResult = if (header.version.isV4) {
            KdbxXmlWriter(isV4 = true, innerStreamEncryptor = encryptor)
                .writeXml(meta = database.meta, rootGroup = database.rootGroup)
        } else {
            KdbxXmlWriter(isV4 = false, innerStreamEncryptor = encryptor)
                .writeXml(meta = database.meta, rootGroup = database.rootGroup, binaries = binaryPool.entries())
        }
        val xmlBytes = xmlResult.xml.encodeToByteArray()

        // 5. Build raw payload
        //    V4: inner header is prepended before the XML (inside the encrypted region)
        //    V3: raw payload is just the XML
        val rawPayload = if (header.version.isV4) {
            val innerHeaderBuffer = Buffer()
            KdbxInnerHeaderWriter().writeInnerHeader(
                sink = innerHeaderBuffer,
                innerRandomStreamId = innerStreamCipherId(innerStreamCipher),
                innerRandomStreamKey = innerStreamKey,
                binaries = binaryPool,
            )
            innerHeaderBuffer.readByteArray() + xmlBytes
        } else {
            xmlBytes
        }

        // 6. Compress
        val compressedPayload = when (header.compression) {
            CompressionAlgorithm.GZIP -> KdbxBinaryPool.compressGzip(rawPayload)
            CompressionAlgorithm.NONE -> rawPayload
        }

        // 7. Wrap in block structure and encrypt
        //    V3: streamStartBytes + hashed blocks → then encrypt
        //    V4: encrypt first, then wrap in HMAC blocks
        val encryptedPayload: ByteArray
        if (header.version.isV3) {
            val hashedBlocks = wrapInHashedBlocks(compressedPayload)
            val v3PlaintextPayload = header.streamStartBytes + hashedBlocks
            encryptedPayload = encrypt(v3PlaintextPayload, masterKey, header)
        } else {
            encryptedPayload = encrypt(compressedPayload, masterKey, header)
        }

        // 8. Write header (captures raw bytes for V4 hash/HMAC computation)
        val headerResult = KdbxHeaderWriter().writeHeader(sink, header)

        // 9. Write V4 header authentication: SHA-256 hash + HMAC-SHA-256
        if (header.version.isV4) {
            val hmacBaseKey = keyDerivation.computeHmacBaseKey(header.masterSeed, transformedKey)
            sink.write(crypto.sha256(headerResult.rawHeaderBytes))
            val headerBlockKey = computeHmacBlockKey(hmacBaseKey, HEADER_HMAC_BLOCK_INDEX)
            sink.write(crypto.hmacSha256(headerBlockKey, headerResult.rawHeaderBytes))
        }

        // 10. Write encrypted payload
        if (header.version.isV4) {
            val hmacBaseKey = keyDerivation.computeHmacBaseKey(header.masterSeed, transformedKey)
            writeHmacBlocks(sink, encryptedPayload, hmacBaseKey)
        } else {
            sink.write(encryptedPayload)
        }
    }

    // -- Encryption --

    private fun encrypt(
        data: ByteArray,
        masterKey: ByteArray,
        header: org.github.keepasscompose.core.model.KdbxHeader,
    ): ByteArray = when (header.cipher) {
        CipherId.AES_256 -> crypto.aesEncrypt(data, masterKey, header.encryptionIv)
        CipherId.TWOFISH -> crypto.twofishEncrypt(data, masterKey, header.encryptionIv)
        CipherId.CHACHA20 -> crypto.chaCha20(data, masterKey, header.encryptionIv)
    }

    // -- V3 hashed blocks --

    /**
     * Wraps [data] in KDBX 3.1 hashed blocks.
     *
     * Each block: blockId(4 bytes LE) + SHA-256(data)(32 bytes) + size(4 bytes LE) + data.
     * Terminal block has size == 0.
     */
    private fun wrapInHashedBlocks(data: ByteArray): ByteArray {
        val output = Buffer()

        // Data block (ID = 0)
        output.writeIntLe(0)
        output.write(crypto.sha256(data))
        output.writeIntLe(data.size)
        output.write(data)

        // Terminal block (ID = 1, size = 0)
        output.writeIntLe(1)
        output.write(crypto.sha256(ByteArray(0)))
        output.writeIntLe(0)

        return output.readByteArray()
    }

    // -- V4 HMAC blocks --

    /**
     * Writes [data] as KDBX 4.x HMAC-authenticated blocks.
     *
     * Each block: HMAC-SHA-256(32 bytes) + blockSize(4 bytes LE) + blockData.
     * Terminates with a block of size 0.
     */
    private fun writeHmacBlocks(
        sink: BufferedSink,
        data: ByteArray,
        hmacBaseKey: ByteArray,
    ) {
        // Data block (index 0)
        writeHmacBlock(sink, blockIndex = 0L, data = data, hmacBaseKey = hmacBaseKey)
        // Terminal block (index 1, empty data)
        writeHmacBlock(sink, blockIndex = 1L, data = ByteArray(0), hmacBaseKey = hmacBaseKey)
    }

    private fun writeHmacBlock(
        sink: BufferedSink,
        blockIndex: Long,
        data: ByteArray,
        hmacBaseKey: ByteArray,
    ) {
        val blockKey = computeHmacBlockKey(hmacBaseKey, blockIndex)
        val hmacData = Buffer().apply {
            writeLongLe(blockIndex)
            writeIntLe(data.size)
            write(data)
        }.readByteArray()
        sink.write(crypto.hmacSha256(blockKey, hmacData))
        sink.writeIntLe(data.size)
        if (data.isNotEmpty()) {
            sink.write(data)
        }
    }

    // -- Binary collection --

    private fun collectBinaries(rootGroup: KdbxGroup): KdbxBinaryPool {
        val pool = KdbxBinaryPool()
        collectBinariesFromGroup(rootGroup, pool)
        return pool
    }

    private fun collectBinariesFromGroup(group: KdbxGroup, pool: KdbxBinaryPool) {
        for (entry in group.entries) {
            collectBinariesFromEntry(entry, pool)
        }
        for (subGroup in group.groups) {
            collectBinariesFromGroup(subGroup, pool)
        }
    }

    private fun collectBinariesFromEntry(entry: KdbxEntry, pool: KdbxBinaryPool) {
        for (attachment in entry.attachments) {
            pool.put(attachment.id, attachment.data)
        }
        for (historyEntry in entry.history) {
            collectBinariesFromEntry(historyEntry, pool)
        }
    }

    // -- Inner stream encryptor --

    /**
     * Creates a stateful inner stream encryptor for protected field values.
     *
     * For Salsa20 (KDBX 3.1): key = SHA-256(innerStreamKey), nonce = fixed 8 bytes.
     * For ChaCha20 (KDBX 4.x): SHA-512(innerStreamKey) split into 32-byte key + 12-byte nonce.
     */
    private fun createInnerStreamEncryptor(
        cipher: InnerStreamCipher,
        innerStreamKey: ByteArray,
    ): InnerStreamEncryptor? {
        if (cipher == InnerStreamCipher.NONE) return null

        val cipherKey: ByteArray
        val nonce: ByteArray

        when (cipher) {
            InnerStreamCipher.SALSA20 -> {
                cipherKey = crypto.sha256(innerStreamKey)
                nonce = SALSA20_INNER_NONCE
            }
            InnerStreamCipher.CHACHA20 -> {
                val hash = crypto.sha512(innerStreamKey)
                cipherKey = hash.copyOfRange(0, 32)
                nonce = hash.copyOfRange(32, 44)
            }
            else -> throw KdbxParseException("Unsupported inner stream cipher: $cipher")
        }

        return StatefulStreamEncryptor(crypto, cipher, cipherKey, nonce)
    }

    // -- HMAC helpers --

    private fun computeHmacBlockKey(hmacBaseKey: ByteArray, blockIndex: Long): ByteArray {
        val indexBytes = Buffer().apply { writeLongLe(blockIndex) }.readByteArray()
        return crypto.sha512(indexBytes + hmacBaseKey)
    }

    private fun innerStreamCipherId(cipher: InnerStreamCipher): Int = when (cipher) {
        InnerStreamCipher.NONE -> 0
        InnerStreamCipher.ARC4 -> 1
        InnerStreamCipher.SALSA20 -> 2
        InnerStreamCipher.CHACHA20 -> 3
    }

    companion object {
        /** Block index used for header HMAC verification (0xFFFFFFFFFFFFFFFF as signed Long). */
        private const val HEADER_HMAC_BLOCK_INDEX = -1L

        /** Fixed nonce for the Salsa20 inner random stream (KDBX 3.1). */
        private val SALSA20_INNER_NONCE = byteArrayOf(
            0xE8.toByte(), 0x30, 0x09, 0x4B,
            0x97.toByte(), 0x20, 0x5D, 0x2A,
        )
    }
}

/**
 * Stateful stream cipher wrapper that generates keystream on demand for encryption.
 *
 * Stream ciphers (Salsa20, ChaCha20) are XOR-based, so encryption is identical to decryption.
 * Protected field values must be encrypted in XML document order (depth-first traversal).
 */
private class StatefulStreamEncryptor(
    private val crypto: CryptoProvider,
    private val cipher: InnerStreamCipher,
    private val key: ByteArray,
    private val nonce: ByteArray,
) : InnerStreamEncryptor {
    private var keystream = ByteArray(0)
    private var position = 0

    override fun encrypt(plaintext: ByteArray): ByteArray {
        ensureKeystream(position + plaintext.size)
        val ciphertext = ByteArray(plaintext.size)
        for (i in plaintext.indices) {
            ciphertext[i] = (plaintext[i].toInt() xor keystream[position + i].toInt()).toByte()
        }
        position += plaintext.size
        return ciphertext
    }

    private fun ensureKeystream(needed: Int) {
        if (keystream.size >= needed) return
        val zeros = ByteArray(needed)
        keystream = when (cipher) {
            InnerStreamCipher.SALSA20 -> crypto.salsa20(zeros, key, nonce)
            InnerStreamCipher.CHACHA20 -> crypto.chaCha20(zeros, key, nonce)
            else -> throw KdbxParseException("Unsupported inner stream cipher: $cipher")
        }
    }
}
