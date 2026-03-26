package org.github.keepasscompose.core.database

import okio.BufferedSource

/**
 * Result of reading the KDBX 4.x inner header.
 *
 * The inner header appears after decryption and (optional) decompression,
 * immediately before the XML payload. It contains:
 * - The inner random stream cipher ID and key (for protected field encryption)
 * - Binary attachments (stored outside XML in KDBX 4.x)
 *
 * @param innerRandomStreamId The inner random stream cipher identifier.
 * @param innerRandomStreamKey The key for the inner random stream cipher.
 * @param binaries The binary attachment pool parsed from inner header fields.
 */
data class KdbxInnerHeaderReadResult(val innerRandomStreamId: Int, val innerRandomStreamKey: ByteArray, val binaries: KdbxBinaryPool) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxInnerHeaderReadResult) return false
        return innerRandomStreamId == other.innerRandomStreamId &&
            innerRandomStreamKey.contentEquals(other.innerRandomStreamKey)
    }

    override fun hashCode(): Int {
        var result = innerRandomStreamId
        result = 31 * result + innerRandomStreamKey.contentHashCode()
        return result
    }
}

/**
 * Reads the KDBX 4.x inner header from a decrypted stream.
 *
 * The inner header format is a sequence of fields:
 * - 1 byte: field ID
 * - 4 bytes (LE): field data length
 * - N bytes: field data
 *
 * Field IDs:
 * - 0x00: End of inner header
 * - 0x01: Inner random stream ID (4 bytes, LE uint32)
 * - 0x02: Inner random stream key (variable length)
 * - 0x03: Binary attachment (first byte = flags, rest = data)
 *
 * Binary flags byte:
 * - Bit 0 (0x01): Memory protection flag (data should be protected in memory)
 */
class KdbxInnerHeaderReader {
    fun readInnerHeader(source: BufferedSource): KdbxInnerHeaderReadResult {
        var innerStreamId = 0
        var innerStreamKey = ByteArray(0)
        val binaries = KdbxBinaryPool()
        var binaryIndex = 0

        while (true) {
            val fieldId = source.readByte().toInt() and 0xFF
            val fieldSize = source.readIntLe()

            if (fieldId == FIELD_END_OF_INNER_HEADER) {
                if (fieldSize > 0) {
                    source.skip(fieldSize.toLong())
                }
                break
            }

            when (fieldId) {
                FIELD_INNER_RANDOM_STREAM_ID -> {
                    val data = source.readByteArray(fieldSize.toLong())
                    innerStreamId = data.toLittleEndianInt()
                }

                FIELD_INNER_RANDOM_STREAM_KEY -> {
                    innerStreamKey = source.readByteArray(fieldSize.toLong())
                }

                FIELD_BINARY -> {
                    val data = source.readByteArray(fieldSize.toLong())
                    if (data.isNotEmpty()) {
                        // First byte is flags, rest is the actual binary data
                        val binaryData = data.copyOfRange(1, data.size)
                        binaries.put(binaryIndex, binaryData)
                    }
                    binaryIndex++
                }

                else -> {
                    // Skip unknown fields
                    source.skip(fieldSize.toLong())
                }
            }
        }

        return KdbxInnerHeaderReadResult(
            innerRandomStreamId = innerStreamId,
            innerRandomStreamKey = innerStreamKey,
            binaries = binaries,
        )
    }

    companion object {
        private const val FIELD_END_OF_INNER_HEADER = 0x00
        private const val FIELD_INNER_RANDOM_STREAM_ID = 0x01
        private const val FIELD_INNER_RANDOM_STREAM_KEY = 0x02
        private const val FIELD_BINARY = 0x03

        private fun ByteArray.toLittleEndianInt(): Int {
            var result = 0
            for (i in indices.take(4)) {
                result = result or ((this[i].toInt() and 0xFF) shl (i * 8))
            }
            return result
        }
    }
}
