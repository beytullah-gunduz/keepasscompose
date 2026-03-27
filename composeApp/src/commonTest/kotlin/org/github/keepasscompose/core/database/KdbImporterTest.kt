package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KdbImporterTest {
    private val crypto = PlatformCryptoProvider()
    private val importer = KdbImporter(crypto)

    // -- Signature validation --

    @Test
    fun parseHeaderRejectsFileTooSmall() {
        val tooSmall = ByteArray(50)
        assertFailsWith<KdbxParseException> {
            importer.import(tooSmall, "password")
        }
    }

    @Test
    fun parseHeaderRejectsInvalidSignature1() {
        val data = ByteArray(124)
        // Write wrong signature1
        writeUInt32(data, 0, 0x12345678L)
        writeUInt32(data, 4, KdbImporter.SIGNATURE_2)

        assertFailsWith<KdbxParseException>("Invalid KDB signature1") {
            importer.import(data, "password")
        }
    }

    @Test
    fun parseHeaderRejectsInvalidSignature2() {
        val data = ByteArray(124)
        writeUInt32(data, 0, KdbImporter.SIGNATURE_1)
        writeUInt32(data, 4, 0x12345678L)

        assertFailsWith<KdbxParseException>("Invalid KDB signature2") {
            importer.import(data, "password")
        }
    }

    @Test
    fun parseHeaderRejectsNonAesEncryption() {
        val data = ByteArray(124)
        writeUInt32(data, 0, KdbImporter.SIGNATURE_1)
        writeUInt32(data, 4, KdbImporter.SIGNATURE_2)
        // flags = 0 (no AES flag)
        writeUInt32(data, 8, 0L)

        assertFailsWith<KdbxParseException>("Only AES") {
            importer.import(data, "password")
        }
    }

    @Test
    fun parseHeaderParsesValidSignatures() {
        val data = ByteArray(200)
        writeUInt32(data, 0, KdbImporter.SIGNATURE_1)
        writeUInt32(data, 4, KdbImporter.SIGNATURE_2)
        writeUInt32(data, 8, 2L) // AES flag
        writeUInt32(data, 12, 0x00030004L) // version
        // masterSeed at 16..31 (leave as zeros)
        // encryptionIv at 32..47 (leave as zeros)
        writeUInt32(data, 48, 0L) // 0 groups
        writeUInt32(data, 52, 0L) // 0 entries
        // contentHash at 56..87 (leave as zeros)
        // transformSeed at 88..119 (leave as zeros)
        writeUInt32(data, 120, 1L) // 1 transform round

        val header = importer.parseHeader(data)
        assertEquals(0, header.groupCount)
        assertEquals(0, header.entryCount)
        assertEquals(1L, header.transformRounds)
    }

    @Test
    fun signatureConstantsAreCorrect() {
        assertEquals(0x9AA2D903L, KdbImporter.SIGNATURE_1)
        assertEquals(0xB54BFB65L, KdbImporter.SIGNATURE_2)
    }

    // -- Helpers --

    private fun writeUInt32(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}
