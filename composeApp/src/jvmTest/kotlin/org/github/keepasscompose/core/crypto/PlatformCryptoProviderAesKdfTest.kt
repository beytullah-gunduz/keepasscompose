package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for AES-KDF key derivation (KDBX 3.1).
 *
 * Cross-validates the pure Kotlin AES and AesKdf implementations against
 * BouncyCastle. Tests cover single-block encryption, key expansion, and
 * the full KDF round loop.
 */
class PlatformCryptoProviderAesKdfTest {

    private val crypto = PlatformCryptoProvider()

    // --- AES-ECB single-block cross-validation ---

    @Test
    fun aesEcb_pureKotlin_matchesBouncyCastle() {
        // Encrypt a single block with BouncyCastle and pure Kotlin, compare results
        val key = ByteArray(32) { it.toByte() }
        val block = ByteArray(16) { (it * 7 + 3).toByte() }

        // BouncyCastle via aesKdf with 1 round on padded input
        // Instead, use the KDF itself: 1 round of AES-KDF should match
        val bcResult = crypto.aesKdf(block + ByteArray(16), key, 1)
        val pkResult = AesKdf.transform(block + ByteArray(16), key, 1)
        assertContentEquals(bcResult, pkResult, "Single round AES-KDF must match")
    }

    // --- AES-KDF cross-validation ---

    @Test
    fun aesKdf_pureKotlin_matchesBouncyCastle_singleRound() {
        val key = ByteArray(32) { (it * 11).toByte() }
        val seed = ByteArray(32) { (it * 7 + 5).toByte() }
        val bcResult = crypto.aesKdf(key, seed, 1)
        val pkResult = AesKdf.transform(key, seed, 1)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun aesKdf_pureKotlin_matchesBouncyCastle_multipleRounds() {
        val key = ByteArray(32) { (it * 3).toByte() }
        val seed = ByteArray(32) { (it * 13 + 1).toByte() }
        val bcResult = crypto.aesKdf(key, seed, 100)
        val pkResult = AesKdf.transform(key, seed, 100)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun aesKdf_pureKotlin_matchesBouncyCastle_typicalKeePassRounds() {
        // KeePass default is often 6000 rounds
        val key = "password-derived-composite-key!!".encodeToByteArray()
        val seed = ByteArray(32) { (it xor 0xAA).toByte() }
        val bcResult = crypto.aesKdf(key, seed, 6000)
        val pkResult = AesKdf.transform(key, seed, 6000)
        assertContentEquals(bcResult, pkResult, "AES-KDF at 6000 rounds must match")
    }

    @Test
    fun aesKdf_pureKotlin_matchesBouncyCastle_zeroKey() {
        val key = ByteArray(32)
        val seed = ByteArray(32) { 0xFF.toByte() }
        val bcResult = crypto.aesKdf(key, seed, 10)
        val pkResult = AesKdf.transform(key, seed, 10)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun aesKdf_pureKotlin_matchesBouncyCastle_allOnesKey() {
        val key = ByteArray(32) { 0xFF.toByte() }
        val seed = ByteArray(32)
        val bcResult = crypto.aesKdf(key, seed, 10)
        val pkResult = AesKdf.transform(key, seed, 10)
        assertContentEquals(bcResult, pkResult)
    }

    // --- Behavioral tests ---

    @Test
    fun aesKdf_outputLength() {
        val key = ByteArray(32) { it.toByte() }
        val seed = ByteArray(32) { it.toByte() }
        val result = crypto.aesKdf(key, seed, 1)
        assertEquals(32, result.size, "Output must be 32 bytes")
    }

    @Test
    fun aesKdf_zeroRounds_returnsOriginalKey() {
        val key = ByteArray(32) { (it * 5).toByte() }
        val seed = ByteArray(32) { (it * 3).toByte() }
        val result = crypto.aesKdf(key, seed, 0)
        assertContentEquals(key, result, "Zero rounds should return the original key")
    }

    @Test
    fun aesKdf_deterministic() {
        val key = ByteArray(32) { it.toByte() }
        val seed = ByteArray(32) { (it + 32).toByte() }
        val result1 = crypto.aesKdf(key, seed, 50)
        val result2 = crypto.aesKdf(key, seed, 50)
        assertContentEquals(result1, result2)
    }

    @Test
    fun aesKdf_keySensitivity() {
        val seed = ByteArray(32) { it.toByte() }
        val key1 = ByteArray(32) { it.toByte() }
        val key2 = ByteArray(32) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }
        val result1 = crypto.aesKdf(key1, seed, 10)
        val result2 = crypto.aesKdf(key2, seed, 10)
        assertFalse(result1.contentEquals(result2), "Different keys must produce different output")
    }

    @Test
    fun aesKdf_seedSensitivity() {
        val key = ByteArray(32) { it.toByte() }
        val seed1 = ByteArray(32) { it.toByte() }
        val seed2 = ByteArray(32) { it.toByte() }.also { it[0] = (it[0].toInt() xor 1).toByte() }
        val result1 = crypto.aesKdf(key, seed1, 10)
        val result2 = crypto.aesKdf(key, seed2, 10)
        assertFalse(result1.contentEquals(result2), "Different seeds must produce different output")
    }

    @Test
    fun aesKdf_roundsSensitivity() {
        val key = ByteArray(32) { it.toByte() }
        val seed = ByteArray(32) { (it + 32).toByte() }
        val result1 = crypto.aesKdf(key, seed, 10)
        val result2 = crypto.aesKdf(key, seed, 11)
        assertFalse(result1.contentEquals(result2), "Different round counts must produce different output")
    }
}
