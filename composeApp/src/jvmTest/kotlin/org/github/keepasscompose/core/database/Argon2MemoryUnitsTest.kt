package org.github.keepasscompose.core.database

import okio.Buffer
import org.github.keepasscompose.core.crypto.CryptoProvider
import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxMeta
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The KDBX 4 Argon2 memory parameter `M` is stored in bytes, while [KdfParameters.Argon2.memory]
 * holds KiB. Older versions of this app wrote the KiB value into `M`; those files must still open.
 */
class Argon2MemoryUnitsTest {
    private val crypto = PlatformCryptoProvider()
    private val compositeKey = CompositeKey(password = "argon2-units")

    @Test
    fun writer_storesMemoryInBytes() {
        val bytes = KdbxWriter(crypto).writeDatabase(buildDatabase(memoryKib = 1024), compositeKey)

        val rawHeader = KdbxHeaderReader().readHeader(Buffer().apply { write(bytes) }).rawHeaderBytes

        assertTrue(rawHeader.containsMemoryEntry(1024L * 1024L), "M must hold 1 MiB expressed in bytes")
    }

    @Test
    fun reader_convertsBytesToKib() {
        val recording = RecordingCryptoProvider(crypto)
        val bytes = KdbxWriter(crypto).writeDatabase(buildDatabase(memoryKib = 1024), compositeKey)

        val headerResult = KdbxHeaderReader().readHeader(Buffer().apply { write(bytes) })
        KdbxReader(recording).readDatabase(bytes, compositeKey)

        assertEquals(1024L, (headerResult.header.kdfParameters as KdfParameters.Argon2).memory)
        assertNull(headerResult.legacyKdfParameters, "1 MiB is a realistic byte count, no legacy fallback")
        assertEquals(listOf(1024L), recording.argon2Memories)
    }

    @Test
    fun reader_keePassXcDefault_requestsSixtyFourMib() {
        val header = buildDatabase(memoryKib = 65536).header
        val output = Buffer()
        KdbxHeaderWriter().writeHeader(output, header)
        output.write(ByteArray(64)) // dummy header hash + HMAC

        val result = KdbxHeaderReader().readHeader(output)

        assertEquals(65536L, (result.header.kdfParameters as KdfParameters.Argon2).memory)
        assertNull(result.legacyKdfParameters)
    }

    @Test
    fun legacyFile_opensWithFallback() {
        val bytes = writeLegacyFile(memoryKib = 1024)

        val result = KdbxReader(crypto).readDatabase(bytes, compositeKey)

        assertEquals("Argon2 Units", result.meta.databaseName)
        assertEquals(1024L, (result.header.kdfParameters as KdfParameters.Argon2).memory)
    }

    @Test
    fun legacyFile_wrongPassword_throws() {
        val bytes = writeLegacyFile(memoryKib = 1024)

        val error =
            assertFailsWith<KdbxParseException> {
                KdbxReader(crypto).readDatabase(bytes, CompositeKey(password = "wrong"))
            }
        assertTrue(error.message!!.startsWith("Invalid credentials"))
    }

    @Test
    fun legacyFile_resave_writesSpecCompliantMemory() {
        val legacy = KdbxReader(crypto).readDatabase(writeLegacyFile(memoryKib = 1024), compositeKey)

        val resaved = KdbxWriter(crypto).writeDatabase(legacy, compositeKey)
        val headerResult = KdbxHeaderReader().readHeader(Buffer().apply { write(resaved) })

        assertTrue(headerResult.rawHeaderBytes.containsMemoryEntry(1024L * 1024L))
        assertNull(headerResult.legacyKdfParameters)
        assertEquals("Argon2 Units", KdbxReader(crypto).readDatabase(resaved, compositeKey).meta.databaseName)
    }

    // -- Helpers --

    /**
     * Reproduces the old writer: `M` holds the KiB count while Argon2 runs with that many KiB.
     * The current writer stores `memory * 1024` in `M`, so writing `memoryKib / 1024` and
     * scaling the Argon2 call back up yields exactly that legacy layout.
     */
    private fun writeLegacyFile(memoryKib: Long): ByteArray {
        val legacyCrypto = MemoryScalingCryptoProvider(crypto, factor = 1024)
        return KdbxWriter(legacyCrypto).writeDatabase(buildDatabase(memoryKib = memoryKib / 1024), compositeKey)
    }

    private fun buildDatabase(memoryKib: Long) = KdbxDatabase(
        meta = KdbxMeta(databaseName = "Argon2 Units"),
        header =
        KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = CipherId.AES_256,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { (it + 0x30).toByte() },
            encryptionIv = ByteArray(16) { (it + 0x40).toByte() },
            kdfParameters =
            KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2D,
                salt = ByteArray(32) { (it + 0x50).toByte() },
                parallelism = 2,
                memory = memoryKib,
                iterations = 1,
            ),
            innerRandomStreamId = InnerStreamCipher.CHACHA20,
            innerRandomStreamKey = ByteArray(64) { (it + 0x60).toByte() },
        ),
        rootGroup = KdbxGroup(uuid = "cm9vdA==", name = "Root"),
    )

    /** Looks for the variant-dictionary entry `UInt64 "M" = value`. */
    private fun ByteArray.containsMemoryEntry(value: Long): Boolean {
        val entry =
            Buffer()
                .writeByte(0x05)
                .writeIntLe(1)
                .writeUtf8("M")
                .writeIntLe(8)
                .writeLongLe(value)
                .readByteArray()
        return (0..size - entry.size).any { start -> entry.indices.all { this[start + it] == entry[it] } }
    }

    private class RecordingCryptoProvider(private val delegate: CryptoProvider) : CryptoProvider by delegate {
        val argon2Memories = mutableListOf<Long>()

        override fun argon2(
            password: ByteArray,
            salt: ByteArray,
            variant: Argon2Variant,
            version: Int,
            memory: Long,
            iterations: Long,
            parallelism: Int,
        ): ByteArray {
            argon2Memories += memory
            return delegate.argon2(password, salt, variant, version, memory, iterations, parallelism)
        }
    }

    private class MemoryScalingCryptoProvider(private val delegate: CryptoProvider, private val factor: Long) : CryptoProvider by delegate {
        override fun argon2(
            password: ByteArray,
            salt: ByteArray,
            variant: Argon2Variant,
            version: Int,
            memory: Long,
            iterations: Long,
            parallelism: Int,
        ): ByteArray = delegate.argon2(password, salt, variant, version, memory * factor, iterations, parallelism)
    }
}
