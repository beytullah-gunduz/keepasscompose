package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for AES-256-CBC encryption/decryption with PKCS7 padding.
 *
 * Uses NIST SP 800-38A test vectors where applicable, plus round-trip tests.
 */
class PlatformCryptoProviderAesTest {

    private val crypto = PlatformCryptoProvider()

    // 256-bit key (32 bytes)
    private val testKey = ByteArray(32) { it.toByte() }
    // 128-bit IV (16 bytes)
    private val testIv = ByteArray(16) { (it + 0x10).toByte() }

    @Test
    fun aes_roundTrip_singleBlock() {
        // 16 bytes = exactly one AES block (padding adds a full block)
        val plaintext = "0123456789abcdef".encodeToByteArray()

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_roundTrip_multiBlock() {
        // 48 bytes = 3 full AES blocks
        val plaintext = ByteArray(48) { (it * 3).toByte() }

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_roundTrip_nonBlockAligned() {
        // 25 bytes — not a multiple of 16, requires PKCS7 padding
        val plaintext = "This is 25 bytes of text!".encodeToByteArray()

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_roundTrip_emptyPlaintext() {
        val plaintext = ByteArray(0)

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_roundTrip_singleByte() {
        val plaintext = byteArrayOf(0x42)

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_roundTrip_largeData() {
        // 1000 bytes of random-ish data
        val plaintext = ByteArray(1000) { (it % 256).toByte() }

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val decrypted = crypto.aesDecrypt(encrypted, testKey, testIv)

        assertTrue(plaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_encryptProducesDifferentOutput() {
        val plaintext = "Hello AES!".encodeToByteArray()

        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)

        assertFalse(plaintext.contentEquals(encrypted))
    }

    @Test
    fun aes_encryptOutputIsPaddedToBlockSize() {
        // 10 bytes input → should produce 16 bytes output (one padded block)
        val plaintext = ByteArray(10)
        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        assertEquals(0, encrypted.size % 16)
        assertEquals(16, encrypted.size)
    }

    @Test
    fun aes_encryptBlockAlignedAddsPaddingBlock() {
        // 16 bytes input → PKCS7 adds a full padding block → 32 bytes output
        val plaintext = ByteArray(16)
        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        assertEquals(32, encrypted.size)
    }

    @Test
    fun aes_differentKeysProduceDifferentCiphertext() {
        val plaintext = "same plaintext".encodeToByteArray()
        val key1 = ByteArray(32) { 0x01 }
        val key2 = ByteArray(32) { 0x02 }

        val enc1 = crypto.aesEncrypt(plaintext, key1, testIv)
        val enc2 = crypto.aesEncrypt(plaintext, key2, testIv)

        assertFalse(enc1.contentEquals(enc2))
    }

    @Test
    fun aes_differentIvsProduceDifferentCiphertext() {
        val plaintext = "same plaintext".encodeToByteArray()
        val iv1 = ByteArray(16) { 0x01 }
        val iv2 = ByteArray(16) { 0x02 }

        val enc1 = crypto.aesEncrypt(plaintext, testKey, iv1)
        val enc2 = crypto.aesEncrypt(plaintext, testKey, iv2)

        assertFalse(enc1.contentEquals(enc2))
    }

    @Test
    fun aes_deterministic() {
        val plaintext = "deterministic".encodeToByteArray()

        val enc1 = crypto.aesEncrypt(plaintext, testKey, testIv)
        val enc2 = crypto.aesEncrypt(plaintext, testKey, testIv)

        assertTrue(enc1.contentEquals(enc2))
    }

    @Test
    fun aes_decryptWithWrongKeyFails() {
        val plaintext = "secret data".encodeToByteArray()
        val encrypted = crypto.aesEncrypt(plaintext, testKey, testIv)
        val wrongKey = ByteArray(32) { 0xFF.toByte() }

        // Decryption with wrong key should either throw or produce garbage
        try {
            val decrypted = crypto.aesDecrypt(encrypted, wrongKey, testIv)
            // If no exception, the result must differ from plaintext
            assertFalse(plaintext.contentEquals(decrypted))
        } catch (_: Exception) {
            // Expected: padding validation fails with wrong key
        }
    }

    @Test
    fun aes_knownVector() {
        // Known AES-256-CBC test: encrypt known plaintext and verify ciphertext is stable
        val key = hexToBytes("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")
        val iv = hexToBytes("000102030405060708090a0b0c0d0e0f")
        val plaintext = hexToBytes("6bc1bee22e409f96e93d7e117393172a")

        val encrypted = crypto.aesEncrypt(plaintext, key, iv)
        val decrypted = crypto.aesDecrypt(encrypted, key, iv)

        assertTrue(plaintext.contentEquals(decrypted))
        // Ciphertext should start with the known NIST block
        // f58c4c04d6e5f1ba779eabfb5f7bfbd6 (first block of AES-256-CBC with this key/iv/plaintext)
        assertEquals("f58c4c04d6e5f1ba779eabfb5f7bfbd6", encrypted.copyOf(16).toHex())
    }

    // -- NIST SP 800-38A Section F.2.5/F.2.6: AES-256-CBC test vectors --
    // Full 4-block encryption test with official NIST vectors.

    // NIST AES-256-CBC key
    private val nistKey = hexToBytes("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4")
    private val nistIv = hexToBytes("000102030405060708090a0b0c0d0e0f")
    // 4 plaintext blocks from NIST SP 800-38A
    private val nistPlaintext = hexToBytes(
        "6bc1bee22e409f96e93d7e117393172a" +
        "ae2d8a571e03ac9c9eb76fac45af8e51" +
        "30c81c46a35ce411e5fbc1191a0a52ef" +
        "f69f2445df4f9b17ad2b417be66c3710"
    )
    // Expected ciphertext: NIST SP 800-38A Section F.2.5 (AES-256-CBC Encrypt)
    private val nistCiphertext = hexToBytes(
        "f58c4c04d6e5f1ba779eabfb5f7bfbd6" +
        "9cfc4e967edb808d679f777bc6702c7d" +
        "39f23369a9d9bacfa530e26304231461" +
        "b2eb05e2c39be9fcda6c19078c6a9d1b"
    )

    @Test
    fun aes_nistVector_encrypt_4blocks() {
        // Our implementation uses PKCS7 padding, so ciphertext will have an extra padding block.
        // Verify the first 64 bytes match the NIST expected ciphertext.
        val encrypted = crypto.aesEncrypt(nistPlaintext, nistKey, nistIv)
        assertEquals(
            nistCiphertext.toHex(),
            encrypted.copyOf(64).toHex(),
            "First 4 blocks must match NIST SP 800-38A AES-256-CBC test vector",
        )
    }

    @Test
    fun aes_nistVector_roundTrip_4blocks() {
        val encrypted = crypto.aesEncrypt(nistPlaintext, nistKey, nistIv)
        val decrypted = crypto.aesDecrypt(encrypted, nistKey, nistIv)
        assertTrue(nistPlaintext.contentEquals(decrypted))
    }

    @Test
    fun aes_nistVector_block1() {
        // Verify first block individually
        val plaintext = hexToBytes("6bc1bee22e409f96e93d7e117393172a")
        val encrypted = crypto.aesEncrypt(plaintext, nistKey, nistIv)
        assertEquals("f58c4c04d6e5f1ba779eabfb5f7bfbd6", encrypted.copyOf(16).toHex())
    }

    @Test
    fun aes_nistVector_block2() {
        // Two blocks: verify second ciphertext block
        val plaintext = hexToBytes("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e51")
        val encrypted = crypto.aesEncrypt(plaintext, nistKey, nistIv)
        assertEquals("9cfc4e967edb808d679f777bc6702c7d", encrypted.copyOfRange(16, 32).toHex())
    }

    // -- Helpers --

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
