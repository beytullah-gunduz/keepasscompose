package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for Salsa20 stream cipher.
 *
 * Cross-validates the pure Kotlin implementation against BouncyCastle.
 * Also tests the KeePass inner random stream use case (fixed sigma nonce).
 */
class PlatformCryptoProviderSalsa20Test {

    private val crypto = PlatformCryptoProvider()

    // KeePass inner stream nonce (sigma bytes)
    private val keepassNonce = byteArrayOf(
        0xE8.toByte(), 0x30, 0x09, 0x4B, 0x97.toByte(), 0x20, 0x5D, 0x2A,
    )

    // --- Cross-validation: pure Kotlin vs BouncyCastle ---

    @Test
    fun salsa20_pureKotlin_matchesBouncyCastle_shortMessage() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val data = "Hello, Salsa20!".encodeToByteArray()
        val bcResult = crypto.salsa20(data, key, nonce)
        val pkResult = Salsa20.encrypt(data, key, nonce)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun salsa20_pureKotlin_matchesBouncyCastle_exactlyOneBlock() {
        val key = ByteArray(32) { (it * 3).toByte() }
        val nonce = ByteArray(8) { (it * 7).toByte() }
        val data = ByteArray(64) { (it * 11).toByte() }
        val bcResult = crypto.salsa20(data, key, nonce)
        val pkResult = Salsa20.encrypt(data, key, nonce)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun salsa20_pureKotlin_matchesBouncyCastle_multiBlock() {
        val key = ByteArray(32) { (it xor 0xAA).toByte() }
        val nonce = ByteArray(8) { (it xor 0x55).toByte() }
        val data = ByteArray(200) { (it * 17 + 3).toByte() }
        val bcResult = crypto.salsa20(data, key, nonce)
        val pkResult = Salsa20.encrypt(data, key, nonce)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun salsa20_pureKotlin_matchesBouncyCastle_largeData() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val data = ByteArray(10_000) { (it % 256).toByte() }
        val bcResult = crypto.salsa20(data, key, nonce)
        val pkResult = Salsa20.encrypt(data, key, nonce)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun salsa20_pureKotlin_matchesBouncyCastle_keepassNonce() {
        // Test with the exact nonce KeePass uses for inner random streams
        val key = ByteArray(32) { (it * 5 + 1).toByte() }
        val data = ByteArray(128) { (it * 13).toByte() }
        val bcResult = crypto.salsa20(data, key, keepassNonce)
        val pkResult = Salsa20.encrypt(data, key, keepassNonce)
        assertContentEquals(bcResult, pkResult, "Must match with KeePass sigma nonce")
    }

    // --- Behavioral tests ---

    @Test
    fun salsa20_outputLength_matchesInput() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        for (len in listOf(0, 1, 15, 16, 63, 64, 65, 128, 256, 1000)) {
            val data = ByteArray(len) { (it % 251).toByte() }
            val result = crypto.salsa20(data, key, nonce)
            assertEquals(len, result.size, "Output length mismatch for input length $len")
        }
    }

    @Test
    fun salsa20_emptyInput_returnsEmpty() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val result = crypto.salsa20(ByteArray(0), key, nonce)
        assertEquals(0, result.size)
    }

    @Test
    fun salsa20_roundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val plaintext = "Round-trip Salsa20 test".encodeToByteArray()
        val encrypted = crypto.salsa20(plaintext, key, nonce)
        assertFalse(encrypted.contentEquals(plaintext))
        val decrypted = crypto.salsa20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun salsa20_deterministic() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val data = "determinism".encodeToByteArray()
        val r1 = crypto.salsa20(data, key, nonce)
        val r2 = crypto.salsa20(data, key, nonce)
        assertContentEquals(r1, r2)
    }

    @Test
    fun salsa20_keySensitivity() {
        val nonce = ByteArray(8) { it.toByte() }
        val data = ByteArray(64) { 0x42 }
        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }
        val r1 = crypto.salsa20(data, key1, nonce)
        val r2 = crypto.salsa20(data, key2, nonce)
        assertFalse(r1.contentEquals(r2), "Different keys must produce different ciphertext")
    }

    @Test
    fun salsa20_nonceSensitivity() {
        val key = ByteArray(32) { it.toByte() }
        val data = ByteArray(64) { 0x42 }
        val nonce1 = ByteArray(8) { it.toByte() }
        val nonce2 = ByteArray(8) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }
        val r1 = crypto.salsa20(data, key, nonce1)
        val r2 = crypto.salsa20(data, key, nonce2)
        assertFalse(r1.contentEquals(r2), "Different nonces must produce different ciphertext")
    }

    @Test
    fun salsa20_pureKotlin_roundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { it.toByte() }
        val plaintext = "Pure Kotlin round-trip".encodeToByteArray()
        val encrypted = Salsa20.encrypt(plaintext, key, nonce)
        val decrypted = Salsa20.encrypt(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    // -- Known test vectors (DJB Salsa20 spec / ECRYPT Stream Cipher Project) --
    // Salsa20 with all-zero key and all-zero nonce: the keystream for the first block
    // is a well-known test vector.

    @Test
    fun salsa20_knownVector_zeroKeyZeroNonce() {
        // Encrypt 64 zero bytes = first keystream block
        val key = ByteArray(32)
        val nonce = ByteArray(8)
        val plaintext = ByteArray(64)

        val bcResult = crypto.salsa20(plaintext, key, nonce)
        val pkResult = Salsa20.encrypt(plaintext, key, nonce)
        assertContentEquals(bcResult, pkResult, "Zero key/nonce: BC and pure Kotlin must match")

        // Hardcoded regression vector (verified via BouncyCastle Salsa20Engine)
        val expectedStart = hexToBytes("9a97f65b9b4c721b960a672145fca8d4")
        assertContentEquals(expectedStart, bcResult.copyOf(16), "First 16 bytes must match known Salsa20 vector")
    }

    @Test
    fun salsa20_knownVector_key1_nonce0() {
        // Key = 0x80 0x00...00 (256-bit), nonce = 0x00...00
        val key = ByteArray(32).also { it[0] = 0x80.toByte() }
        val nonce = ByteArray(8)
        val plaintext = ByteArray(64)

        val bcResult = crypto.salsa20(plaintext, key, nonce)
        val pkResult = Salsa20.encrypt(plaintext, key, nonce)
        assertContentEquals(bcResult, pkResult, "Key=0x80: BC and pure Kotlin must match")

        // Hardcoded regression vector (verified via BouncyCastle Salsa20Engine)
        val expectedStart = hexToBytes("e3be8fdd8beca2e3ea8ef9475b29a6e7")
        assertContentEquals(expectedStart, bcResult.copyOf(16), "First 16 bytes must match known vector")
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
