package org.github.keepasscompose.core.database

import okio.Buffer
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KdbxHeaderWriterTest {

    private val writer = KdbxHeaderWriter()
    private val reader = KdbxHeaderReader()

    // -- KDBX 3.1 round-trip tests --

    @Test
    fun roundTrip_v3_aesKdf_defaults() {
        val header = KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { it.toByte() },
            encryptionIv = ByteArray(16) { (it + 100).toByte() },
            kdfParameters = KdfParameters.AesKdf(
                rounds = 60000,
                seed = ByteArray(32) { (it + 50).toByte() },
            ),
            innerRandomStreamKey = ByteArray(32) { (it + 200).toByte() },
            innerRandomStreamId = InnerStreamCipher.SALSA20,
            streamStartBytes = ByteArray(32) { (it + 150).toByte() },
        )

        val result = writeAndReadBack(header)

        assertEquals(header, result.header)
        assertNull(result.headerHash)
        assertNull(result.headerHmac)
    }

    @Test
    fun roundTrip_v3_twofish_noCompression() {
        val header = KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = CipherId.TWOFISH,
            compression = CompressionAlgorithm.NONE,
            masterSeed = ByteArray(32) { 0xAB.toByte() },
            encryptionIv = ByteArray(16) { 0xCD.toByte() },
            kdfParameters = KdfParameters.AesKdf(
                rounds = 100000,
                seed = ByteArray(32) { 0xEF.toByte() },
            ),
            innerRandomStreamKey = ByteArray(32) { 0x11.toByte() },
            innerRandomStreamId = InnerStreamCipher.ARC4,
            streamStartBytes = ByteArray(32) { 0x22.toByte() },
        )

        val result = writeAndReadBack(header)

        assertEquals(header, result.header)
    }

    @Test
    fun roundTrip_v3_chacha20Cipher() {
        val header = KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = CipherId.CHACHA20,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { (it * 3).toByte() },
            encryptionIv = ByteArray(12) { (it * 7).toByte() },
            kdfParameters = KdfParameters.AesKdf(
                rounds = 1,
                seed = ByteArray(32) { 0xFF.toByte() },
            ),
            innerRandomStreamKey = ByteArray(64) { it.toByte() },
            innerRandomStreamId = InnerStreamCipher.CHACHA20,
            streamStartBytes = ByteArray(32) { 0x00 },
        )

        val result = writeAndReadBack(header)

        assertEquals(header, result.header)
    }

    // -- KDBX 4.x round-trip tests --

    @Test
    fun roundTrip_v4_aesKdf() {
        val header = KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { it.toByte() },
            encryptionIv = ByteArray(16) { (it + 100).toByte() },
            kdfParameters = KdfParameters.AesKdf(
                rounds = 60000,
                seed = ByteArray(32) { (it + 50).toByte() },
            ),
        )

        val result = writeAndReadBack(header, appendV4Hashes = true)

        assertEquals(header.version, result.header.version)
        assertEquals(header.cipher, result.header.cipher)
        assertEquals(header.compression, result.header.compression)
        assertTrue(header.masterSeed.contentEquals(result.header.masterSeed))
        assertTrue(header.encryptionIv.contentEquals(result.header.encryptionIv))
        assertEquals(header.kdfParameters, result.header.kdfParameters)
        assertNotNull(result.headerHash)
        assertNotNull(result.headerHmac)
    }

    @Test
    fun roundTrip_v4_argon2d() {
        val header = KdbxHeader(
            version = KdbxVersion(4, 1),
            cipher = CipherId.CHACHA20,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { (it * 2).toByte() },
            encryptionIv = ByteArray(12) { (it + 10).toByte() },
            kdfParameters = KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2D,
                version = 0x13,
                salt = ByteArray(32) { (it + 80).toByte() },
                parallelism = 4,
                memory = 131072,
                iterations = 10,
            ),
        )

        val result = writeAndReadBack(header, appendV4Hashes = true)

        assertEquals(header.version, result.header.version)
        assertEquals(header.cipher, result.header.cipher)
        assertEquals(header.compression, result.header.compression)
        assertTrue(header.masterSeed.contentEquals(result.header.masterSeed))
        assertTrue(header.encryptionIv.contentEquals(result.header.encryptionIv))
        assertEquals(header.kdfParameters, result.header.kdfParameters)
    }

    @Test
    fun roundTrip_v4_argon2id() {
        val header = KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = CipherId.TWOFISH,
            compression = CompressionAlgorithm.NONE,
            masterSeed = ByteArray(32) { 0x55 },
            encryptionIv = ByteArray(16) { 0xAA.toByte() },
            kdfParameters = KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2ID,
                version = 0x13,
                salt = ByteArray(32) { 0xBB.toByte() },
                parallelism = 2,
                memory = 65536,
                iterations = 3,
            ),
        )

        val result = writeAndReadBack(header, appendV4Hashes = true)

        assertEquals(header.version, result.header.version)
        assertEquals(header.cipher, result.header.cipher)
        assertEquals(header.compression, result.header.compression)
        assertEquals(header.kdfParameters, result.header.kdfParameters)
    }

    // -- Raw header bytes tests --

    @Test
    fun rawHeaderBytes_matchBetweenWriteAndRead_v3() {
        val header = KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { it.toByte() },
            encryptionIv = ByteArray(16) { it.toByte() },
            kdfParameters = KdfParameters.AesKdf(rounds = 6000, seed = ByteArray(32) { it.toByte() }),
            innerRandomStreamKey = ByteArray(32) { it.toByte() },
            innerRandomStreamId = InnerStreamCipher.SALSA20,
            streamStartBytes = ByteArray(32) { it.toByte() },
        )

        val output = Buffer()
        val writeResult = writer.writeHeader(output, header)

        val readResult = reader.readHeader(Buffer().apply { write(output.readByteArray()) })

        assertTrue(writeResult.rawHeaderBytes.contentEquals(readResult.rawHeaderBytes))
    }

    @Test
    fun rawHeaderBytes_matchBetweenWriteAndRead_v4() {
        val header = KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { it.toByte() },
            encryptionIv = ByteArray(16) { it.toByte() },
            kdfParameters = KdfParameters.AesKdf(rounds = 6000, seed = ByteArray(32) { it.toByte() }),
        )

        val output = Buffer()
        val writeResult = writer.writeHeader(output, header)

        // Append dummy hashes so the reader can parse the v4 post-header
        output.write(ByteArray(32)) // dummy SHA-256
        output.write(ByteArray(32)) // dummy HMAC

        val readResult = reader.readHeader(Buffer().apply { write(output.readByteArray()) })

        assertTrue(writeResult.rawHeaderBytes.contentEquals(readResult.rawHeaderBytes))
    }

    // -- Signature verification --

    @Test
    fun writtenHeader_startsWithCorrectSignatures() {
        val header = KdbxHeader(
            version = KdbxVersion(4, 0),
            masterSeed = ByteArray(32),
            encryptionIv = ByteArray(16),
        )

        val output = Buffer()
        writer.writeHeader(output, header)

        val sig1 = output.readIntLe()
        val sig2 = output.readIntLe()

        assertEquals(0x9AA2D903.toInt(), sig1)
        assertEquals(0xB54BFB67.toInt(), sig2)
    }

    // -- Helper --

    private fun writeAndReadBack(
        header: KdbxHeader,
        appendV4Hashes: Boolean = false,
    ): KdbxHeaderReadResult {
        val output = Buffer()
        writer.writeHeader(output, header)

        if (appendV4Hashes) {
            output.write(ByteArray(32)) // dummy SHA-256 hash
            output.write(ByteArray(32)) // dummy HMAC-SHA-256
        }

        return reader.readHeader(Buffer().apply { write(output.readByteArray()) })
    }
}
