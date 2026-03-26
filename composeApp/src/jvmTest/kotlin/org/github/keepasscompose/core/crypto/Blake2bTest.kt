package org.github.keepasscompose.core.crypto

import org.bouncycastle.crypto.digests.Blake2bDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Tests for the pure Kotlin Blake2b implementation.
 * Cross-validates against BouncyCastle's Blake2bDigest.
 */
class Blake2bTest {
    private fun bouncyCastleBlake2b(data: ByteArray, digestSize: Int): ByteArray {
        val digest = Blake2bDigest(digestSize * 8)
        digest.update(data, 0, data.size)
        val result = ByteArray(digestSize)
        digest.doFinal(result, 0)
        return result
    }

    @Test
    fun blake2b_emptyInput_matchesBouncyCastle() {
        val bc = bouncyCastleBlake2b(ByteArray(0), 64)
        val pk = Blake2b.hash(ByteArray(0), 64)
        assertContentEquals(bc, pk)
    }

    @Test
    fun blake2b_abc_matchesBouncyCastle() {
        val data = "abc".encodeToByteArray()
        val bc = bouncyCastleBlake2b(data, 64)
        val pk = Blake2b.hash(data, 64)
        assertContentEquals(bc, pk, "BLAKE2b-512(\"abc\") must match BouncyCastle")
    }

    @Test
    fun blake2b_shortInput_matchesBouncyCastle() {
        for (len in listOf(1, 3, 15, 16, 31, 32, 63, 64, 127)) {
            val data = ByteArray(len) { (it * 7 + 3).toByte() }
            val bc = bouncyCastleBlake2b(data, 64)
            val pk = Blake2b.hash(data, 64)
            assertContentEquals(bc, pk, "BLAKE2b-512 mismatch for input length $len")
        }
    }

    @Test
    fun blake2b_exactlyOneBlock_matchesBouncyCastle() {
        val data = ByteArray(128) { it.toByte() }
        val bc = bouncyCastleBlake2b(data, 64)
        val pk = Blake2b.hash(data, 64)
        assertContentEquals(bc, pk)
    }

    @Test
    fun blake2b_multiBlock_matchesBouncyCastle() {
        val data = ByteArray(200) { (it % 251).toByte() }
        val bc = bouncyCastleBlake2b(data, 64)
        val pk = Blake2b.hash(data, 64)
        assertContentEquals(bc, pk)
    }

    @Test
    fun blake2b_variousDigestSizes_matchBouncyCastle() {
        val data = "test data".encodeToByteArray()
        for (digestSize in listOf(1, 16, 20, 32, 48, 64)) {
            val bc = bouncyCastleBlake2b(data, digestSize)
            val pk = Blake2b.hash(data, digestSize)
            assertContentEquals(bc, pk, "Digest size $digestSize mismatch")
        }
    }

    @Test
    fun blake2b_outputLength() {
        for (len in listOf(1, 16, 32, 48, 64)) {
            val result = Blake2b.hash("test".encodeToByteArray(), len)
            assertEquals(len, result.size, "Output length should be $len")
        }
    }

    @Test
    fun blake2b_deterministic() {
        val data = "determinism".encodeToByteArray()
        val r1 = Blake2b.hash(data, 64)
        val r2 = Blake2b.hash(data, 64)
        assertContentEquals(r1, r2)
    }
}
