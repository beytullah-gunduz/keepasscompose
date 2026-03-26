package org.github.keepasscompose.core.crypto

import org.github.keepasscompose.core.model.Argon2Variant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for Argon2 key derivation (KDBX 4.x).
 *
 * Cross-validates the pure Kotlin implementation against BouncyCastle.
 * Tests cover both Argon2d and Argon2id variants.
 */
class PlatformCryptoProviderArgon2Test {

    private val crypto = PlatformCryptoProvider()

    // --- Cross-validation: pure Kotlin vs BouncyCastle ---

    @Test
    fun argon2d_pureKotlin_matchesBouncyCastle() {
        val password = "testpassword".encodeToByteArray()
        val salt = ByteArray(16) { (it * 3).toByte() }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2D, 0x13, 64, 2, 2)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2D, 0x13, 64, 2, 2)
        assertContentEquals(
            bcResult, pkResult,
            "Pure Kotlin Argon2d must match BouncyCastle output",
        )
    }

    @Test
    fun argon2id_pureKotlin_matchesBouncyCastle() {
        val password = "testpassword".encodeToByteArray()
        val salt = ByteArray(16) { (it * 3).toByte() }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 64, 2, 2)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2ID, 0x13, 64, 2, 2)
        assertContentEquals(
            bcResult, pkResult,
            "Pure Kotlin Argon2id must match BouncyCastle output",
        )
    }

    @Test
    fun argon2d_pureKotlin_matchesBouncyCastle_rfcParams() {
        // RFC 9106 parameters (without key/AD, which our interface doesn't support)
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2D, 0x13, 32, 3, 4)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2D, 0x13, 32, 3, 4)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun argon2id_pureKotlin_matchesBouncyCastle_rfcParams() {
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 3, 4)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 3, 4)
        assertContentEquals(bcResult, pkResult)
    }

    @Test
    fun argon2_pureKotlin_matchesBouncyCastle_singleLane() {
        val password = "singlelane".encodeToByteArray()
        val salt = ByteArray(16) { 0xFF.toByte() }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 2, 1)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 2, 1)
        assertContentEquals(bcResult, pkResult, "Single-lane Argon2id must match")
    }

    @Test
    fun argon2_pureKotlin_matchesBouncyCastle_higherMemory() {
        val password = "highmemory".encodeToByteArray()
        val salt = ByteArray(16) { (it * 7).toByte() }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 256, 1, 2)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2ID, 0x13, 256, 1, 2)
        assertContentEquals(bcResult, pkResult, "Higher memory Argon2id must match")
    }

    @Test
    fun argon2d_pureKotlin_matchesBouncyCastle_singleIteration() {
        val password = "quick".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val bcResult = crypto.argon2(password, salt, Argon2Variant.ARGON2D, 0x13, 32, 1, 1)
        val pkResult = Argon2.derive(password, salt, Argon2Variant.ARGON2D, 0x13, 32, 1, 1)
        assertContentEquals(bcResult, pkResult, "Single-iteration Argon2d must match")
    }

    // --- Behavioral tests ---

    @Test
    fun argon2_outputLength() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val result = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertEquals(32, result.size, "Output must be 32 bytes")
    }

    @Test
    fun argon2_deterministic() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val result1 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        val result2 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertContentEquals(result1, result2)
    }

    @Test
    fun argon2_passwordSensitivity() {
        val salt = ByteArray(16) { it.toByte() }
        val result1 = crypto.argon2("password1".encodeToByteArray(), salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        val result2 = crypto.argon2("password2".encodeToByteArray(), salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertFalse(result1.contentEquals(result2), "Different passwords must produce different output")
    }

    @Test
    fun argon2_saltSensitivity() {
        val password = "password".encodeToByteArray()
        val salt1 = ByteArray(16) { it.toByte() }
        val salt2 = ByteArray(16) { (it + 1).toByte() }
        val result1 = crypto.argon2(password, salt1, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        val result2 = crypto.argon2(password, salt2, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertFalse(result1.contentEquals(result2), "Different salts must produce different output")
    }

    @Test
    fun argon2_variantSensitivity() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val resultD = crypto.argon2(password, salt, Argon2Variant.ARGON2D, 0x13, 32, 1, 1)
        val resultID = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertFalse(resultD.contentEquals(resultID), "Argon2d and Argon2id must produce different output")
    }

    @Test
    fun argon2_iterationSensitivity() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val result1 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        val result2 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 2, 1)
        assertFalse(result1.contentEquals(result2), "Different iterations must produce different output")
    }

    @Test
    fun argon2_memorySensitivity() {
        val password = "password".encodeToByteArray()
        val salt = ByteArray(16) { it.toByte() }
        val result1 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        val result2 = crypto.argon2(password, salt, Argon2Variant.ARGON2ID, 0x13, 64, 1, 1)
        assertFalse(result1.contentEquals(result2), "Different memory costs must produce different output")
    }
}
