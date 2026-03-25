package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests for ChaCha20 (IETF variant, RFC 8439).
 *
 * Uses RFC 8439 Section 2.4.2 test vector plus round-trip and edge-case tests.
 * Tests run on JVM (BouncyCastle) and validate the pure Kotlin implementation
 * produces identical output.
 */
class PlatformCryptoProviderChaCha20Test {

    private val crypto = PlatformCryptoProvider()

    // RFC 8439 Section 2.4.2 test vector
    private val rfc8439Key = byteArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
    )
    private val rfc8439Nonce = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09.toByte(),
        0x00, 0x00, 0x00, 0x4a, // 12-byte nonce
    )
    // "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it."
    private val rfc8439Plaintext =
        "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it."
            .encodeToByteArray()

    // Expected ciphertext from RFC 8439 Section 2.4.2
    // Note: RFC 8439 uses initial counter = 1 for this test vector.
    // KeePass uses initial counter = 0, so we test round-trip behavior
    // and cross-validate the pure Kotlin impl against BouncyCastle.

    @Test
    fun chaCha20_outputLength_matchesInput() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }

        for (len in listOf(0, 1, 15, 16, 63, 64, 65, 128, 256, 1000)) {
            val data = ByteArray(len) { (it % 251).toByte() }
            val result = crypto.chaCha20(data, key, nonce)
            assertEquals(len, result.size, "Output length mismatch for input length $len")
        }
    }

    @Test
    fun chaCha20_emptyInput_returnsEmpty() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val result = crypto.chaCha20(ByteArray(0), key, nonce)
        assertEquals(0, result.size)
    }

    @Test
    fun chaCha20_roundTrip_shortMessage() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = "Hello, ChaCha20!".encodeToByteArray()

        val encrypted = crypto.chaCha20(plaintext, key, nonce)
        assertFalse(encrypted.contentEquals(plaintext), "Encrypted should differ from plaintext")

        val decrypted = crypto.chaCha20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun chaCha20_roundTrip_exactlyOneBlock() {
        // 64 bytes = exactly one ChaCha20 block
        val key = ByteArray(32) { (it * 3).toByte() }
        val nonce = ByteArray(12) { (it * 7).toByte() }
        val plaintext = ByteArray(64) { (it * 11).toByte() }

        val encrypted = crypto.chaCha20(plaintext, key, nonce)
        val decrypted = crypto.chaCha20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun chaCha20_roundTrip_multiBlock() {
        // 200 bytes spans multiple blocks
        val key = ByteArray(32) { (it xor 0xAA).toByte() }
        val nonce = ByteArray(12) { (it xor 0x55).toByte() }
        val plaintext = ByteArray(200) { (it * 17 + 3).toByte() }

        val encrypted = crypto.chaCha20(plaintext, key, nonce)
        val decrypted = crypto.chaCha20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun chaCha20_roundTrip_largeData() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = ByteArray(10_000) { (it % 256).toByte() }

        val encrypted = crypto.chaCha20(plaintext, key, nonce)
        val decrypted = crypto.chaCha20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun chaCha20_keySensitivity() {
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = ByteArray(64) { 0x42 }

        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }

        val enc1 = crypto.chaCha20(plaintext, key1, nonce)
        val enc2 = crypto.chaCha20(plaintext, key2, nonce)
        assertFalse(enc1.contentEquals(enc2), "Different keys must produce different ciphertext")
    }

    @Test
    fun chaCha20_nonceSensitivity() {
        val key = ByteArray(32) { it.toByte() }
        val plaintext = ByteArray(64) { 0x42 }

        val nonce1 = ByteArray(12) { it.toByte() }
        val nonce2 = ByteArray(12) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }

        val enc1 = crypto.chaCha20(plaintext, key, nonce1)
        val enc2 = crypto.chaCha20(plaintext, key, nonce2)
        assertFalse(enc1.contentEquals(enc2), "Different nonces must produce different ciphertext")
    }

    @Test
    fun chaCha20_deterministic() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = "determinism test".encodeToByteArray()

        val enc1 = crypto.chaCha20(plaintext, key, nonce)
        val enc2 = crypto.chaCha20(plaintext, key, nonce)
        assertContentEquals(enc1, enc2)
    }

    @Test
    fun chaCha20_singleByte() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = byteArrayOf(0x42)

        val encrypted = crypto.chaCha20(plaintext, key, nonce)
        assertEquals(1, encrypted.size)
        val decrypted = crypto.chaCha20(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun chaCha20_pureKotlin_matchesBouncyCastle() {
        // Cross-validate: pure Kotlin ChaCha20 must produce the same output as BouncyCastle
        val key = rfc8439Key
        val nonce = rfc8439Nonce
        val plaintext = rfc8439Plaintext

        val bouncyCastleResult = crypto.chaCha20(plaintext, key, nonce)
        val pureKotlinResult = ChaCha20.encrypt(plaintext, key, nonce)

        assertContentEquals(
            bouncyCastleResult,
            pureKotlinResult,
            "Pure Kotlin ChaCha20 must match BouncyCastle output",
        )
    }

    @Test
    fun chaCha20_pureKotlin_matchesBouncyCastle_multiBlock() {
        val key = ByteArray(32) { (it * 5 + 7).toByte() }
        val nonce = ByteArray(12) { (it * 3 + 1).toByte() }
        val plaintext = ByteArray(300) { (it * 13).toByte() }

        val bouncyCastleResult = crypto.chaCha20(plaintext, key, nonce)
        val pureKotlinResult = ChaCha20.encrypt(plaintext, key, nonce)

        assertContentEquals(bouncyCastleResult, pureKotlinResult)
    }

    @Test
    fun chaCha20_pureKotlin_roundTrip() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { it.toByte() }
        val plaintext = "Pure Kotlin round-trip test".encodeToByteArray()

        val encrypted = ChaCha20.encrypt(plaintext, key, nonce)
        val decrypted = ChaCha20.encrypt(encrypted, key, nonce)
        assertContentEquals(plaintext, decrypted)
    }
}
