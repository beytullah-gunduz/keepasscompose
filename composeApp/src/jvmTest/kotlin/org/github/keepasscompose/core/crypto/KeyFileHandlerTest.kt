package org.github.keepasscompose.core.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [KeyFileHandler].
 *
 * Covers XML key file v1.0/v2.0, binary 32-byte, hex-encoded, and arbitrary file formats,
 * as well as key file generation.
 */
@OptIn(ExperimentalEncodingApi::class)
class KeyFileHandlerTest {
    private val crypto = PlatformCryptoProvider()

    // --- XML key file v1.0 ---

    @Test
    fun parseKeyFile_xmlV1() {
        val keyData = ByteArray(32) { it.toByte() }
        val xml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <KeyFile>
                <Meta>
                    <Version>1.0</Version>
                </Meta>
                <Key>
                    <Data>${Base64.encode(keyData)}</Data>
                </Key>
            </KeyFile>
            """.trimIndent()
        val result = KeyFileHandler.parseKeyFile(xml.encodeToByteArray())
        assertContentEquals(keyData, result)
    }

    @Test
    fun parseKeyFile_xmlV2() {
        val keyData = ByteArray(32) { (it * 7).toByte() }
        val hash = crypto.sha256(keyData)
        val hashHex = hash.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }.uppercase()
        val xml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <KeyFile>
                <Meta>
                    <Version>2.0</Version>
                </Meta>
                <Key>
                    <Data Hash="$hashHex">${Base64.encode(keyData)}</Data>
                </Key>
            </KeyFile>
            """.trimIndent()
        val result = KeyFileHandler.parseKeyFile(xml.encodeToByteArray())
        assertContentEquals(keyData, result)
    }

    @Test
    fun parseKeyFile_xmlWithWhitespace() {
        val keyData = ByteArray(32) { it.toByte() }
        val base64 = Base64.encode(keyData)
        // Insert line breaks in base64 content
        val brokenBase64 = base64.chunked(20).joinToString("\n    ")
        val xml =
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("<KeyFile>")
                appendLine("  <Meta><Version>1.0</Version></Meta>")
                appendLine("  <Key>")
                appendLine("    <Data>$brokenBase64</Data>")
                appendLine("  </Key>")
                appendLine("</KeyFile>")
            }
        val result = KeyFileHandler.parseKeyFile(xml.encodeToByteArray())
        assertContentEquals(keyData, result)
    }

    // --- Binary 32-byte key file ---

    @Test
    fun parseKeyFile_binary32() {
        val keyData = ByteArray(32) { (it * 3 + 1).toByte() }
        val result = KeyFileHandler.parseKeyFile(keyData)
        assertContentEquals(keyData, result)
    }

    // --- Hex-encoded key file ---

    @Test
    fun parseKeyFile_hex64chars() {
        val keyData = ByteArray(32) { it.toByte() }
        val hex = keyData.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        val result = KeyFileHandler.parseKeyFile(hex.encodeToByteArray())
        assertContentEquals(keyData, result)
    }

    @Test
    fun parseKeyFile_hexUppercase() {
        val keyData = ByteArray(32) { it.toByte() }
        val hex = keyData.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }.uppercase()
        val result = KeyFileHandler.parseKeyFile(hex.encodeToByteArray())
        assertContentEquals(keyData, result)
    }

    @Test
    fun parseKeyFile_hex64NotValidHex_treatedAsArbitrary() {
        // 64 bytes that aren't valid hex characters
        val data = ByteArray(64) { 0xFF.toByte() }
        val result = KeyFileHandler.parseKeyFile(data)
        // Should return as-is since it's not valid hex and not valid XML
        assertContentEquals(data, result)
    }

    // --- Arbitrary binary file ---

    @Test
    fun parseKeyFile_arbitraryBinary() {
        val data = ByteArray(100) { (it * 11).toByte() }
        val result = KeyFileHandler.parseKeyFile(data)
        assertContentEquals(data, result)
    }

    @Test
    fun parseKeyFile_arbitrarySmallFile() {
        val data = ByteArray(5) { it.toByte() }
        val result = KeyFileHandler.parseKeyFile(data)
        assertContentEquals(data, result)
    }

    @Test
    fun parseKeyFile_arbitraryLargeFile() {
        val data = ByteArray(10_000) { (it % 256).toByte() }
        val result = KeyFileHandler.parseKeyFile(data)
        assertContentEquals(data, result)
    }

    // --- Key file generation ---

    @Test
    fun generateKeyFile_producesValidXml() {
        val keyFileBytes = KeyFileHandler.generateKeyFile(crypto, Random(42))
        val parsed = KeyFileHandler.parseKeyFile(keyFileBytes)
        assertEquals(32, parsed.size)
    }

    @Test
    fun generateKeyFile_roundTrip() {
        val keyFileBytes = KeyFileHandler.generateKeyFile(crypto, Random(123))
        val keyData = KeyFileHandler.parseKeyFile(keyFileBytes)
        assertEquals(32, keyData.size)
        // Parse again — should produce same result
        val keyData2 = KeyFileHandler.parseKeyFile(keyFileBytes)
        assertContentEquals(keyData, keyData2)
    }

    @Test
    fun generateKeyFile_containsVersion2() {
        val keyFileBytes = KeyFileHandler.generateKeyFile(crypto, Random(99))
        val xml = keyFileBytes.decodeToString()
        assertTrue(xml.contains("<Version>2.0</Version>"))
    }

    @Test
    fun generateKeyFile_differentSeeds_differentKeys() {
        val kf1 = KeyFileHandler.generateKeyFile(crypto, Random(1))
        val kf2 = KeyFileHandler.generateKeyFile(crypto, Random(2))
        val key1 = KeyFileHandler.parseKeyFile(kf1)
        val key2 = KeyFileHandler.parseKeyFile(kf2)
        assertTrue(!key1.contentEquals(key2))
    }
}
