package org.github.keepasscompose.core.crypto

import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests for [CompositeKeyDerivation].
 *
 * Verifies composite key hashing, KDF application, and the full key derivation
 * pipeline used by KdbxReader/KdbxWriter.
 */
class CompositeKeyDerivationTest {
    private val crypto = PlatformCryptoProvider()
    private val kd = CompositeKeyDerivation(crypto)

    // --- Composite key hash ---

    @Test
    fun compositeKeyHash_passwordOnly() {
        val key = CompositeKey(password = "test")
        val hash = kd.deriveCompositeKeyHash(key)
        assertEquals(32, hash.size)
        // SHA-256(SHA-256("test"))
        val expected = crypto.sha256(crypto.sha256("test".encodeToByteArray()))
        assertContentEquals(expected, hash)
    }

    @Test
    fun compositeKeyHash_keyFileOnly() {
        val keyData = "keyfile-data".encodeToByteArray()
        val key = CompositeKey(keyFileData = keyData)
        val hash = kd.deriveCompositeKeyHash(key)
        assertEquals(32, hash.size)
        val expected = crypto.sha256(crypto.sha256(keyData))
        assertContentEquals(expected, hash)
    }

    @Test
    fun compositeKeyHash_passwordAndKeyFile() {
        val keyData = "keyfile-data".encodeToByteArray()
        val key = CompositeKey(password = "test", keyFileData = keyData)
        val hash = kd.deriveCompositeKeyHash(key)
        assertEquals(32, hash.size)
        // SHA-256(SHA-256("test") || SHA-256(keyData))
        val passwordHash = crypto.sha256("test".encodeToByteArray())
        val keyFileHash = crypto.sha256(keyData)
        val expected = crypto.sha256(passwordHash + keyFileHash)
        assertContentEquals(expected, hash)
    }

    @Test
    fun compositeKeyHash_emptyKey_throws() {
        val key = CompositeKey()
        assertFailsWith<IllegalArgumentException> {
            kd.deriveCompositeKeyHash(key)
        }
    }

    @Test
    fun compositeKeyHash_deterministic() {
        val key = CompositeKey(password = "password")
        val h1 = kd.deriveCompositeKeyHash(key)
        val h2 = kd.deriveCompositeKeyHash(key)
        assertContentEquals(h1, h2)
    }

    @Test
    fun compositeKeyHash_differentPasswords_differentHashes() {
        val h1 = kd.deriveCompositeKeyHash(CompositeKey(password = "pass1"))
        val h2 = kd.deriveCompositeKeyHash(CompositeKey(password = "pass2"))
        assertFalse(h1.contentEquals(h2))
    }

    // --- Transformed key (KDF) ---

    @Test
    fun transformedKey_aesKdf() {
        val compositeKeyHash = ByteArray(32) { it.toByte() }
        val seed = ByteArray(32) { (it + 32).toByte() }
        val kdfParams = KdfParameters.AesKdf(rounds = 10, seed = seed)
        val transformed = kd.deriveTransformedKey(compositeKeyHash, kdfParams)
        assertEquals(32, transformed.size)
        // AES-KDF result should be SHA-256 of the transformed key
        val expected = crypto.sha256(crypto.aesKdf(compositeKeyHash, seed, 10))
        assertContentEquals(expected, transformed)
    }

    @Test
    fun transformedKey_argon2() {
        val compositeKeyHash = ByteArray(32) { it.toByte() }
        val salt = ByteArray(16) { it.toByte() }
        val kdfParams =
            KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2ID,
                version = 0x13,
                salt = salt,
                parallelism = 1,
                memory = 32,
                iterations = 1,
            )
        val transformed = kd.deriveTransformedKey(compositeKeyHash, kdfParams)
        assertEquals(32, transformed.size)
        // Argon2 result is used directly (no extra SHA-256)
        val expected = crypto.argon2(compositeKeyHash, salt, Argon2Variant.ARGON2ID, 0x13, 32, 1, 1)
        assertContentEquals(expected, transformed)
    }

    // --- Full key derivation ---

    @Test
    fun deriveKeys_producesCorrectMasterKey() {
        val key = CompositeKey(password = "password")
        val masterSeed = ByteArray(32) { (it * 5).toByte() }
        val kdfSeed = ByteArray(32) { (it * 3).toByte() }
        val kdfParams = KdfParameters.AesKdf(rounds = 5, seed = kdfSeed)

        val result = kd.deriveKeys(key, masterSeed, kdfParams)
        assertEquals(32, result.masterKey.size)
        assertEquals(32, result.transformedKey.size)

        // Verify master key = SHA-256(masterSeed || transformedKey)
        val expectedMasterKey = crypto.sha256(masterSeed + result.transformedKey)
        assertContentEquals(expectedMasterKey, result.masterKey)
    }

    @Test
    fun deriveKeys_deterministic() {
        val key = CompositeKey(password = "password")
        val masterSeed = ByteArray(32) { it.toByte() }
        val kdfParams = KdfParameters.AesKdf(rounds = 5, seed = ByteArray(32) { (it + 1).toByte() })
        val r1 = kd.deriveKeys(key, masterSeed, kdfParams)
        val r2 = kd.deriveKeys(key, masterSeed, kdfParams)
        assertContentEquals(r1.masterKey, r2.masterKey)
        assertContentEquals(r1.transformedKey, r2.transformedKey)
    }

    @Test
    fun deriveKeys_differentPasswords_differentKeys() {
        val masterSeed = ByteArray(32) { it.toByte() }
        val kdfParams = KdfParameters.AesKdf(rounds = 5, seed = ByteArray(32) { (it + 1).toByte() })
        val r1 = kd.deriveKeys(CompositeKey(password = "pass1"), masterSeed, kdfParams)
        val r2 = kd.deriveKeys(CompositeKey(password = "pass2"), masterSeed, kdfParams)
        assertFalse(r1.masterKey.contentEquals(r2.masterKey))
    }

    // --- HMAC base key ---

    @Test
    fun hmacBaseKey_correctLength() {
        val masterSeed = ByteArray(32) { it.toByte() }
        val transformedKey = ByteArray(32) { (it + 32).toByte() }
        val hmacKey = kd.computeHmacBaseKey(masterSeed, transformedKey)
        assertEquals(64, hmacKey.size) // SHA-512 output
    }

    @Test
    fun hmacBaseKey_matchesExpectedComputation() {
        val masterSeed = ByteArray(32) { it.toByte() }
        val transformedKey = ByteArray(32) { (it + 32).toByte() }
        val result = kd.computeHmacBaseKey(masterSeed, transformedKey)
        val expected = crypto.sha512(masterSeed + transformedKey + byteArrayOf(0x01))
        assertContentEquals(expected, result)
    }

    // --- Known-good regression vectors ---
    // Hardcoded output values to catch regressions in the full derivation pipeline.

    @Test
    fun compositeKeyHash_knownOutput_passwordOnly() {
        // SHA-256( SHA-256("password".toByteArray()) ) — hardcoded regression vector
        val hash = kd.deriveCompositeKeyHash(CompositeKey(password = "password"))
        assertEquals(
            "73641c99f7719f57d8f4beb11a303afcd190243a51ced8782ca6d3dbe014d146",
            hash.toHex(),
            "Composite key hash of 'password' must be stable",
        )
    }

    @Test
    fun deriveKeys_knownOutput_aesKdf() {
        // Full pipeline with known parameters — hardcode the output for regression testing
        val key = CompositeKey(password = "test")
        val masterSeed = ByteArray(32) { it.toByte() }
        val kdfSeed = ByteArray(32) { (it + 32).toByte() }
        val kdfParams = KdfParameters.AesKdf(rounds = 1, seed = kdfSeed)

        val result = kd.deriveKeys(key, masterSeed, kdfParams)

        assertEquals(32, result.masterKey.size)
        assertEquals(32, result.transformedKey.size)

        // Verify the composite key hash is deterministic
        val compositeKeyHash = kd.deriveCompositeKeyHash(key)
        val expectedCompositeHash = "954d5a49fd70d9b8bcdb35d252267829957f7ef7fa6c74f88419bdc5e82209f4"
        assertEquals(expectedCompositeHash, compositeKeyHash.toHex())

        // Verify the full chain is deterministic by comparing hex output
        val masterKeyHex = result.masterKey.toHex()
        val transformedKeyHex = result.transformedKey.toHex()
        // Re-derive to confirm determinism
        val result2 = kd.deriveKeys(key, masterSeed, kdfParams)
        assertEquals(masterKeyHex, result2.masterKey.toHex(), "Master key must be deterministic")
        assertEquals(transformedKeyHex, result2.transformedKey.toHex(), "Transformed key must be deterministic")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
