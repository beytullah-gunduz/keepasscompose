package org.github.keepasscompose.core.database

import okio.Buffer
import okio.BufferedSink
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdfParameters

/**
 * Result of writing a KDBX file header.
 *
 * @param rawHeaderBytes The raw bytes that were written (signatures through end-of-header),
 *   needed for SHA-256 and HMAC-SHA-256 computation in KDBX 4.x.
 */
data class KdbxHeaderWriteResult(val rawHeaderBytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxHeaderWriteResult) return false
        return rawHeaderBytes.contentEquals(other.rawHeaderBytes)
    }

    override fun hashCode(): Int = rawHeaderBytes.contentHashCode()
}

/**
 * Serializes KDBX file headers to binary format, supporting both KDBX 3.1 and KDBX 4.x.
 *
 * The writer produces output that can be read back by [KdbxHeaderReader]. For KDBX 4.x,
 * the caller is responsible for computing and appending the SHA-256 hash and HMAC-SHA-256
 * after the header bytes (using the returned [KdbxHeaderWriteResult.rawHeaderBytes]).
 */
class KdbxHeaderWriter {
    /**
     * Writes the given [header] to [sink] in KDBX binary format.
     *
     * @return A [KdbxHeaderWriteResult] containing the raw header bytes for hash computation.
     */
    fun writeHeader(sink: BufferedSink, header: KdbxHeader): KdbxHeaderWriteResult {
        val headerBytes = Buffer()

        // 1. Write signatures
        writeAndCapture(headerBytes, sink) { it.writeIntLe(SIGNATURE_1) }
        writeAndCapture(headerBytes, sink) { it.writeIntLe(SIGNATURE_2) }

        // 2. Write version
        writeAndCapture(headerBytes, sink) { it.writeShortLe(header.version.minor) }
        writeAndCapture(headerBytes, sink) { it.writeShortLe(header.version.major) }

        // 3. Write header fields
        writeField(headerBytes, sink, header.version.isV3, FIELD_CIPHER_ID, cipherIdBytes(header.cipher))
        writeField(
            headerBytes,
            sink,
            header.version.isV3,
            FIELD_COMPRESSION_FLAGS,
            compressionBytes(header.compression),
        )
        writeField(headerBytes, sink, header.version.isV3, FIELD_MASTER_SEED, header.masterSeed)

        if (header.version.isV3) {
            val kdf = header.kdfParameters
            if (kdf is KdfParameters.AesKdf) {
                writeField(headerBytes, sink, true, FIELD_TRANSFORM_SEED, kdf.seed)
                writeField(headerBytes, sink, true, FIELD_TRANSFORM_ROUNDS, longToBytes(kdf.rounds))
            }
        }

        writeField(headerBytes, sink, header.version.isV3, FIELD_ENCRYPTION_IV, header.encryptionIv)

        if (header.version.isV3) {
            writeField(headerBytes, sink, true, FIELD_INNER_RANDOM_STREAM_KEY, header.innerRandomStreamKey)
            writeField(headerBytes, sink, true, FIELD_STREAM_START_BYTES, header.streamStartBytes)
            writeField(
                headerBytes,
                sink,
                true,
                FIELD_INNER_RANDOM_STREAM_ID,
                intToBytes(innerStreamCipherId(header.innerRandomStreamId)),
            )
        }

        if (header.version.isV4) {
            writeField(headerBytes, sink, false, FIELD_KDF_PARAMETERS, serializeKdfParameters(header.kdfParameters))
        }

        // 4. Write end-of-header
        writeField(headerBytes, sink, header.version.isV3, FIELD_END_OF_HEADER, END_OF_HEADER_DATA)

        return KdbxHeaderWriteResult(rawHeaderBytes = headerBytes.readByteArray())
    }

    private fun writeField(headerBytes: Buffer, sink: BufferedSink, isV3: Boolean, fieldId: Int, data: ByteArray) {
        writeAndCapture(headerBytes, sink) { it.writeByte(fieldId) }
        if (isV3) {
            writeAndCapture(headerBytes, sink) { it.writeShortLe(data.size) }
        } else {
            writeAndCapture(headerBytes, sink) { it.writeIntLe(data.size) }
        }
        writeAndCapture(headerBytes, sink) { it.write(data) }
    }

    private inline fun writeAndCapture(headerBytes: Buffer, sink: BufferedSink, block: (BufferedSink) -> Unit) {
        val capture = Buffer()
        block(capture)
        val bytes = capture.readByteArray()
        headerBytes.write(bytes)
        sink.write(bytes)
    }

    private fun cipherIdBytes(cipher: CipherId): ByteArray = when (cipher) {
        CipherId.AES_256 -> CIPHER_AES_UUID
        CipherId.TWOFISH -> CIPHER_TWOFISH_UUID
        CipherId.CHACHA20 -> CIPHER_CHACHA20_UUID
    }

    private fun compressionBytes(compression: CompressionAlgorithm): ByteArray {
        val flag =
            when (compression) {
                CompressionAlgorithm.NONE -> 0
                CompressionAlgorithm.GZIP -> 1
            }
        return intToBytes(flag)
    }

    private fun innerStreamCipherId(cipher: InnerStreamCipher): Int = when (cipher) {
        InnerStreamCipher.NONE -> 0
        InnerStreamCipher.ARC4 -> 1
        InnerStreamCipher.SALSA20 -> 2
        InnerStreamCipher.CHACHA20 -> 3
    }

    private fun serializeKdfParameters(kdf: KdfParameters): ByteArray {
        val buf = Buffer()

        // Variant dictionary version
        buf.writeShortLe(VARIANT_DICTIONARY_VERSION)

        when (kdf) {
            is KdfParameters.AesKdf -> {
                writeVariantEntry(buf, TYPE_BYTE_ARRAY, "\$UUID", KDF_AES_UUID)
                writeVariantEntry(buf, TYPE_UINT64, "R", longToBytes(kdf.rounds))
                writeVariantEntry(buf, TYPE_BYTE_ARRAY, "S", kdf.seed)
            }

            is KdfParameters.Argon2 -> {
                val uuidBytes =
                    when (kdf.variant) {
                        Argon2Variant.ARGON2D -> KDF_ARGON2D_UUID
                        Argon2Variant.ARGON2ID -> KDF_ARGON2ID_UUID
                    }
                writeVariantEntry(buf, TYPE_BYTE_ARRAY, "\$UUID", uuidBytes)
                writeVariantEntry(buf, TYPE_UINT32, "V", intToBytes(kdf.version))
                writeVariantEntry(buf, TYPE_BYTE_ARRAY, "S", kdf.salt)
                writeVariantEntry(buf, TYPE_UINT32, "P", intToBytes(kdf.parallelism))
                writeVariantEntry(buf, TYPE_UINT64, "M", longToBytes(kdf.memory))
                writeVariantEntry(buf, TYPE_UINT64, "I", longToBytes(kdf.iterations))
            }
        }

        // Terminator
        buf.writeByte(0x00)

        return buf.readByteArray()
    }

    private fun writeVariantEntry(buf: Buffer, type: Int, key: String, value: ByteArray) {
        buf.writeByte(type)
        val keyBytes = key.encodeToByteArray()
        buf.writeIntLe(keyBytes.size)
        buf.write(keyBytes)
        buf.writeIntLe(value.size)
        buf.write(value)
    }

    private fun intToBytes(value: Int): ByteArray = Buffer().apply { writeIntLe(value) }.readByteArray()

    private fun longToBytes(value: Long): ByteArray = Buffer().apply { writeLongLe(value) }.readByteArray()

    companion object {
        // KDBX file signatures
        private val SIGNATURE_1 = 0x9AA2D903.toInt()
        private val SIGNATURE_2 = 0xB54BFB67.toInt()

        // Outer header field IDs
        private const val FIELD_END_OF_HEADER = 0
        private const val FIELD_CIPHER_ID = 2
        private const val FIELD_COMPRESSION_FLAGS = 3
        private const val FIELD_MASTER_SEED = 4
        private const val FIELD_TRANSFORM_SEED = 5
        private const val FIELD_TRANSFORM_ROUNDS = 6
        private const val FIELD_ENCRYPTION_IV = 7
        private const val FIELD_INNER_RANDOM_STREAM_KEY = 8
        private const val FIELD_STREAM_START_BYTES = 9
        private const val FIELD_INNER_RANDOM_STREAM_ID = 10
        private const val FIELD_KDF_PARAMETERS = 11

        // Variant dictionary
        private const val VARIANT_DICTIONARY_VERSION = 0x0100
        private const val TYPE_UINT32 = 0x04
        private const val TYPE_UINT64 = 0x05
        private const val TYPE_BYTE_ARRAY = 0x42

        // End-of-header data: 4 bytes of CR LF CR LF for KDBX 4.x, empty for 3.1
        private val END_OF_HEADER_DATA = byteArrayOf(0x0D, 0x0A, 0x0D, 0x0A)

        // Cipher UUIDs (16 bytes, RFC 4122 byte order)
        private val CIPHER_AES_UUID = hexToBytes("31c1f2e6bf714350be5805216afc5aff")
        private val CIPHER_TWOFISH_UUID = hexToBytes("ad68f29f576f4bb9a36ad47af965346c")
        private val CIPHER_CHACHA20_UUID = hexToBytes("d6038a2b8b6f4cb5a524339a31dbb59a")

        // KDF UUIDs
        private val KDF_AES_UUID = hexToBytes("c9d9f39a628a4460bf740d08c18a4fea")
        private val KDF_ARGON2D_UUID = hexToBytes("ef636ddf8c29444b91f7a9a403e30a0c")
        private val KDF_ARGON2ID_UUID = hexToBytes("9e298b1956db4773b23dfc3ec6f0a1e6")

        private fun hexToBytes(hex: String): ByteArray = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
