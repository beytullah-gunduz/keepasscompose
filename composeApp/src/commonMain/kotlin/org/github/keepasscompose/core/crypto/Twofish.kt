package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of Twofish block cipher (256-bit key)
 * with CBC mode and PKCS7 padding.
 *
 * Follows the Twofish specification by Schneier, Kelsey, Whiting, Wagner, Hall, Ferguson.
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's TwofishEngine instead.
 */
internal object Twofish {
    const val BLOCK_SIZE = 16
    private const val ROUNDS = 16
    private const val TOTAL_SUBKEYS = 40

    // ---- q permutation tables (Section 4.3.2) ----

    // q0 4-bit S-boxes from specification Table 2
    private val Q0_T =
        arrayOf(
            intArrayOf(0x8, 0x1, 0x7, 0xD, 0x6, 0xF, 0x3, 0x2, 0x0, 0xB, 0x5, 0x9, 0xE, 0xC, 0xA, 0x4),
            intArrayOf(0xE, 0xC, 0xB, 0x8, 0x1, 0x2, 0x3, 0x5, 0xF, 0x4, 0xA, 0x6, 0x7, 0x0, 0x9, 0xD),
            intArrayOf(0xB, 0xA, 0x5, 0xE, 0x6, 0xD, 0x9, 0x0, 0xC, 0x8, 0xF, 0x3, 0x2, 0x4, 0x7, 0x1),
            intArrayOf(0xD, 0x7, 0xF, 0x4, 0x1, 0x2, 0x6, 0xE, 0x9, 0xB, 0x3, 0x0, 0x8, 0x5, 0xC, 0xA),
        )

    // q1 4-bit S-boxes
    private val Q1_T =
        arrayOf(
            intArrayOf(0x2, 0x8, 0xB, 0xD, 0xF, 0x7, 0x6, 0xE, 0x3, 0x1, 0x9, 0x4, 0x0, 0xA, 0xC, 0x5),
            intArrayOf(0x1, 0xE, 0x2, 0xB, 0x4, 0xC, 0x3, 0x7, 0x6, 0xD, 0xA, 0x5, 0xF, 0x9, 0x0, 0x8),
            intArrayOf(0x4, 0xC, 0x7, 0x5, 0x1, 0x6, 0x9, 0xA, 0x0, 0xE, 0xD, 0x8, 0x2, 0xB, 0x3, 0xF),
            intArrayOf(0xB, 0x9, 0x5, 0x1, 0xC, 0x3, 0xD, 0xE, 0x6, 0x4, 0x7, 0xF, 0x2, 0x0, 0x8, 0xA),
        )

    /** Compute q permutation from 4-bit S-boxes (Section 4.3.2 algorithm) */
    private fun computeQ(x: Int, t: Array<IntArray>): Int {
        val a0 = x shr 4
        val b0 = x and 0xF
        val a1 = a0 xor b0
        val b1 = a0 xor ror4(b0) xor ((a0 shl 3) and 0xF)
        val a2 = t[0][a1]
        val b2 = t[1][b1]
        val a3 = a2 xor b2
        val b3 = a2 xor ror4(b2) xor ((a2 shl 3) and 0xF)
        return (t[3][b3] shl 4) or t[2][a3]
    }

    private fun ror4(x: Int): Int = ((x shr 1) or ((x and 1) shl 3)) and 0xF

    // Precomputed 8-bit q permutations
    private val Q0 = IntArray(256) { computeQ(it, Q0_T) }
    private val Q1 = IntArray(256) { computeQ(it, Q1_T) }

    // ---- MDS matrix (Section 4.3.1) ----

    // Primitive polynomial: x^8 + x^6 + x^5 + x^3 + 1
    private const val MDS_POLY = 0x169

    private val MDS_MATRIX =
        arrayOf(
            intArrayOf(0x01, 0xEF, 0x5B, 0x5B),
            intArrayOf(0x5B, 0xEF, 0xEF, 0x01),
            intArrayOf(0xEF, 0x5B, 0x01, 0xEF),
            intArrayOf(0xEF, 0x01, 0xEF, 0x5B),
        )

    private fun gfMult(a: Int, b: Int, poly: Int): Int {
        var result = 0
        var aa = a
        var bb = b
        while (bb != 0) {
            if (bb and 1 != 0) result = result xor aa
            aa = aa shl 1
            if (aa and 0x100 != 0) aa = aa xor poly
            bb = bb shr 1
        }
        return result and 0xFF
    }

    // P_x0 constants: which Q permutation to bake into each gMDS column
    // (matching BouncyCastle's gMDS0..gMDS3 structure)
    private val P_X0 = intArrayOf(1, 0, 1, 0) // Q1, Q0, Q1, Q0

    // gMDS[col][i] = Q_perm(i) → packed 32-bit MDS column product
    // Includes the Q permutation (like BouncyCastle), giving the correct
    // total of 5 Q layers for 256-bit keys (4 explicit + 1 here).
    private val gMDS =
        Array(4) { col ->
            val qPerm = if (P_X0[col] == 1) Q1 else Q0
            IntArray(256) { i ->
                val b = qPerm[i]
                gfMult(MDS_MATRIX[0][col], b, MDS_POLY) or
                    (gfMult(MDS_MATRIX[1][col], b, MDS_POLY) shl 8) or
                    (gfMult(MDS_MATRIX[2][col], b, MDS_POLY) shl 16) or
                    (gfMult(MDS_MATRIX[3][col], b, MDS_POLY) shl 24)
            }
        }

    // ---- RS matrix for key schedule (Section 4.3.4) ----

    // Primitive polynomial: x^8 + x^6 + x^3 + x^2 + 1
    private const val RS_POLY = 0x14D

    private fun rsRem(x: Int): Int {
        val b = (x ushr 24) and 0xFF
        val g2 = ((b shl 1) xor (if (b and 0x80 != 0) RS_POLY else 0)) and 0xFF
        val g3 = ((b ushr 1) xor (if (b and 0x01 != 0) RS_POLY ushr 1 else 0)) xor g2
        return (x shl 8) xor (g3 shl 24) xor (g2 shl 16) xor (g3 shl 8) xor b
    }

    private fun rsMdsEncode(k0: Int, k1: Int): Int {
        var r = k1
        repeat(4) { r = rsRem(r) }
        r = r xor k0
        repeat(4) { r = rsRem(r) }
        return r
    }

    // ---- Byte extraction ----

    private fun b0(x: Int) = x and 0xFF

    private fun b1(x: Int) = (x ushr 8) and 0xFF

    private fun b2(x: Int) = (x ushr 16) and 0xFF

    private fun b3(x: Int) = (x ushr 24) and 0xFF

    // ---- h function for key schedule (Section 4.3.5) ----

    private fun h256(x: Int, L: IntArray): Int {
        var x0 = Q1[b0(x)] xor b0(L[3])
        var x1 = Q0[b1(x)] xor b1(L[3])
        var x2 = Q0[b2(x)] xor b2(L[3])
        var x3 = Q1[b3(x)] xor b3(L[3])

        x0 = Q1[x0] xor b0(L[2])
        x1 = Q1[x1] xor b1(L[2])
        x2 = Q0[x2] xor b2(L[2])
        x3 = Q0[x3] xor b3(L[2])

        return gMDS[0][Q0[Q0[x0] xor b0(L[1])] xor b0(L[0])] xor
            gMDS[1][Q0[Q1[x1] xor b1(L[1])] xor b1(L[0])] xor
            gMDS[2][Q1[Q0[x2] xor b2(L[1])] xor b2(L[0])] xor
            gMDS[3][Q1[Q1[x3] xor b3(L[1])] xor b3(L[0])]
    }

    // ---- Key expansion (Section 4.3.1 / 4.3.5) ----

    class ExpandedKey(val subKeys: IntArray, val sBoxes: Array<IntArray>)

    fun expandKey(key: ByteArray): ExpandedKey {
        require(key.size == 32) { "Twofish key must be 32 bytes, got ${key.size}" }

        val k32 = IntArray(8) { i -> leToInt(key, i * 4) }
        val me = intArrayOf(k32[0], k32[2], k32[4], k32[6])
        val mo = intArrayOf(k32[1], k32[3], k32[5], k32[7])

        // S-box keys via RS matrix (reversed index order)
        val sBoxKeys = IntArray(4)
        for (i in 0 until 4) {
            sBoxKeys[3 - i] = rsMdsEncode(me[i], mo[i])
        }

        // Round + whitening subkeys
        val subKeys = IntArray(TOTAL_SUBKEYS)
        for (i in 0 until TOTAL_SUBKEYS / 2) {
            val a = h256(2 * i * 0x01010101, me)
            val b = h256((2 * i + 1) * 0x01010101, mo).rotateLeft(8)
            subKeys[2 * i] = a + b
            subKeys[2 * i + 1] = (a + b + b).rotateLeft(9)
        }

        return ExpandedKey(subKeys, computeSBoxes(sBoxKeys))
    }

    private fun computeSBoxes(sk: IntArray): Array<IntArray> = Array(4) { pos ->
        IntArray(256) { b ->
            var x = b
            when (pos) {
                0 -> {
                    x = Q1[x] xor b0(sk[3])
                    x = Q1[x] xor b0(sk[2])
                    x = Q0[x] xor b0(sk[1])
                    x =
                        Q0[x] xor b0(sk[0])
                }

                1 -> {
                    x = Q0[x] xor b1(sk[3])
                    x = Q1[x] xor b1(sk[2])
                    x = Q1[x] xor b1(sk[1])
                    x =
                        Q0[x] xor b1(sk[0])
                }

                2 -> {
                    x = Q0[x] xor b2(sk[3])
                    x = Q0[x] xor b2(sk[2])
                    x = Q0[x] xor b2(sk[1])
                    x =
                        Q1[x] xor b2(sk[0])
                }

                3 -> {
                    x = Q1[x] xor b3(sk[3])
                    x = Q0[x] xor b3(sk[2])
                    x = Q1[x] xor b3(sk[1])
                    x =
                        Q1[x] xor b3(sk[0])
                }
            }
            gMDS[pos][x]
        }
    }

    // ---- Round function ----

    private fun g(x: Int, s: Array<IntArray>): Int = s[0][b0(x)] xor s[1][b1(x)] xor s[2][b2(x)] xor s[3][b3(x)]

    // ---- Block encrypt / decrypt ----

    fun encryptBlock(ek: ExpandedKey, src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        var x0 = leToInt(src, srcOff) xor ek.subKeys[0]
        var x1 = leToInt(src, srcOff + 4) xor ek.subKeys[1]
        var x2 = leToInt(src, srcOff + 8) xor ek.subKeys[2]
        var x3 = leToInt(src, srcOff + 12) xor ek.subKeys[3]

        for (r in 0 until ROUNDS step 2) {
            var t0 = g(x0, ek.sBoxes)
            var t1 = g(x1.rotateLeft(8), ek.sBoxes)
            x2 = (x2 xor (t0 + t1 + ek.subKeys[8 + 2 * r])).rotateRight(1)
            x3 = x3.rotateLeft(1) xor (t0 + t1 + t1 + ek.subKeys[9 + 2 * r])

            t0 = g(x2, ek.sBoxes)
            t1 = g(x3.rotateLeft(8), ek.sBoxes)
            x0 = (x0 xor (t0 + t1 + ek.subKeys[10 + 2 * r])).rotateRight(1)
            x1 = x1.rotateLeft(1) xor (t0 + t1 + t1 + ek.subKeys[11 + 2 * r])
        }

        intToLe(x2 xor ek.subKeys[4], dst, dstOff)
        intToLe(x3 xor ek.subKeys[5], dst, dstOff + 4)
        intToLe(x0 xor ek.subKeys[6], dst, dstOff + 8)
        intToLe(x1 xor ek.subKeys[7], dst, dstOff + 12)
    }

    fun decryptBlock(ek: ExpandedKey, src: ByteArray, srcOff: Int, dst: ByteArray, dstOff: Int) {
        var x2 = leToInt(src, srcOff) xor ek.subKeys[4]
        var x3 = leToInt(src, srcOff + 4) xor ek.subKeys[5]
        var x0 = leToInt(src, srcOff + 8) xor ek.subKeys[6]
        var x1 = leToInt(src, srcOff + 12) xor ek.subKeys[7]

        for (r in ROUNDS - 1 downTo 0 step 2) {
            var t0 = g(x2, ek.sBoxes)
            var t1 = g(x3.rotateLeft(8), ek.sBoxes)
            x1 = (x1 xor (t0 + t1 + t1 + ek.subKeys[8 + 2 * r + 1])).rotateRight(1)
            x0 = x0.rotateLeft(1) xor (t0 + t1 + ek.subKeys[8 + 2 * r])

            t0 = g(x0, ek.sBoxes)
            t1 = g(x1.rotateLeft(8), ek.sBoxes)
            x3 = (x3 xor (t0 + t1 + t1 + ek.subKeys[8 + 2 * r - 1])).rotateRight(1)
            x2 = x2.rotateLeft(1) xor (t0 + t1 + ek.subKeys[8 + 2 * r - 2])
        }

        intToLe(x0 xor ek.subKeys[0], dst, dstOff)
        intToLe(x1 xor ek.subKeys[1], dst, dstOff + 4)
        intToLe(x2 xor ek.subKeys[2], dst, dstOff + 8)
        intToLe(x3 xor ek.subKeys[3], dst, dstOff + 12)
    }

    // ---- CBC mode with PKCS7 padding ----

    fun encryptCbc(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(iv.size == BLOCK_SIZE) { "IV must be $BLOCK_SIZE bytes" }
        val ek = expandKey(key)

        val padLen = BLOCK_SIZE - (data.size % BLOCK_SIZE)
        val padded = ByteArray(data.size + padLen)
        data.copyInto(padded)
        for (i in data.size until padded.size) padded[i] = padLen.toByte()

        val output = ByteArray(padded.size)
        val prev = iv.copyOf()

        for (offset in padded.indices step BLOCK_SIZE) {
            for (i in 0 until BLOCK_SIZE) {
                padded[offset + i] = (padded[offset + i].toInt() xor prev[i].toInt()).toByte()
            }
            encryptBlock(ek, padded, offset, output, offset)
            output.copyInto(prev, 0, offset, offset + BLOCK_SIZE)
        }
        return output
    }

    fun decryptCbc(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(iv.size == BLOCK_SIZE) { "IV must be $BLOCK_SIZE bytes" }
        require(data.size % BLOCK_SIZE == 0 && data.isNotEmpty()) { "Invalid ciphertext length" }
        val ek = expandKey(key)

        val output = ByteArray(data.size)
        var prev = iv.copyOf()

        for (offset in data.indices step BLOCK_SIZE) {
            decryptBlock(ek, data, offset, output, offset)
            for (i in 0 until BLOCK_SIZE) {
                output[offset + i] = (output[offset + i].toInt() xor prev[i].toInt()).toByte()
            }
            prev = data.copyOfRange(offset, offset + BLOCK_SIZE)
        }

        val padLen = output.last().toInt() and 0xFF
        require(padLen in 1..BLOCK_SIZE) { "Invalid PKCS7 padding: $padLen" }
        for (i in output.size - padLen until output.size) {
            require(output[i].toInt() and 0xFF == padLen) { "Invalid PKCS7 padding" }
        }
        return output.copyOf(output.size - padLen)
    }

    // ---- Little-endian conversion ----

    private fun leToInt(b: ByteArray, off: Int): Int = (b[off].toInt() and 0xFF) or
        ((b[off + 1].toInt() and 0xFF) shl 8) or
        ((b[off + 2].toInt() and 0xFF) shl 16) or
        ((b[off + 3].toInt() and 0xFF) shl 24)

    private fun intToLe(v: Int, b: ByteArray, off: Int) {
        b[off] = v.toByte()
        b[off + 1] = (v ushr 8).toByte()
        b[off + 2] = (v ushr 16).toByte()
        b[off + 3] = (v ushr 24).toByte()
    }
}
