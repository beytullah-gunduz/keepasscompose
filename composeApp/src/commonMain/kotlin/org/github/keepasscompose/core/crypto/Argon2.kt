package org.github.keepasscompose.core.crypto

import org.github.keepasscompose.core.model.Argon2Variant

/**
 * Pure Kotlin implementation of Argon2 (RFC 9106).
 * Supports Argon2d (data-dependent) and Argon2id (hybrid) variants.
 *
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's Argon2BytesGenerator instead.
 *
 * Key concepts:
 * - Memory is organized as a matrix of 1024-byte blocks.
 * - The matrix has [parallelism] lanes, each with [segmentLength] * 4 blocks.
 * - Blocks are filled using a compression function G based on Blake2b.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object Argon2 {

    private const val BLOCK_SIZE = 1024 // bytes per block
    private const val QWORDS_PER_BLOCK = 128 // 1024 / 8
    private const val SYNC_POINTS = 4
    private const val ARGON2_PREHASH_DIGEST_LENGTH = 64
    private const val ARGON2_HASH_LENGTH = 32

    fun derive(
        password: ByteArray,
        salt: ByteArray,
        variant: Argon2Variant,
        version: Int,
        memoryKB: Long,
        iterations: Long,
        parallelism: Int,
    ): ByteArray {
        val type = when (variant) {
            Argon2Variant.ARGON2D -> 0
            Argon2Variant.ARGON2ID -> 2
        }
        val tagLength = ARGON2_HASH_LENGTH

        // Compute memory size: must be at least 8 * parallelism blocks
        val memoryBlocks = maxOf(memoryKB.toInt(), 8 * parallelism)
        // Round down to multiple of 4 * parallelism
        val segmentLength = memoryBlocks / (SYNC_POINTS * parallelism)
        val laneLength = segmentLength * SYNC_POINTS
        val totalBlocks = laneLength * parallelism

        // Allocate memory matrix as flat array of blocks
        // Each block is 128 ULongs (1024 bytes)
        val memory = Array(totalBlocks) { ULongArray(QWORDS_PER_BLOCK) }

        // H0 = H(p, salt, ...) — initial hash
        val h0 = initialHash(
            password, salt, type, version, tagLength,
            memoryKB.toInt(), iterations.toInt(), parallelism,
        )

        // Initialize first two blocks of each lane
        for (lane in 0 until parallelism) {
            // B[lane][0] = H'(H0 || 0 || lane)
            val input0 = ByteArray(ARGON2_PREHASH_DIGEST_LENGTH + 8)
            h0.copyInto(input0)
            intToLittleEndian(0, input0, ARGON2_PREHASH_DIGEST_LENGTH)
            intToLittleEndian(lane, input0, ARGON2_PREHASH_DIGEST_LENGTH + 4)
            val block0 = variableLengthHash(input0, BLOCK_SIZE)
            loadBlock(memory[lane * laneLength], block0)

            // B[lane][1] = H'(H0 || 1 || lane)
            val input1 = ByteArray(ARGON2_PREHASH_DIGEST_LENGTH + 8)
            h0.copyInto(input1)
            intToLittleEndian(1, input1, ARGON2_PREHASH_DIGEST_LENGTH)
            intToLittleEndian(lane, input1, ARGON2_PREHASH_DIGEST_LENGTH + 4)
            val block1 = variableLengthHash(input1, BLOCK_SIZE)
            loadBlock(memory[lane * laneLength + 1], block1)
        }

        // Fill memory (main loop)
        for (pass in 0 until iterations.toInt()) {
            for (slice in 0 until SYNC_POINTS) {
                for (lane in 0 until parallelism) {
                    fillSegment(
                        memory, pass, lane, slice,
                        type, version, iterations.toInt(), parallelism,
                        segmentLength, laneLength, totalBlocks,
                    )
                }
            }
        }

        // Finalize: XOR last blocks of all lanes
        val finalBlock = ULongArray(QWORDS_PER_BLOCK)
        memory[(parallelism - 1) * laneLength + laneLength - 1].copyInto(finalBlock)
        for (lane in 0 until parallelism - 1) {
            val lastBlock = memory[lane * laneLength + laneLength - 1]
            for (i in 0 until QWORDS_PER_BLOCK) {
                finalBlock[i] = finalBlock[i] xor lastBlock[i]
            }
        }

        val finalBytes = storeBlock(finalBlock)
        return variableLengthHash(finalBytes, tagLength)
    }

    /**
     * Compute the initial hash H0 per RFC 9106 Section 3.
     */
    private fun initialHash(
        password: ByteArray,
        salt: ByteArray,
        type: Int,
        version: Int,
        tagLength: Int,
        memoryKB: Int,
        iterations: Int,
        parallelism: Int,
    ): ByteArray {
        // H0 = Blake2b-512(LE32(p) || LE32(τ) || LE32(m) || LE32(t) ||
        //                   LE32(v) || LE32(y) || LE32(|P|) || P ||
        //                   LE32(|S|) || S || LE32(|K|) || K || LE32(|X|) || X)
        // where K = empty, X = empty for our use case
        val bufSize = 4 * 7 + // 7 LE32 fields before P
            password.size + 4 + salt.size + 4 + 4 // |P|, P, |S|, S, |K|=0, |X|=0
        val buf = ByteArray(bufSize + 4) // extra 4 for |X|
        var off = 0

        fun putInt(v: Int) { intToLittleEndian(v, buf, off); off += 4 }

        putInt(parallelism)       // p (lanes)
        putInt(tagLength)         // τ (tag length)
        putInt(memoryKB)          // m (memory in KB)
        putInt(iterations)        // t (iterations)
        putInt(version)           // v (version)
        putInt(type)              // y (type: 0=d, 2=id)
        putInt(password.size)     // |P|
        password.copyInto(buf, off); off += password.size
        putInt(salt.size)         // |S|
        salt.copyInto(buf, off); off += salt.size
        putInt(0)                 // |K| = 0 (no secret)
        putInt(0)                 // |X| = 0 (no associated data)

        return Blake2b.hash(buf.copyOf(off), ARGON2_PREHASH_DIGEST_LENGTH)
    }

    /**
     * Variable-length hash function H' per RFC 9106 Section 3.2.
     *
     * For T <= 64: return Blake2b(LE32(T) || X, T)
     * For T > 64:
     *   r = ceil(T/32) - 2
     *   V_1 = Blake2b-64(LE32(T) || X), take first 32 bytes
     *   V_i = Blake2b-64(V_{i-1}) for i = 2..r, take first 32 bytes each
     *   V_{r+1} = Blake2b(V_r, T - 32*r), take all bytes
     *   Output = V_1[0:32] || ... || V_r[0:32] || V_{r+1}
     */
    internal fun variableLengthHash(input: ByteArray, tagLength: Int): ByteArray {
        if (tagLength <= 64) {
            val lenPrefix = ByteArray(4)
            intToLittleEndian(tagLength, lenPrefix, 0)
            return Blake2b.hash(lenPrefix + input, tagLength)
        }

        val result = ByteArray(tagLength)
        val lenPrefix = ByteArray(4)
        intToLittleEndian(tagLength, lenPrefix, 0)

        val r = (tagLength + 31) / 32 - 2 // ceil(T/32) - 2

        // V_1 = Blake2b-64(LE32(T) || input)
        var vi = Blake2b.hash(lenPrefix + input, 64)
        vi.copyInto(result, 0, 0, 32)
        var pos = 32

        // V_2 through V_r: intermediate 64-byte digests, take first 32 bytes each
        for (i in 2..r) {
            vi = Blake2b.hash(vi, 64)
            vi.copyInto(result, pos, 0, 32)
            pos += 32
        }

        // V_{r+1}: final block with exactly the remaining bytes
        val lastSize = tagLength - 32 * r
        vi = Blake2b.hash(vi, lastSize)
        vi.copyInto(result, pos, 0, lastSize)

        return result
    }

    /**
     * Fill one segment of memory (one slice of one lane).
     */
    private fun fillSegment(
        memory: Array<ULongArray>,
        pass: Int,
        lane: Int,
        slice: Int,
        type: Int,
        version: Int,
        totalPasses: Int,
        parallelism: Int,
        segmentLength: Int,
        laneLength: Int,
        totalBlocks: Int,
    ) {
        val startIndex = if (pass == 0 && slice == 0) 2 else 0

        // For Argon2i/Argon2id: generate pseudo-random values for addressing
        // Argon2id uses data-independent addressing for first pass, first two slices
        val useDataIndependent = type == 2 && pass == 0 && slice < 2 // Argon2id
        var addressBlock = ULongArray(QWORDS_PER_BLOCK)
        val inputBlock = ULongArray(QWORDS_PER_BLOCK)
        val zeroBlock = ULongArray(QWORDS_PER_BLOCK)

        if (useDataIndependent) {
            inputBlock[0] = pass.toULong()
            inputBlock[1] = lane.toULong()
            inputBlock[2] = slice.toULong()
            inputBlock[3] = totalBlocks.toULong()
            inputBlock[4] = totalPasses.toULong()
            inputBlock[5] = type.toULong()
            // Generate first set of addresses (counter = 1)
            inputBlock[6] = 1uL
            addressBlock = compressG(zeroBlock, compressG(zeroBlock, inputBlock))
        }

        for (s in startIndex until segmentLength) {
            val currentIndex = slice * segmentLength + s
            val absoluteIndex = lane * laneLength + currentIndex

            // Determine reference indices (j1, j2)
            val j1: ULong
            val j2: ULong

            if (useDataIndependent) {
                // Regenerate address block when positions 0-127 are exhausted
                if (s != 0 && s % QWORDS_PER_BLOCK == 0) {
                    inputBlock[6]++
                    addressBlock = compressG(zeroBlock, compressG(zeroBlock, inputBlock))
                }
                // Use segment position as index (matching reference implementation)
                val addrIdx = s % QWORDS_PER_BLOCK
                j1 = addressBlock[addrIdx].toUInt().toULong()
                j2 = (addressBlock[addrIdx] shr 32).toUInt().toULong()
            } else {
                // Argon2d: data-dependent
                val prevIndex = if (currentIndex == 0) {
                    lane * laneLength + laneLength - 1
                } else {
                    absoluteIndex - 1
                }
                j1 = memory[prevIndex][0].toUInt().toULong()
                j2 = (memory[prevIndex][0] shr 32).toUInt().toULong()
            }

            // Compute reference lane and index
            val refLane = if (pass == 0 && slice == 0) {
                lane // Can only reference own lane in first slice of first pass
            } else {
                (j2 % parallelism.toULong()).toInt()
            }

            // Compute reference index within the reference lane
            val referenceAreaSize: Int
            if (pass == 0) {
                if (refLane == lane) {
                    referenceAreaSize = slice * segmentLength + s - 1
                } else {
                    referenceAreaSize = if (s == 0) {
                        slice * segmentLength - 1
                    } else {
                        slice * segmentLength
                    }
                }
            } else {
                if (refLane == lane) {
                    referenceAreaSize = laneLength - segmentLength + s - 1
                } else {
                    referenceAreaSize = if (s == 0) {
                        laneLength - segmentLength - 1
                    } else {
                        laneLength - segmentLength
                    }
                }
            }

            // Map j1 to an index in the reference area
            val x = j1 * j1 shr 32
            val y = (referenceAreaSize.toULong() * x) shr 32
            val relativePosition = (referenceAreaSize.toULong() - 1uL - y).toInt()

            val startPosition = if (pass == 0) {
                0
            } else {
                (slice + 1) * segmentLength
            }
            val refIndex = refLane * laneLength + ((startPosition + relativePosition) % laneLength)

            // Previous block
            val prevIndex = if (currentIndex == 0) {
                lane * laneLength + laneLength - 1
            } else {
                absoluteIndex - 1
            }

            // Compute new block: G(prev, ref) XOR current (if not first pass and version 0x13)
            val newBlock = compressG(memory[prevIndex], memory[refIndex])
            if (pass > 0 && version == 0x13) {
                for (i in 0 until QWORDS_PER_BLOCK) {
                    memory[absoluteIndex][i] = memory[absoluteIndex][i] xor newBlock[i]
                }
            } else {
                newBlock.copyInto(memory[absoluteIndex])
            }
        }
    }

    /**
     * Compression function G. Takes two 1024-byte blocks and produces a 1024-byte block.
     * G(X, Y) = X XOR Y XOR permute(X XOR Y)
     */
    private fun compressG(x: ULongArray, y: ULongArray): ULongArray {
        val r = ULongArray(QWORDS_PER_BLOCK)
        for (i in 0 until QWORDS_PER_BLOCK) {
            r[i] = x[i] xor y[i]
        }

        // Save R for final XOR
        val z = ULongArray(QWORDS_PER_BLOCK)
        r.copyInto(z)

        // Apply permutation P to rows and columns of 8x8 matrix of 16-byte values
        // The 1024-byte block is viewed as an 8×8 matrix of 16-byte (128-bit) values,
        // where each cell contains two ULong values (row-major order).

        // Apply P to each row (8 rows, each with 8 cells = 16 ULongs)
        for (row in 0 until 8) {
            val base = row * 16
            permutationP(
                r, base, base + 1, base + 2, base + 3,
                base + 4, base + 5, base + 6, base + 7,
                base + 8, base + 9, base + 10, base + 11,
                base + 12, base + 13, base + 14, base + 15,
            )
        }

        // Apply P to each column
        for (col in 0 until 8) {
            val i0 = col * 2
            permutationP(
                r, i0, i0 + 1, i0 + 16, i0 + 17,
                i0 + 32, i0 + 33, i0 + 48, i0 + 49,
                i0 + 64, i0 + 65, i0 + 80, i0 + 81,
                i0 + 96, i0 + 97, i0 + 112, i0 + 113,
            )
        }

        // XOR with saved copy
        for (i in 0 until QWORDS_PER_BLOCK) {
            r[i] = r[i] xor z[i]
        }

        return r
    }

    /**
     * Permutation P operating on 8 pairs of 64-bit words (v0..v15).
     * Applies Blake2b-style G mixing function in a specific pattern.
     */
    private fun permutationP(
        v: ULongArray,
        i0: Int, i1: Int, i2: Int, i3: Int,
        i4: Int, i5: Int, i6: Int, i7: Int,
        i8: Int, i9: Int, i10: Int, i11: Int,
        i12: Int, i13: Int, i14: Int, i15: Int,
    ) {
        gb(v, i0, i4, i8, i12)
        gb(v, i1, i5, i9, i13)
        gb(v, i2, i6, i10, i14)
        gb(v, i3, i7, i11, i15)
        gb(v, i0, i5, i10, i15)
        gb(v, i1, i6, i11, i12)
        gb(v, i2, i7, i8, i13)
        gb(v, i3, i4, i9, i14)
    }

    /**
     * Blake2b-based mixing function GB for Argon2.
     * Different from standard Blake2b G: uses multiplication for diffusion.
     */
    private fun gb(v: ULongArray, a: Int, b: Int, c: Int, d: Int) {
        v[a] = v[a] + v[b] + 2uL * (v[a].toUInt().toULong() * v[b].toUInt().toULong())
        v[d] = (v[d] xor v[a]).ror64(32)
        v[c] = v[c] + v[d] + 2uL * (v[c].toUInt().toULong() * v[d].toUInt().toULong())
        v[b] = (v[b] xor v[c]).ror64(24)
        v[a] = v[a] + v[b] + 2uL * (v[a].toUInt().toULong() * v[b].toUInt().toULong())
        v[d] = (v[d] xor v[a]).ror64(16)
        v[c] = v[c] + v[d] + 2uL * (v[c].toUInt().toULong() * v[d].toUInt().toULong())
        v[b] = (v[b] xor v[c]).ror64(63)
    }

    private fun ULong.ror64(n: Int): ULong = (this shr n) or (this shl (64 - n))

    // --- Helpers ---

    private fun loadBlock(block: ULongArray, bytes: ByteArray) {
        for (i in 0 until QWORDS_PER_BLOCK) {
            block[i] = littleEndianToULong(bytes, i * 8)
        }
    }

    private fun storeBlock(block: ULongArray): ByteArray {
        val bytes = ByteArray(BLOCK_SIZE)
        for (i in 0 until QWORDS_PER_BLOCK) {
            ulongToLittleEndian(block[i], bytes, i * 8)
        }
        return bytes
    }

    private fun intToLittleEndian(value: Int, bytes: ByteArray, offset: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
        bytes[offset + 2] = (value shr 16).toByte()
        bytes[offset + 3] = (value shr 24).toByte()
    }

    private fun littleEndianToULong(bytes: ByteArray, offset: Int): ULong {
        var value = 0uL
        for (i in 0 until 8) {
            value = value or (bytes[offset + i].toUByte().toULong() shl (i * 8))
        }
        return value
    }

    private fun ulongToLittleEndian(value: ULong, bytes: ByteArray, offset: Int) {
        for (i in 0 until 8) {
            bytes[offset + i] = (value shr (i * 8)).toByte()
        }
    }
}
