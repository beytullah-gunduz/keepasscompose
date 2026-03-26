package org.github.keepasscompose.core.database

import okio.Buffer
import okio.BufferedSource
import org.github.keepasscompose.core.crypto.CompositeKeyDerivation
import org.github.keepasscompose.core.crypto.CryptoProvider
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxDatabase

/**
 * Orchestrates reading a KDBX file by combining header parsing, key derivation,
 * decryption, decompression, and XML parsing into a single entry point.
 *
 * Supports both KDBX 3.1 and KDBX 4.x formats.
 *
 * @param crypto Platform-specific cryptographic provider.
 */
class KdbxReader(private val crypto: CryptoProvider) {
    private val keyDerivation = CompositeKeyDerivation(crypto)

    /**
     * Reads and decrypts a KDBX database from a byte array.
     *
     * @param data The raw KDBX file bytes.
     * @param compositeKey The composite key (password, key file, etc.) to unlock the database.
     * @return The decrypted [KdbxDatabase].
     * @throws KdbxParseException if the file is malformed or the credentials are incorrect.
     */
    fun readDatabase(data: ByteArray, compositeKey: CompositeKey): KdbxDatabase {
        val source = Buffer().apply { write(data) }
        return readDatabase(source, compositeKey)
    }

    /**
     * Reads and decrypts a KDBX database from a buffered source.
     *
     * @param source The buffered source containing the KDBX file data.
     * @param compositeKey The composite key (password, key file, etc.) to unlock the database.
     * @return The decrypted [KdbxDatabase].
     * @throws KdbxParseException if the file is malformed or the credentials are incorrect.
     */
    fun readDatabase(source: BufferedSource, compositeKey: CompositeKey): KdbxDatabase {
        // 1. Parse outer header
        val headerResult = KdbxHeaderReader().readHeader(source)
        val header = headerResult.header

        // 2. Derive keys
        val keys = keyDerivation.deriveKeys(compositeKey, header.masterSeed, header.kdfParameters)
        val masterKey = keys.masterKey
        val transformedKey = keys.transformedKey

        // 3. Verify and read encrypted payload
        val encryptedPayload: ByteArray
        if (header.version.isV4) {
            verifyHeaderHash(headerResult)
            verifyHeaderHmac(headerResult, header.masterSeed, transformedKey)
            encryptedPayload = readHmacBlocks(source, header.masterSeed, transformedKey)
        } else {
            encryptedPayload = source.readByteArray()
        }

        // 4. Decrypt
        val decryptedPayload = decrypt(encryptedPayload, masterKey, header)

        // 5. For KDBX 3.1: verify stream start bytes and extract hashed blocks
        val rawPayload =
            if (header.version.isV3) {
                verifyAndExtractV3Payload(decryptedPayload, header)
            } else {
                decryptedPayload
            }

        // 6. Decompress
        val decompressedPayload =
            when (header.compression) {
                CompressionAlgorithm.GZIP -> KdbxBinaryPool.decompressGzip(rawPayload)
                CompressionAlgorithm.NONE -> rawPayload
            }

        // 7. Read inner header (V4) or use outer header fields (V3)
        val payloadSource = Buffer().apply { write(decompressedPayload) }
        val innerStreamKey: ByteArray
        val innerStreamCipher: InnerStreamCipher
        val binaryPool: KdbxBinaryPool?

        if (header.version.isV4) {
            val innerHeader = KdbxInnerHeaderReader().readInnerHeader(payloadSource)
            innerStreamKey = innerHeader.innerRandomStreamKey
            innerStreamCipher = innerStreamCipherFromId(innerHeader.innerRandomStreamId)
            binaryPool = innerHeader.binaries
        } else {
            innerStreamKey = header.innerRandomStreamKey
            innerStreamCipher = header.innerRandomStreamId
            binaryPool = null
        }

        // 8. Create inner stream decryptor for protected values
        val decryptor = createInnerStreamDecryptor(innerStreamCipher, innerStreamKey)

        // 9. Parse XML
        val xml = payloadSource.readUtf8()
        val xmlResult =
            KdbxXmlReader(
                isV4 = header.version.isV4,
                innerStreamDecryptor = decryptor,
                externalBinaryPool = binaryPool,
            ).readXml(xml)

        return KdbxDatabase(
            meta = xmlResult.meta,
            header = header,
            rootGroup = xmlResult.rootGroup,
        )
    }

    // -- Header verification (KDBX 4.x) --

    private fun verifyHeaderHash(headerResult: KdbxHeaderReadResult) {
        val expected =
            headerResult.headerHash
                ?: throw KdbxParseException("KDBX 4.x file missing header hash")
        val actual = crypto.sha256(headerResult.rawHeaderBytes)
        if (!actual.contentEquals(expected)) {
            throw KdbxParseException("Header SHA-256 verification failed: file may be corrupted")
        }
    }

    private fun verifyHeaderHmac(headerResult: KdbxHeaderReadResult, masterSeed: ByteArray, transformedKey: ByteArray) {
        val expected =
            headerResult.headerHmac
                ?: throw KdbxParseException("KDBX 4.x file missing header HMAC")

        val hmacBaseKey = keyDerivation.computeHmacBaseKey(masterSeed, transformedKey)
        val blockKey = computeHmacBlockKey(hmacBaseKey, HEADER_HMAC_BLOCK_INDEX)
        val actual = crypto.hmacSha256(blockKey, headerResult.rawHeaderBytes)

        if (!actual.contentEquals(expected)) {
            throw KdbxParseException(
                "Invalid credentials: header HMAC verification failed",
            )
        }
    }

    // -- Payload reading --

    /**
     * Reads KDBX 4.x HMAC-authenticated blocks.
     *
     * Each block: HMAC(32 bytes) + blockSize(4 bytes LE) + blockData.
     * Terminates when blockSize == 0.
     */
    private fun readHmacBlocks(source: BufferedSource, masterSeed: ByteArray, transformedKey: ByteArray): ByteArray {
        val hmacBaseKey = keyDerivation.computeHmacBaseKey(masterSeed, transformedKey)
        val output = Buffer()
        var blockIndex = 0L

        while (true) {
            val storedHmac = source.readByteArray(32)
            val blockSize = source.readIntLe()

            val blockData =
                if (blockSize > 0) {
                    source.readByteArray(blockSize.toLong())
                } else {
                    ByteArray(0)
                }

            // Verify block HMAC
            val blockKey = computeHmacBlockKey(hmacBaseKey, blockIndex)
            val hmacData =
                Buffer()
                    .apply {
                        writeLongLe(blockIndex)
                        writeIntLe(blockSize)
                        write(blockData)
                    }.readByteArray()
            val expectedHmac = crypto.hmacSha256(blockKey, hmacData)

            if (!expectedHmac.contentEquals(storedHmac)) {
                throw KdbxParseException(
                    "Invalid credentials: HMAC block verification failed at block $blockIndex",
                )
            }

            if (blockSize == 0) break

            output.write(blockData)
            blockIndex++
        }

        return output.readByteArray()
    }

    /**
     * Verifies stream start bytes and extracts data from KDBX 3.1 hashed blocks.
     *
     * After decryption, the payload starts with 32 stream-start bytes (must match header),
     * followed by a sequence of hashed blocks.
     */
    private fun verifyAndExtractV3Payload(decryptedPayload: ByteArray, header: org.github.keepasscompose.core.model.KdbxHeader): ByteArray {
        val source = Buffer().apply { write(decryptedPayload) }

        // Verify stream start bytes
        val streamStartBytes = source.readByteArray(32)
        if (!streamStartBytes.contentEquals(header.streamStartBytes)) {
            throw KdbxParseException(
                "Invalid credentials: stream start bytes do not match",
            )
        }

        return readHashedBlocks(source)
    }

    /**
     * Reads KDBX 3.1 hashed blocks.
     *
     * Each block: blockId(4 bytes LE) + hash(32 bytes) + size(4 bytes LE) + data.
     * Terminal block has size == 0.
     */
    private fun readHashedBlocks(source: BufferedSource): ByteArray {
        val output = Buffer()

        while (true) {
            source.readIntLe() // block ID (unused, sequential)
            val blockHash = source.readByteArray(32)
            val blockSize = source.readIntLe()

            if (blockSize == 0) break

            val blockData = source.readByteArray(blockSize.toLong())

            // Verify block hash
            val actualHash = crypto.sha256(blockData)
            if (!actualHash.contentEquals(blockHash)) {
                throw KdbxParseException("Hashed block verification failed: data may be corrupted")
            }

            output.write(blockData)
        }

        return output.readByteArray()
    }

    // -- Decryption --

    private fun decrypt(data: ByteArray, masterKey: ByteArray, header: org.github.keepasscompose.core.model.KdbxHeader): ByteArray =
        when (header.cipher) {
            CipherId.AES_256 -> crypto.aesDecrypt(data, masterKey, header.encryptionIv)
            CipherId.TWOFISH -> crypto.twofishDecrypt(data, masterKey, header.encryptionIv)
            CipherId.CHACHA20 -> crypto.chaCha20(data, masterKey, header.encryptionIv)
        }

    // -- Inner stream decryptor --

    /**
     * Creates a stateful inner stream decryptor for protected field values.
     *
     * For Salsa20: key = SHA-256(innerStreamKey), nonce = fixed 8 bytes.
     * For ChaCha20: SHA-512(innerStreamKey) split into 32-byte key + 12-byte nonce.
     */
    private fun createInnerStreamDecryptor(cipher: InnerStreamCipher, innerStreamKey: ByteArray): InnerStreamDecryptor? {
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

            else -> {
                throw KdbxParseException("Unsupported inner stream cipher: $cipher")
            }
        }

        return StatefulStreamDecryptor(crypto, cipher, cipherKey, nonce)
    }

    // -- HMAC helpers --

    private fun computeHmacBlockKey(hmacBaseKey: ByteArray, blockIndex: Long): ByteArray {
        val indexBytes = Buffer().apply { writeLongLe(blockIndex) }.readByteArray()
        return crypto.sha512(indexBytes + hmacBaseKey)
    }

    private fun innerStreamCipherFromId(id: Int): InnerStreamCipher = when (id) {
        0 -> InnerStreamCipher.NONE
        1 -> InnerStreamCipher.ARC4
        2 -> InnerStreamCipher.SALSA20
        3 -> InnerStreamCipher.CHACHA20
        else -> throw KdbxParseException("Unknown inner stream cipher ID: $id")
    }

    companion object {
        /** Block index used for header HMAC verification (0xFFFFFFFFFFFFFFFF). */
        private const val HEADER_HMAC_BLOCK_INDEX = -1L // 0xFFFFFFFFFFFFFFFF as signed Long

        /** Fixed nonce for the Salsa20 inner random stream (KDBX 3.1). */
        private val SALSA20_INNER_NONCE =
            byteArrayOf(
                0xE8.toByte(),
                0x30,
                0x09,
                0x4B,
                0x97.toByte(),
                0x20,
                0x5D,
                0x2A,
            )
    }
}

/**
 * Stateful stream cipher wrapper that generates keystream on demand.
 *
 * Stream ciphers (Salsa20, ChaCha20) produce a keystream that is XOR'd with data.
 * Each call to [decrypt] consumes the next portion of the keystream, so protected
 * values must be decrypted in XML document order.
 */
private class StatefulStreamDecryptor(
    private val crypto: CryptoProvider,
    private val cipher: InnerStreamCipher,
    private val key: ByteArray,
    private val nonce: ByteArray,
) : InnerStreamDecryptor {
    private var keystream = ByteArray(0)
    private var position = 0

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        ensureKeystream(position + ciphertext.size)
        val plaintext = ByteArray(ciphertext.size)
        for (i in ciphertext.indices) {
            plaintext[i] = (ciphertext[i].toInt() xor keystream[position + i].toInt()).toByte()
        }
        position += ciphertext.size
        return plaintext
    }

    private fun ensureKeystream(needed: Int) {
        if (keystream.size >= needed) return
        // Generate keystream by encrypting zeros up to the needed length
        val zeros = ByteArray(needed)
        keystream =
            when (cipher) {
                InnerStreamCipher.SALSA20 -> crypto.salsa20(zeros, key, nonce)
                InnerStreamCipher.CHACHA20 -> crypto.chaCha20(zeros, key, nonce)
                else -> throw KdbxParseException("Unsupported inner stream cipher: $cipher")
            }
    }
}
