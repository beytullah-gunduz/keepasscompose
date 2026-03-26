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

    // -- NIST AES-256-ECB known answer test --
    // AES-KDF uses AES-256-ECB internally. Verify the pure Kotlin Aes.encryptBlock
    // against the NIST SP 800-38A Section F.1.5 ECB-AES256 test vector.

    @Test
    fun aesEcb_nistVector() {
        // NIST SP 800-38A F.1.5: AES-256 ECB Encrypt
        val key = hexToBytes("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")
        val plaintext = hexToBytes("6bc1bee22e409f96e93d7e117393172a")
        val expected = hexToBytes("f3eed1bdb5d2a03c064b5a7e3db181f8")

        val roundKeys = Aes.expandKey(key)
        val block = plaintext.copyOf()
        Aes.encryptBlock(block, 0, roundKeys)
        assertContentEquals(expected, block, "Must match NIST SP 800-38A AES-256-ECB test vector")
    }

    @Test
    fun aesEcb_nistVector_block2() {
        // NIST SP 800-38A F.1.5: second plaintext block
        val key = hexToBytes("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")
        val plaintext = hexToBytes("ae2d8a571e03ac9c9eb76fac45af8e51")
        val expected = hexToBytes("591ccb10d410ed26dc5ba74a31362870")

        val roundKeys = Aes.expandKey(key)
        val block = plaintext.copyOf()
        Aes.encryptBlock(block, 0, roundKeys)
        assertContentEquals(expected, block, "Must match NIST AES-256-ECB block 2")
    }

    @Test
    fun aesKdf_knownOutput() {
        // Hardcoded known-good output for regression testing.
        // AES-KDF with all-zero key and all-zero seed, 1 round:
        // Result = AES-ECB(key=0, block0=0) || AES-ECB(key=0, block1=0)
        // NIST: AES-256-ECB(key=0, pt=0) = dc95c078a2408989ad48a21492842087
        val key = ByteArray(32)
        val seed = ByteArray(32)
        val result = crypto.aesKdf(key, seed, 1)
        val pkResult = AesKdf.transform(key, seed, 1)
        assertContentEquals(result, pkResult)
        // Verify first 16 bytes match known AES-256-ECB(key=0, pt=0)
        assertEquals("dc95c078a2408989ad48a21492842087", result.copyOf(16).toHex())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
