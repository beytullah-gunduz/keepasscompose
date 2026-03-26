package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SHA-256 and SHA-512 implementations using known test vectors.
 *
 * Test vectors sourced from NIST FIPS 180-4 and RFC 6234.
 */
class PlatformCryptoProviderShaTest {

    private val crypto = PlatformCryptoProvider()

    // -- SHA-256 tests --

    @Test
    fun sha256_emptyInput() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val result = crypto.sha256(ByteArray(0))
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha256_abc() {
        // NIST FIPS 180-4 test vector: SHA-256("abc")
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val result = crypto.sha256("abc".encodeToByteArray())
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha256_twoBlockMessage() {
        // NIST test vector: SHA-256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
        val input = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        val expected = "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1"
        val result = crypto.sha256(input.encodeToByteArray())
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha256_outputLength() {
        val result = crypto.sha256("test".encodeToByteArray())
        assertEquals(32, result.size)
    }

    @Test
    fun sha256_deterministic() {
        val data = "deterministic test".encodeToByteArray()
        val result1 = crypto.sha256(data)
        val result2 = crypto.sha256(data)
        assertTrue(result1.contentEquals(result2))
    }

    @Test
    fun sha256_differentInputsDifferentOutputs() {
        val hash1 = crypto.sha256("input1".encodeToByteArray())
        val hash2 = crypto.sha256("input2".encodeToByteArray())
        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun sha256_singleByte() {
        // SHA-256 of single zero byte
        val expected = "6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d"
        val result = crypto.sha256(byteArrayOf(0))
        assertEquals(expected, result.toHex())
    }

    // -- SHA-512 tests --

    @Test
    fun sha512_emptyInput() {
        // SHA-512("")
        val expected = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
            "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"
        val result = crypto.sha512(ByteArray(0))
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha512_abc() {
        // NIST FIPS 180-4 test vector: SHA-512("abc")
        val expected = "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
            "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
        val result = crypto.sha512("abc".encodeToByteArray())
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha512_twoBlockMessage() {
        // NIST test vector: SHA-512("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu")
        val input = "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"
        val expected = "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018" +
            "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909"
        val result = crypto.sha512(input.encodeToByteArray())
        assertEquals(expected, result.toHex())
    }

    @Test
    fun sha512_outputLength() {
        val result = crypto.sha512("test".encodeToByteArray())
        assertEquals(64, result.size)
    }

    @Test
    fun sha512_deterministic() {
        val data = "deterministic test".encodeToByteArray()
        val result1 = crypto.sha512(data)
        val result2 = crypto.sha512(data)
        assertTrue(result1.contentEquals(result2))
    }

    @Test
    fun sha512_differentInputsDifferentOutputs() {
        val hash1 = crypto.sha512("input1".encodeToByteArray())
        val hash2 = crypto.sha512("input2".encodeToByteArray())
        assertFalse(hash1.contentEquals(hash2))
    }

    // -- Helpers --

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun assertFalse(condition: Boolean) {
        kotlin.test.assertFalse(condition)
    }
}
