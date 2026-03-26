package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of BLAKE2b (RFC 7693).
 * Supports variable output length (1–64 bytes) and optional key (0–64 bytes).
 *
 * Used by [Argon2] for hashing within the key derivation algorithm.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object Blake2b {
    private const val BLOCK_SIZE = 128 // bytes
    private const val MAX_DIGEST_SIZE = 64 // bytes

    // Initialization vector (same as SHA-512)
    private val IV =
        ulongArrayOf(
            0x6A09E667F3BCC908uL,
            0xBB67AE8584CAA73BuL,
            0x3C6EF372FE94F82BuL,
            0xA54FF53A5F1D36F1uL,
            0x510E527FADE682D1uL,
            0x9B05688C2B3E6C1FuL,
            0x1F83D9ABFB41BD6BuL,
            0x5BE0CD19137E2179uL,
        )

    // Message schedule permutation (sigma)
    private val SIGMA =
        arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            intArrayOf(14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3),
            intArrayOf(11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4),
            intArrayOf(7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8),
            intArrayOf(9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13),
            intArrayOf(2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9),
            intArrayOf(12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11),
            intArrayOf(13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10),
            intArrayOf(6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5),
            intArrayOf(10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0),
        )

    /**
     * Compute BLAKE2b hash of [data] with the specified [digestSize] (1–64).
     */
    fun hash(data: ByteArray, digestSize: Int): ByteArray {
        require(digestSize in 1..MAX_DIGEST_SIZE) { "digestSize must be 1..64" }
        return hash(data, digestSize, key = null)
    }

    /**
     * Compute keyed BLAKE2b hash of [data] with [key] and [digestSize].
     */
    fun hash(data: ByteArray, digestSize: Int, key: ByteArray?): ByteArray {
        require(digestSize in 1..MAX_DIGEST_SIZE) { "digestSize must be 1..64" }
        val keyLen = key?.size ?: 0
        require(keyLen <= MAX_DIGEST_SIZE) { "key must be 0..64 bytes" }

        // Initialize state
        val h = ULongArray(8)
        IV.copyInto(h)
        // Parameter block: fanout=1, depth=1, all others 0
        h[0] = h[0] xor (digestSize.toULong() or (keyLen.toULong() shl 8) or (0x01uL shl 16) or (0x01uL shl 24))

        var bytesCompressed = 0uL
        val buffer = ByteArray(BLOCK_SIZE)
        var bufferLen = 0

        // If keyed, the first block is the padded key
        if (key != null && keyLen > 0) {
            key.copyInto(buffer)
            // rest is already zero-filled
            bufferLen = BLOCK_SIZE
        }

        // Process data
        var offset = 0
        val remaining = data.size

        while (offset < remaining) {
            // If buffer is full, compress it (it's not the last block because we still have data)
            if (bufferLen == BLOCK_SIZE) {
                bytesCompressed += BLOCK_SIZE.toULong()
                compress(h, buffer, bytesCompressed, lastBlock = false)
                bufferLen = 0
            }
            val toCopy = minOf(BLOCK_SIZE - bufferLen, remaining - offset)
            data.copyInto(buffer, bufferLen, offset, offset + toCopy)
            bufferLen += toCopy
            offset += toCopy
        }

        // Final block
        bytesCompressed += bufferLen.toULong()
        // Pad with zeros
        for (i in bufferLen until BLOCK_SIZE) {
            buffer[i] = 0
        }
        compress(h, buffer, bytesCompressed, lastBlock = true)

        // Produce output
        val output = ByteArray(digestSize)
        for (i in 0 until digestSize) {
            output[i] = (h[i / 8] shr (8 * (i % 8))).toByte()
        }
        return output
    }

    private fun compress(h: ULongArray, block: ByteArray, t: ULong, lastBlock: Boolean) {
        // Initialize working vector
        val v = ULongArray(16)
        h.copyInto(v, 0, 0, 8)
        IV.copyInto(v, 8, 0, 8)

        v[12] = v[12] xor t // low word of counter
        // v[13] = v[13] xor 0 (high word of counter, not needed for < 2^64 bytes)
        if (lastBlock) {
            v[14] = v[14].inv()
        }

        // Parse block into 16 message words (little-endian)
        val m = ULongArray(16)
        for (i in 0 until 16) {
            m[i] = littleEndianToULong(block, i * 8)
        }

        // 12 rounds of mixing
        for (round in 0 until 12) {
            val s = SIGMA[round % 10]
            mix(v, 0, 4, 8, 12, m[s[0]], m[s[1]])
            mix(v, 1, 5, 9, 13, m[s[2]], m[s[3]])
            mix(v, 2, 6, 10, 14, m[s[4]], m[s[5]])
            mix(v, 3, 7, 11, 15, m[s[6]], m[s[7]])
            mix(v, 0, 5, 10, 15, m[s[8]], m[s[9]])
            mix(v, 1, 6, 11, 12, m[s[10]], m[s[11]])
            mix(v, 2, 7, 8, 13, m[s[12]], m[s[13]])
            mix(v, 3, 4, 9, 14, m[s[14]], m[s[15]])
        }

        // Finalize
        for (i in 0 until 8) {
            h[i] = h[i] xor v[i] xor v[i + 8]
        }
    }

    private fun mix(v: ULongArray, a: Int, b: Int, c: Int, d: Int, x: ULong, y: ULong) {
        v[a] += v[b] + x
        v[d] = (v[d] xor v[a]).rotateRight(32)
        v[c] += v[d]
        v[b] = (v[b] xor v[c]).rotateRight(24)
        v[a] += v[b] + y
        v[d] = (v[d] xor v[a]).rotateRight(16)
        v[c] += v[d]
        v[b] = (v[b] xor v[c]).rotateRight(63)
    }

    private fun littleEndianToULong(bytes: ByteArray, offset: Int): ULong {
        var value = 0uL
        for (i in 0 until 8) {
            value = value or (bytes[offset + i].toUByte().toULong() shl (i * 8))
        }
        return value
    }

    private fun ULong.rotateRight(n: Int): ULong = (this shr n) or (this shl (64 - n))
}
