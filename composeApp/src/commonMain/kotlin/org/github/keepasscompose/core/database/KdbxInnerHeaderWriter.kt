package org.github.keepasscompose.core.database

import okio.Buffer
import okio.BufferedSink

/**
 * Writes the KDBX 4.x inner header to a sink.
 *
 * The inner header is written after encryption/decryption boundaries and before the XML payload.
 * It contains the inner random stream parameters and binary attachments.
 *
 * Field format: 1 byte ID + 4 bytes LE length + N bytes data.
 *
 * @see KdbxInnerHeaderReader
 */
class KdbxInnerHeaderWriter {
    /**
     * Writes the inner header fields to [sink].
     *
     * @param sink The output sink.
     * @param innerRandomStreamId The inner random stream cipher ID.
     * @param innerRandomStreamKey The inner random stream cipher key.
     * @param binaries The binary attachment pool to serialize.
     */
    fun writeInnerHeader(sink: BufferedSink, innerRandomStreamId: Int, innerRandomStreamKey: ByteArray, binaries: KdbxBinaryPool) {
        // 1. Write inner random stream ID
        writeField(sink, FIELD_INNER_RANDOM_STREAM_ID, intToLittleEndianBytes(innerRandomStreamId))

        // 2. Write inner random stream key
        writeField(sink, FIELD_INNER_RANDOM_STREAM_KEY, innerRandomStreamKey)

        // 3. Write binary attachments in order of their IDs
        val sortedEntries = binaries.entries().entries.sortedBy { it.key }
        for ((_, data) in sortedEntries) {
            // Prepend the flags byte (0x01 = memory protection)
            val fieldData = ByteArray(1 + data.size)
            fieldData[0] = BINARY_FLAG_MEMORY_PROTECTION
            data.copyInto(fieldData, 1)
            writeField(sink, FIELD_BINARY, fieldData)
        }

        // 4. Write end-of-inner-header
        writeField(sink, FIELD_END_OF_INNER_HEADER, ByteArray(0))
    }

    private fun writeField(sink: BufferedSink, fieldId: Int, data: ByteArray) {
        sink.writeByte(fieldId)
        sink.writeIntLe(data.size)
        sink.write(data)
    }

    companion object {
        private const val FIELD_END_OF_INNER_HEADER = 0x00
        private const val FIELD_INNER_RANDOM_STREAM_ID = 0x01
        private const val FIELD_INNER_RANDOM_STREAM_KEY = 0x02
        private const val FIELD_BINARY = 0x03

        private const val BINARY_FLAG_MEMORY_PROTECTION: Byte = 0x01

        private fun intToLittleEndianBytes(value: Int): ByteArray = Buffer().apply { writeIntLe(value) }.readByteArray()
    }
}
