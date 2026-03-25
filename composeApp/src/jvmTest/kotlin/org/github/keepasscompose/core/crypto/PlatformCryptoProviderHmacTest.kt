package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for HMAC-SHA-256 using RFC 4231 test vectors.
 */
class PlatformCryptoProviderHmacTest {

    private val crypto = PlatformCryptoProvider()

    @Test
    fun hmacSha256_rfc4231_testCase1() {
        // Key = 0x0b repeated 20 times, Data = "Hi There"
        val key = ByteArray(20) { 0x0b }
        val data = "Hi There".encodeToByteArray()
        val expected = "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_rfc4231_testCase2() {
        // Key = "Jefe", Data = "what do ya want for nothing?"
        val key = "Jefe".encodeToByteArray()
        val data = "what do ya want for nothing?".encodeToByteArray()
        val expected = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_rfc4231_testCase3() {
        // Key = 0xaa repeated 20 times, Data = 0xdd repeated 50 times
        val key = ByteArray(20) { 0xaa.toByte() }
        val data = ByteArray(50) { 0xdd.toByte() }
        val expected = "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_rfc4231_testCase4() {
        // Key = 0x0102...1819 (25 bytes), Data = 0xcd repeated 50 times
        val key = ByteArray(25) { (it + 1).toByte() }
        val data = ByteArray(50) { 0xcd.toByte() }
        val expected = "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_rfc4231_testCase6_longKey() {
        // Key = 0xaa repeated 131 times (longer than block size)
        // Data = "Test Using Larger Than Block-Size Key - Hash Key First"
        val key = ByteArray(131) { 0xaa.toByte() }
        val data = "Test Using Larger Than Block-Size Key - Hash Key First".encodeToByteArray()
        val expected = "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_rfc4231_testCase7_longKeyAndData() {
        // Key = 0xaa repeated 131 times
        // Data = "This is a test using a larger than block-size key and a larger than block-size data..."
        val key = ByteArray(131) { 0xaa.toByte() }
        val data = ("This is a test using a larger than block-size key and a " +
            "larger than block-size data. The key needs to be hashed " +
            "before being used by the HMAC algorithm.").encodeToByteArray()
        val expected = "9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2"

        val result = crypto.hmacSha256(key, data)
        assertEquals(expected, result.toHex())
    }

    @Test
    fun hmacSha256_outputLength() {
        val result = crypto.hmacSha256(
            "key".encodeToByteArray(),
            "data".encodeToByteArray(),
        )
        assertEquals(32, result.size)
    }

    @Test
    fun hmacSha256_deterministic() {
        val key = "key".encodeToByteArray()
        val data = "data".encodeToByteArray()
        val result1 = crypto.hmacSha256(key, data)
        val result2 = crypto.hmacSha256(key, data)
        assertTrue(result1.contentEquals(result2))
    }

    @Test
    fun hmacSha256_differentKeysDifferentOutputs() {
        val data = "same data".encodeToByteArray()
        val mac1 = crypto.hmacSha256("key1".encodeToByteArray(), data)
        val mac2 = crypto.hmacSha256("key2".encodeToByteArray(), data)
        assertFalse(mac1.contentEquals(mac2))
    }

    @Test
    fun hmacSha256_differentDataDifferentOutputs() {
        val key = "same key".encodeToByteArray()
        val mac1 = crypto.hmacSha256(key, "data1".encodeToByteArray())
        val mac2 = crypto.hmacSha256(key, "data2".encodeToByteArray())
        assertFalse(mac1.contentEquals(mac2))
    }

    // -- Helper --

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
