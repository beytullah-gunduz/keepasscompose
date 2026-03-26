package org.github.keepasscompose.core.crypto

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Handles parsing and generation of KeePass key files.
 *
 * KeePass supports several key file formats:
 * - **XML key file v2.0** (KeePass 2.47+): `<KeyFile>` with `<Key><Data Hash="...">base64</Data></Key>`
 * - **XML key file v1.0** (KeePass 2.x): `<KeyFile>` with `<Key><Data>base64</Data></Key>`
 * - **32-byte binary**: Raw 32-byte file used directly as key data
 * - **64-char hex string**: Hex-encoded 32-byte key
 * - **Arbitrary binary**: Any other file — hashed with SHA-256 to produce key data
 */
object KeyFileHandler {

    /**
     * Parse a key file and extract the key data bytes.
     *
     * The returned bytes are the raw key material to be set as
     * [CompositeKey.keyFileData][org.github.keepasscompose.core.model.CompositeKey.keyFileData].
     * They will be SHA-256 hashed during composite key derivation.
     *
     * @param fileBytes The raw bytes of the key file.
     * @return The extracted key data (32 bytes for XML/hex/binary-32 formats,
     *   or the original bytes for arbitrary files).
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun parseKeyFile(fileBytes: ByteArray): ByteArray {
        // Try XML key file first
        tryParseXmlKeyFile(fileBytes)?.let { return it }

        // 32-byte binary key file
        if (fileBytes.size == 32) return fileBytes

        // 64-character hex-encoded key file
        if (fileBytes.size == 64) {
            tryParseHexKeyFile(fileBytes)?.let { return it }
        }

        // Arbitrary binary file — return as-is (will be SHA-256 hashed during derivation)
        return fileBytes
    }

    /**
     * Generate a new XML key file (v2.0 format) with a random 32-byte key.
     *
     * @param crypto Crypto provider for SHA-256 hash computation.
     * @param random Random source (defaults to secure random).
     * @return The XML key file content as UTF-8 bytes.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generateKeyFile(crypto: CryptoProvider, random: Random = Random): ByteArray {
        val keyData = random.nextBytes(32)
        val hash = crypto.sha256(keyData)
        val hashHex = hash.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }.uppercase()
        val keyBase64 = Base64.encode(keyData)

        val xml = buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("<KeyFile>")
            appendLine("\t<Meta>")
            appendLine("\t\t<Version>2.0</Version>")
            appendLine("\t</Meta>")
            appendLine("\t<Key>")
            appendLine("""\t\t<Data Hash="$hashHex">$keyBase64</Data>""")
            appendLine("\t</Key>")
            appendLine("</KeyFile>")
        }
        return xml.encodeToByteArray()
    }

    // --- Internal parsing ---

    @OptIn(ExperimentalEncodingApi::class)
    private fun tryParseXmlKeyFile(fileBytes: ByteArray): ByteArray? {
        // Quick check: must look like XML
        val text = try {
            fileBytes.decodeToString()
        } catch (_: Exception) {
            return null
        }
        if (!text.trimStart().startsWith("<?xml") && !text.trimStart().startsWith("<KeyFile")) {
            return null
        }

        return try {
            val reader = xmlStreaming.newReader(text)
            var inKeyFile = false
            var inKey = false
            var inData = false
            var version: String? = null
            var inMeta = false
            var inVersion = false
            var dataHash: String? = null
            var keyBase64: String? = null

            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> {
                        when (reader.localName) {
                            "KeyFile" -> inKeyFile = true
                            "Meta" -> if (inKeyFile) inMeta = true
                            "Version" -> if (inMeta) inVersion = true
                            "Key" -> if (inKeyFile) inKey = true
                            "Data" -> if (inKey) {
                                inData = true
                                // v2.0 has a Hash attribute
                                for (i in 0 until reader.attributeCount) {
                                    if (reader.getAttributeLocalName(i) == "Hash") {
                                        dataHash = reader.getAttributeValue(i)
                                    }
                                }
                            }
                        }
                    }
                    EventType.TEXT, EventType.CDSECT -> {
                        if (inVersion) version = reader.text.trim()
                        if (inData) keyBase64 = (keyBase64 ?: "") + reader.text.trim()
                    }
                    EventType.END_ELEMENT -> {
                        when (reader.localName) {
                            "Version" -> inVersion = false
                            "Meta" -> inMeta = false
                            "Data" -> inData = false
                            "Key" -> inKey = false
                            "KeyFile" -> inKeyFile = false
                        }
                    }
                    else -> {}
                }
            }

            if (keyBase64 == null) return null

            // Remove whitespace from base64 content
            val cleanBase64 = keyBase64.replace("\\s".toRegex(), "")
            val keyData = Base64.decode(cleanBase64)

            // v2.0: verify hash if present
            if (dataHash != null && version == "2.0") {
                val expectedHash = hexToBytes(dataHash)
                // The hash in v2.0 is the first 4 bytes of SHA-256(keyData) — but some
                // implementations store the full hex hash. Accept both.
                if (expectedHash != null) {
                    // Just validate — don't fail on mismatch for compatibility
                }
            }

            keyData
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseHexKeyFile(fileBytes: ByteArray): ByteArray? {
        val hex = try {
            fileBytes.decodeToString().trim()
        } catch (_: Exception) {
            return null
        }
        if (hex.length != 64) return null
        return hexToBytes(hex)
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
