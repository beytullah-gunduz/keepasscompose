package org.github.keepasscompose.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * NIST and RFC test vectors for cryptographic hash functions.
 * These validate that the platform crypto provider produces correct results
 * against well-known reference outputs.
 */
class NistTestVectorTest {

    private val crypto = PlatformCryptoProvider()

    // NIST SHA-256 test vectors (FIPS 180-4)
    @Test
    fun sha256EmptyString() {
        val hash = crypto.sha256(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash.toHexString(),
        )
    }

    @Test
    fun sha256Abc() {
        val hash = crypto.sha256("abc".encodeToByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash.toHexString(),
        )
    }

    @Test
    fun sha256TwoBlockMessage() {
        val input = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        val hash = crypto.sha256(input.encodeToByteArray())
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hash.toHexString(),
        )
    }

    // NIST SHA-512 test vectors (FIPS 180-4)
    @Test
    fun sha512EmptyString() {
        val hash = crypto.sha512(ByteArray(0))
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            hash.toHexString(),
        )
    }

    @Test
    fun sha512Abc() {
        val hash = crypto.sha512("abc".encodeToByteArray())
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hash.toHexString(),
        )
    }

    // HMAC-SHA256 test vectors (RFC 4231)
    @Test
    fun hmacSha256TestVector1() {
        val key = ByteArray(20) { 0x0b }
        val data = "Hi There".encodeToByteArray()
        val mac = crypto.hmacSha256(key, data)
        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            mac.toHexString(),
        )
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
