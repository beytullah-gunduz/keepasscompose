package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests for Twofish-256-CBC encryption/decryption with PKCS7 padding.
 *
 * Cross-validates the pure Kotlin implementation against BouncyCastle.
 */
class PlatformCryptoProviderTwofishTest {

    private val crypto = PlatformCryptoProvider()

    private val testKey = ByteArray(32) { it.toByte() }
    private val testIv = ByteArray(16) { (it + 0x10).toByte() }

    @Test
    fun twofish_roundTrip_singleBlock() {
        val plaintext = "0123456789abcdef".encodeToByteArray() // 16 bytes
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_roundTrip_empty() {
        val plaintext = ByteArray(0)
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        assertEquals(16, encrypted.size, "Empty input should produce one padding block")
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_roundTrip_singleByte() {
        val plaintext = byteArrayOf(0x42)
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_roundTrip_nonAligned() {
        val plaintext = ByteArray(25) { (it * 7).toByte() }
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        assertEquals(32, encrypted.size, "25 bytes + 7 padding = 32")
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_roundTrip_multiBlock() {
        val plaintext = ByteArray(200) { (it * 13 + 3).toByte() }
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_roundTrip_largeData() {
        val plaintext = ByteArray(10_000) { (it % 256).toByte() }
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.twofishDecrypt(encrypted, testKey, testIv)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun twofish_keySensitivity() {
        val plaintext = ByteArray(32) { 0x42 }
        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }

        val enc1 = crypto.twofishEncrypt(plaintext, key1, testIv)
        val enc2 = crypto.twofishEncrypt(plaintext, key2, testIv)
        assertFalse(enc1.contentEquals(enc2), "Different keys must produce different ciphertext")
    }

    @Test
    fun twofish_ivSensitivity() {
        val plaintext = ByteArray(32) { 0x42 }
        val iv1 = ByteArray(16) { it.toByte() }
        val iv2 = ByteArray(16) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }

        val enc1 = crypto.twofishEncrypt(plaintext, testKey, iv1)
        val enc2 = crypto.twofishEncrypt(plaintext, testKey, iv2)
        assertFalse(enc1.contentEquals(enc2), "Different IVs must produce different ciphertext")
    }

    @Test
    fun twofish_deterministic() {
        val plaintext = "determinism test".encodeToByteArray()
        val enc1 = crypto.twofishEncrypt(plaintext, testKey, testIv)
        val enc2 = crypto.twofishEncrypt(plaintext, testKey, testIv)
        assertContentEquals(enc1, enc2)
    }

    @Test
    fun twofish_encryptedDiffersFromPlaintext() {
        val plaintext = ByteArray(32) { it.toByte() }
        val encrypted = crypto.twofishEncrypt(plaintext, testKey, testIv)
        assertFalse(
            encrypted.copyOf(plaintext.size).contentEquals(plaintext),
            "Ciphertext should differ from plaintext",
        )
    }

    @Test
    fun twofish_pureKotlin_matchesBouncyCastle_encrypt() {
        val key = ByteArray(32) { (it * 5 + 7).toByte() }
        val iv = ByteArray(16) { (it * 3 + 1).toByte() }
        val plaintext = ByteArray(100) { (it * 11).toByte() }

        val bcResult = crypto.twofishEncrypt(plaintext, key, iv)
        val pkResult = Twofish.encryptCbc(plaintext, key, iv)
        assertContentEquals(bcResult, pkResult, "Pure Kotlin must match BouncyCastle encrypt")
    }

    @Test
    fun twofish_pureKotlin_matchesBouncyCastle_decrypt() {
        val key = ByteArray(32) { (it * 5 + 7).toByte() }
        val iv = ByteArray(16) { (it * 3 + 1).toByte() }
        val plaintext = ByteArray(100) { (it * 11).toByte() }

        // Encrypt with BouncyCastle, decrypt with pure Kotlin
        val encrypted = crypto.twofishEncrypt(plaintext, key, iv)
        val decrypted = Twofish.decryptCbc(encrypted, key, iv)
        assertContentEquals(plaintext, decrypted, "Pure Kotlin decrypt must match BouncyCastle encrypt")
    }

    @Test
    fun twofish_pureKotlin_matchesBouncyCastle_multiBlock() {
        val key = ByteArray(32) { (it xor 0xAA).toByte() }
        val iv = ByteArray(16) { (it xor 0x55).toByte() }
        val plaintext = ByteArray(300) { (it * 17).toByte() }

        val bcResult = crypto.twofishEncrypt(plaintext, key, iv)
        val pkResult = Twofish.encryptCbc(plaintext, key, iv)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun twofish_pureKotlin_roundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val iv = ByteArray(16) { it.toByte() }
        val plaintext = "Pure Kotlin round-trip test".encodeToByteArray()

        val encrypted = Twofish.encryptCbc(plaintext, key, iv)
        val decrypted = Twofish.decryptCbc(encrypted, key, iv)
        assertContentEquals(plaintext, decrypted)
    }
}
