package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of AES-256 ECB encryption.
 * Only implements single-block encryption, which is all that [AesKdf] needs.
 *
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's AESEngine instead.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object Aes {

    private const val BLOCK_SIZE = 16
    private const val KEY_SIZE = 32 // AES-256
    private const val ROUNDS = 14 // AES-256 uses 14 rounds
    private const val NK = 8 // Key length in 32-bit words (256 / 32)
    private const val NB = 4 // Block size in 32-bit words

    // AES S-Box
    private val SBOX = ubyteArrayOf(
        0x63u, 0x7Cu, 0x77u, 0x7Bu, 0xF2u, 0x6Bu, 0x6Fu, 0xC5u, 0x30u, 0x01u, 0x67u, 0x2Bu, 0xFEu, 0xD7u, 0xABu, 0x76u,
        0xCAu, 0x82u, 0xC9u, 0x7Du, 0xFAu, 0x59u, 0x47u, 0xF0u, 0xADu, 0xD4u, 0xA2u, 0xAFu, 0x9Cu, 0xA4u, 0x72u, 0xC0u,
        0xB7u, 0xFDu, 0x93u, 0x26u, 0x36u, 0x3Fu, 0xF7u, 0xCCu, 0x34u, 0xA5u, 0xE5u, 0xF1u, 0x71u, 0xD8u, 0x31u, 0x15u,
        0x04u, 0xC7u, 0x23u, 0xC3u, 0x18u, 0x96u, 0x05u, 0x9Au, 0x07u, 0x12u, 0x80u, 0xE2u, 0xEBu, 0x27u, 0xB2u, 0x75u,
        0x09u, 0x83u, 0x2Cu, 0x1Au, 0x1Bu, 0x6Eu, 0x5Au, 0xA0u, 0x52u, 0x3Bu, 0xD6u, 0xB3u, 0x29u, 0xE3u, 0x2Fu, 0x84u,
        0x53u, 0xD1u, 0x00u, 0xEDu, 0x20u, 0xFCu, 0xB1u, 0x5Bu, 0x6Au, 0xCBu, 0xBEu, 0x39u, 0x4Au, 0x4Cu, 0x58u, 0xCFu,
        0xD0u, 0xEFu, 0xAAu, 0xFBu, 0x43u, 0x4Du, 0x33u, 0x85u, 0x45u, 0xF9u, 0x02u, 0x7Fu, 0x50u, 0x3Cu, 0x9Fu, 0xA8u,
        0x51u, 0xA3u, 0x40u, 0x8Fu, 0x92u, 0x9Du, 0x38u, 0xF5u, 0xBCu, 0xB6u, 0xDAu, 0x21u, 0x10u, 0xFFu, 0xF3u, 0xD2u,
        0xCDu, 0x0Cu, 0x13u, 0xECu, 0x5Fu, 0x97u, 0x44u, 0x17u, 0xC4u, 0xA7u, 0x7Eu, 0x3Du, 0x64u, 0x5Du, 0x19u, 0x73u,
        0x60u, 0x81u, 0x4Fu, 0xDCu, 0x22u, 0x2Au, 0x90u, 0x88u, 0x46u, 0xEEu, 0xB8u, 0x14u, 0xDEu, 0x5Eu, 0x0Bu, 0xDBu,
        0xE0u, 0x32u, 0x3Au, 0x0Au, 0x49u, 0x06u, 0x24u, 0x5Cu, 0xC2u, 0xD3u, 0xACu, 0x62u, 0x91u, 0x95u, 0xE4u, 0x79u,
        0xE7u, 0xC8u, 0x37u, 0x6Du, 0x8Du, 0xD5u, 0x4Eu, 0xA9u, 0x6Cu, 0x56u, 0xF4u, 0xEAu, 0x65u, 0x7Au, 0xAEu, 0x08u,
        0xBAu, 0x78u, 0x25u, 0x2Eu, 0x1Cu, 0xA6u, 0xB4u, 0xC6u, 0xE8u, 0xDDu, 0x74u, 0x1Fu, 0x4Bu, 0xBDu, 0x8Bu, 0x8Au,
        0x70u, 0x3Eu, 0xB5u, 0x66u, 0x48u, 0x03u, 0xF6u, 0x0Eu, 0x61u, 0x35u, 0x57u, 0xB9u, 0x86u, 0xC1u, 0x1Du, 0x9Eu,
        0xE1u, 0xF8u, 0x98u, 0x11u, 0x69u, 0xD9u, 0x8Eu, 0x94u, 0x9Bu, 0x1Eu, 0x87u, 0xE9u, 0xCEu, 0x55u, 0x28u, 0xDFu,
        0x8Cu, 0xA1u, 0x89u, 0x0Du, 0xBFu, 0xE6u, 0x42u, 0x68u, 0x41u, 0x99u, 0x2Du, 0x0Fu, 0xB0u, 0x54u, 0xBBu, 0x16u,
    )

    // Round constants
    private val RCON = uintArrayOf(
        0x01000000u, 0x02000000u, 0x04000000u, 0x08000000u,
        0x10000000u, 0x20000000u, 0x40000000u, 0x80000000u,
        0x1B000000u, 0x36000000u,
    )

    /**
     * Create an expanded key schedule for AES-256 encryption.
     * Returns the round keys as an array of 60 UInt words (15 rounds × 4 words).
     */
    fun expandKey(key: ByteArray): UIntArray {
        require(key.size == KEY_SIZE) { "AES-256 key must be 32 bytes, got ${key.size}" }

        val w = UIntArray(NB * (ROUNDS + 1)) // 60 words

        // Copy key into first NK words
        for (i in 0 until NK) {
            w[i] = (key[4 * i].toUByte().toUInt() shl 24) or
                (key[4 * i + 1].toUByte().toUInt() shl 16) or
                (key[4 * i + 2].toUByte().toUInt() shl 8) or
                key[4 * i + 3].toUByte().toUInt()
        }

        for (i in NK until w.size) {
            var temp = w[i - 1]
            if (i % NK == 0) {
                temp = subWord(rotWord(temp)) xor RCON[i / NK - 1]
            } else if (i % NK == 4) {
                temp = subWord(temp)
            }
            w[i] = w[i - NK] xor temp
        }

        return w
    }

    /**
     * Encrypt a single 16-byte block using the expanded key.
     * Operates in-place on [block] starting at [offset].
     */
    fun encryptBlock(block: ByteArray, offset: Int, roundKeys: UIntArray) {
        // Load state (column-major order)
        val s = Array(4) { col ->
            UByteArray(4) { row -> block[offset + col * 4 + row].toUByte() }
        }

        // Initial round key addition
        addRoundKey(s, roundKeys, 0)

        // Rounds 1 to ROUNDS-1
        for (round in 1 until ROUNDS) {
            subBytes(s)
            shiftRows(s)
            mixColumns(s)
            addRoundKey(s, roundKeys, round)
        }

        // Final round (no MixColumns)
        subBytes(s)
        shiftRows(s)
        addRoundKey(s, roundKeys, ROUNDS)

        // Store state back
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                block[offset + col * 4 + row] = s[col][row].toByte()
            }
        }
    }

    private fun subBytes(s: Array<UByteArray>) {
        for (col in 0 until 4) {
            for (row in 0 until 4) {
                s[col][row] = SBOX[s[col][row].toInt()]
            }
        }
    }

    private fun shiftRows(s: Array<UByteArray>) {
        // Row 0: no shift
        // Row 1: shift left by 1
        var tmp = s[0][1]
        s[0][1] = s[1][1]; s[1][1] = s[2][1]; s[2][1] = s[3][1]; s[3][1] = tmp
        // Row 2: shift left by 2
        tmp = s[0][2]; s[0][2] = s[2][2]; s[2][2] = tmp
        tmp = s[1][2]; s[1][2] = s[3][2]; s[3][2] = tmp
        // Row 3: shift left by 3 (= shift right by 1)
        tmp = s[3][3]
        s[3][3] = s[2][3]; s[2][3] = s[1][3]; s[1][3] = s[0][3]; s[0][3] = tmp
    }

    private fun mixColumns(s: Array<UByteArray>) {
        for (col in 0 until 4) {
            val a = UByteArray(4) { s[col][it] }
            s[col][0] = (gmul(2u, a[0]) xor gmul(3u, a[1]) xor a[2] xor a[3])
            s[col][1] = (a[0] xor gmul(2u, a[1]) xor gmul(3u, a[2]) xor a[3])
            s[col][2] = (a[0] xor a[1] xor gmul(2u, a[2]) xor gmul(3u, a[3]))
            s[col][3] = (gmul(3u, a[0]) xor a[1] xor a[2] xor gmul(2u, a[3]))
        }
    }

    private fun addRoundKey(s: Array<UByteArray>, w: UIntArray, round: Int) {
        for (col in 0 until 4) {
            val word = w[round * NB + col]
            s[col][0] = s[col][0] xor (word shr 24).toUByte()
            s[col][1] = s[col][1] xor (word shr 16).toUByte()
            s[col][2] = s[col][2] xor (word shr 8).toUByte()
            s[col][3] = s[col][3] xor word.toUByte()
        }
    }

    /** Galois field multiplication in GF(2^8) with irreducible polynomial x^8 + x^4 + x^3 + x + 1. */
    private fun gmul(a: UByte, b: UByte): UByte {
        var p = 0u
        var aa = a.toUInt()
        var bb = b.toUInt()
        for (i in 0 until 8) {
            if (bb and 1u != 0u) p = p xor aa
            val hiBit = aa and 0x80u
            aa = (aa shl 1) and 0xFFu
            if (hiBit != 0u) aa = aa xor 0x1Bu
            bb = bb shr 1
        }
        return p.toUByte()
    }

    private fun subWord(word: UInt): UInt {
        return (SBOX[(word shr 24).toInt() and 0xFF].toUInt() shl 24) or
            (SBOX[(word shr 16).toInt() and 0xFF].toUInt() shl 16) or
            (SBOX[(word shr 8).toInt() and 0xFF].toUInt() shl 8) or
            SBOX[word.toInt() and 0xFF].toUInt()
    }

    private fun rotWord(word: UInt): UInt = (word shl 8) or (word shr 24)
}
