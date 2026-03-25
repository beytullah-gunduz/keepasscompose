package org.github.keepasscompose.core.database

import okio.Buffer
import okio.BufferedSource
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters

/**
 * Result of reading a KDBX file header.
 *
 * @param header The parsed header fields.
 * @param rawHeaderBytes The raw bytes of the header (signatures through end-of-header),
 *   used for SHA-256 verification in KDBX 4.x.
 * @param headerHash SHA-256 hash stored after the header (KDBX 4.x only).
 * @param headerHmac HMAC-SHA-256 hash stored after the header (KDBX 4.x only).
 *   Verification requires the derived master key and is handled separately.
 */
data class KdbxHeaderReadResult(
    val header: KdbxHeader,
    val rawHeaderBytes: ByteArray,
    val headerHash: ByteArray? = null,
    val headerHmac: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxHeaderReadResult) return false
        return header == other.header &&
            rawHeaderBytes.contentEquals(other.rawHeaderBytes) &&
            headerHash.contentEquals(other.headerHash) &&
            headerHmac.contentEquals(other.headerHmac)
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + rawHeaderBytes.contentHashCode()
        result = 31 * result + (headerHash?.contentHashCode() ?: 0)
        result = 31 * result + (headerHmac?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Reads and parses KDBX file headers, supporting both KDBX 3.1 and KDBX 4.x formats.
 */
class KdbxHeaderReader {

    fun readHeader(source: BufferedSource): KdbxHeaderReadResult {
        val headerBytes = Buffer()

        // 1. Read and validate signatures
        val sig1 = source.readIntLe()
        val sig2 = source.readIntLe()
        headerBytes.writeIntLe(sig1)
        headerBytes.writeIntLe(sig2)
        validateSignatures(sig1, sig2)

        // 2. Read version
        val versionMinor = source.readShortLe().toInt() and 0xFFFF
        val versionMajor = source.readShortLe().toInt() and 0xFFFF
        headerBytes.writeShortLe(versionMinor)
        headerBytes.writeShortLe(versionMajor)
        val version = KdbxVersion(versionMajor, versionMinor)
        validateVersion(version)

        // 3. Parse header fields
        var cipher = CipherId.AES_256
        var compression = CompressionAlgorithm.GZIP
        var masterSeed = ByteArray(0)
        var encryptionIv = ByteArray(0)
        var innerRandomStreamKey = ByteArray(0)
        var innerRandomStreamId = InnerStreamCipher.SALSA20
        var streamStartBytes = ByteArray(0)
        var kdfParameters: KdfParameters = KdfParameters.AesKdf()

        // KDBX 3.1 specific fields (converted to KdfParameters after parsing)
        var transformSeed = ByteArray(0)
        var transformRounds = 6000L

        while (true) {
            val fieldId: Int
            val fieldSize: Int

            if (version.isV3) {
                fieldId = source.readByte().toInt() and 0xFF
                fieldSize = source.readShortLe().toInt() and 0xFFFF
                headerBytes.writeByte(fieldId)
                headerBytes.writeShortLe(fieldSize)
            } else {
                fieldId = source.readByte().toInt() and 0xFF
                fieldSize = source.readIntLe()
                headerBytes.writeByte(fieldId)
                headerBytes.writeIntLe(fieldSize)
            }

            val fieldData = source.readByteArray(fieldSize.toLong())
            headerBytes.write(fieldData)

            if (fieldId == FIELD_END_OF_HEADER) break

            when (fieldId) {
                FIELD_CIPHER_ID -> cipher = parseCipherId(fieldData)
                FIELD_COMPRESSION_FLAGS -> compression = parseCompression(fieldData)
                FIELD_MASTER_SEED -> masterSeed = fieldData
                FIELD_TRANSFORM_SEED -> transformSeed = fieldData
                FIELD_TRANSFORM_ROUNDS -> {
                    transformRounds = Buffer().apply { write(fieldData) }.readLongLe()
                }
                FIELD_ENCRYPTION_IV -> encryptionIv = fieldData
                FIELD_INNER_RANDOM_STREAM_KEY -> innerRandomStreamKey = fieldData
                FIELD_STREAM_START_BYTES -> streamStartBytes = fieldData
                FIELD_INNER_RANDOM_STREAM_ID -> {
                    val id = Buffer().apply { write(fieldData) }.readIntLe()
                    innerRandomStreamId = parseInnerStreamCipher(id)
                }
                FIELD_KDF_PARAMETERS -> kdfParameters = parseKdfParameters(fieldData)
            }
        }

        // For KDBX 3.1, convert transform seed/rounds to KdfParameters
        if (version.isV3) {
            kdfParameters = KdfParameters.AesKdf(
                rounds = transformRounds,
                seed = transformSeed,
            )
        }

        val rawHeaderBytes = headerBytes.readByteArray()

        // For KDBX 4.x, read header hash and HMAC that follow the header
        var headerHash: ByteArray? = null
        var headerHmac: ByteArray? = null
        if (version.isV4) {
            headerHash = source.readByteArray(32)
            headerHmac = source.readByteArray(32)
        }

        return KdbxHeaderReadResult(
            header = KdbxHeader(
                version = version,
                cipher = cipher,
                compression = compression,
                masterSeed = masterSeed,
                encryptionIv = encryptionIv,
                kdfParameters = kdfParameters,
                innerRandomStreamKey = innerRandomStreamKey,
                innerRandomStreamId = innerRandomStreamId,
                streamStartBytes = streamStartBytes,
            ),
            rawHeaderBytes = rawHeaderBytes,
            headerHash = headerHash,
            headerHmac = headerHmac,
        )
    }

    private fun validateSignatures(sig1: Int, sig2: Int) {
        if (sig1 != SIGNATURE_1) {
            throw KdbxParseException(
                "Invalid KDBX signature 1: expected 0x${SIGNATURE_1.toHex()}, got 0x${sig1.toHex()}"
            )
        }
        if (sig2 != SIGNATURE_2) {
            throw KdbxParseException(
                "Invalid KDBX signature 2: expected 0x${SIGNATURE_2.toHex()}, got 0x${sig2.toHex()}"
            )
        }
    }

    private fun validateVersion(version: KdbxVersion) {
        if (version.major != 3 && version.major != 4) {
            throw KdbxParseException(
                "Unsupported KDBX version: ${version.major}.${version.minor}"
            )
        }
    }

    private fun parseCipherId(data: ByteArray): CipherId = when {
        data.contentEquals(CIPHER_AES_UUID) -> CipherId.AES_256
        data.contentEquals(CIPHER_TWOFISH_UUID) -> CipherId.TWOFISH
        data.contentEquals(CIPHER_CHACHA20_UUID) -> CipherId.CHACHA20
        else -> throw KdbxParseException("Unknown cipher UUID")
    }

    private fun parseCompression(data: ByteArray): CompressionAlgorithm {
        val flags = Buffer().apply { write(data) }.readIntLe()
        return when (flags) {
            0 -> CompressionAlgorithm.NONE
            1 -> CompressionAlgorithm.GZIP
            else -> throw KdbxParseException("Unknown compression algorithm: $flags")
        }
    }

    private fun parseInnerStreamCipher(id: Int): InnerStreamCipher = when (id) {
        0 -> InnerStreamCipher.NONE
        1 -> InnerStreamCipher.ARC4
        2 -> InnerStreamCipher.SALSA20
        3 -> InnerStreamCipher.CHACHA20
        else -> throw KdbxParseException("Unknown inner stream cipher: $id")
    }

    private fun parseKdfParameters(data: ByteArray): KdfParameters {
        val buf = Buffer().apply { write(data) }
        val variantMap = parseVariantDictionary(buf)

        val uuidBytes = variantMap["\$UUID"] as? ByteArray
            ?: throw KdbxParseException("KDF parameters missing \$UUID")

        return when {
            uuidBytes.contentEquals(KDF_AES_UUID) -> KdfParameters.AesKdf(
                rounds = (variantMap["R"] as? Long) ?: 6000L,
                seed = (variantMap["S"] as? ByteArray) ?: ByteArray(0),
            )
            uuidBytes.contentEquals(KDF_ARGON2D_UUID) -> parseArgon2(Argon2Variant.ARGON2D, variantMap)
            uuidBytes.contentEquals(KDF_ARGON2ID_UUID) -> parseArgon2(Argon2Variant.ARGON2ID, variantMap)
            else -> throw KdbxParseException("Unknown KDF UUID")
        }
    }

    private fun parseArgon2(variant: Argon2Variant, map: Map<String, Any>) = KdfParameters.Argon2(
        variant = variant,
        version = (map["V"] as? Int) ?: 0x13,
        salt = (map["S"] as? ByteArray) ?: ByteArray(0),
        parallelism = (map["P"] as? Int) ?: 2,
        memory = (map["M"] as? Long) ?: 65536L,
        iterations = (map["I"] as? Long) ?: 3L,
    )

    private fun parseVariantDictionary(buf: Buffer): Map<String, Any> {
        val version = buf.readShortLe().toInt() and 0xFFFF
        if (version and 0xFF00 != 0x0100) {
            throw KdbxParseException("Unsupported variant dictionary version: 0x${version.toHex()}")
        }

        val map = mutableMapOf<String, Any>()

        while (true) {
            val type = buf.readByte().toInt() and 0xFF
            if (type == 0x00) break

            val keyLength = buf.readIntLe()
            val key = buf.readUtf8(keyLength.toLong())
            val valueLength = buf.readIntLe()

            val value: Any? = when (type) {
                TYPE_UINT32 -> buf.readIntLe()
                TYPE_UINT64 -> buf.readLongLe()
                TYPE_BOOL -> buf.readByte().toInt() != 0
                TYPE_INT32 -> buf.readIntLe()
                TYPE_INT64 -> buf.readLongLe()
                TYPE_STRING -> buf.readUtf8(valueLength.toLong())
                TYPE_BYTE_ARRAY -> buf.readByteArray(valueLength.toLong())
                else -> {
                    buf.skip(valueLength.toLong())
                    null
                }
            }

            if (value != null) {
                map[key] = value
            }
        }

        return map
    }

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

        // Variant dictionary value types
        private const val TYPE_UINT32 = 0x04
        private const val TYPE_UINT64 = 0x05
        private const val TYPE_BOOL = 0x08
        private const val TYPE_INT32 = 0x0C
        private const val TYPE_INT64 = 0x0D
        private const val TYPE_STRING = 0x18
        private const val TYPE_BYTE_ARRAY = 0x42

        // Cipher UUIDs (16 bytes, RFC 4122 byte order)
        private val CIPHER_AES_UUID = hexToBytes("31c1f2e6bf714350be5805216afc5aff")
        private val CIPHER_TWOFISH_UUID = hexToBytes("ad68f29f576f4bb9a36ad47af965346c")
        private val CIPHER_CHACHA20_UUID = hexToBytes("d6038a2b8b6f4cb5a524339a31dbb59a")

        // KDF UUIDs
        private val KDF_AES_UUID = hexToBytes("c9d9f39a628a4460bf740d08c18a4fea")
        private val KDF_ARGON2D_UUID = hexToBytes("ef636ddf8c29444b91f7a9a403e30a0c")
        private val KDF_ARGON2ID_UUID = hexToBytes("9e298b1956db4773b23dfc3ec6f0a1e6")

        private fun hexToBytes(hex: String): ByteArray =
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        private fun Int.toHex(): String = toUInt().toString(16)
    }
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    if (this.size != other.size) return false
    for (i in this.indices) {
        if (this[i] != other[i]) return false
    }
    return true
}
