package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of Salsa20 (original variant, 20 rounds).
 * 256-bit key, 64-bit (8-byte) nonce, 64-bit block counter.
 *
 * Used by KeePass (KDBX 3.1) for the inner random stream that protects
 * field values in the XML payload. The nonce is fixed at
 * `{0xE8, 0x30, 0x09, 0x4B, 0x97, 0x20, 0x5D, 0x2A}` (Sigma bytes).
 *
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's Salsa20Engine instead.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object Salsa20 {

    private const val STATE_SIZE = 16 // 16 x 32-bit words
    private const val BLOCK_SIZE = 64 // 64 bytes per block

    // "expand 32-byte k" as little-endian 32-bit words
    private const val SIGMA0 = 0x61707865u // "expa"
    private const val SIGMA1 = 0x3320646eu // "nd 3"
    private const val SIGMA2 = 0x79622d32u // "2-by"
    private const val SIGMA3 = 0x6b206574u // "te k"

    fun encrypt(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        require(key.size == 32) { "Salsa20 key must be 32 bytes, got ${key.size}" }
        require(nonce.size == 8) { "Salsa20 nonce must be 8 bytes, got ${nonce.size}" }

        if (data.isEmpty()) return ByteArray(0)

        val output = ByteArray(data.size)
        val state = UIntArray(STATE_SIZE)
        val workingState = UIntArray(STATE_SIZE)
        val keyStream = ByteArray(BLOCK_SIZE)

        // Initialize state (Salsa20 layout for 256-bit key):
        // sigma0  key[0]  key[1]  key[2]
        // key[3]  sigma1  nonce0  nonce1
        // ctr_lo  ctr_hi  sigma2  key[4]
        // key[5]  key[6]  key[7]  sigma3
        state[0] = SIGMA0
        state[1] = littleEndianToUInt(key, 0)
        state[2] = littleEndianToUInt(key, 4)
        state[3] = littleEndianToUInt(key, 8)
        state[4] = littleEndianToUInt(key, 12)
        state[5] = SIGMA1
        state[6] = littleEndianToUInt(nonce, 0)
        state[7] = littleEndianToUInt(nonce, 4)
        state[8] = 0u // counter low
        state[9] = 0u // counter high
        state[10] = SIGMA2
        state[11] = littleEndianToUInt(key, 16)
        state[12] = littleEndianToUInt(key, 20)
        state[13] = littleEndianToUInt(key, 24)
        state[14] = littleEndianToUInt(key, 28)
        state[15] = SIGMA3

        var offset = 0
        while (offset < data.size) {
            // Copy state to working state
            state.copyInto(workingState)

            // 20 rounds (10 double-rounds)
            for (i in 0 until 10) {
                // Column rounds
                quarterRound(workingState, 0, 4, 8, 12)
                quarterRound(workingState, 5, 9, 13, 1)
                quarterRound(workingState, 10, 14, 2, 6)
                quarterRound(workingState, 15, 3, 7, 11)
                // Row rounds
                quarterRound(workingState, 0, 1, 2, 3)
                quarterRound(workingState, 5, 6, 7, 4)
                quarterRound(workingState, 10, 11, 8, 9)
                quarterRound(workingState, 15, 12, 13, 14)
            }

            // Add original state
            for (j in 0 until STATE_SIZE) {
                workingState[j] += state[j]
            }

            // Serialize to key stream bytes (little-endian)
            for (j in 0 until STATE_SIZE) {
                uintToLittleEndian(workingState[j], keyStream, j * 4)
            }

            // XOR with data
            val bytesToProcess = minOf(BLOCK_SIZE, data.size - offset)
            for (j in 0 until bytesToProcess) {
                output[offset + j] = (data[offset + j].toInt() xor keyStream[j].toInt()).toByte()
            }

            offset += BLOCK_SIZE
            // Increment 64-bit counter
            state[8]++
            if (state[8] == 0u) state[9]++
        }

        return output
    }

    private fun quarterRound(state: UIntArray, a: Int, b: Int, c: Int, d: Int) {
        state[b] = state[b] xor ((state[a] + state[d]).rotateLeft(7))
        state[c] = state[c] xor ((state[b] + state[a]).rotateLeft(9))
        state[d] = state[d] xor ((state[c] + state[b]).rotateLeft(13))
        state[a] = state[a] xor ((state[d] + state[c]).rotateLeft(18))
    }

    private fun littleEndianToUInt(bytes: ByteArray, offset: Int): UInt =
        (bytes[offset].toUByte().toUInt()) or
            (bytes[offset + 1].toUByte().toUInt() shl 8) or
            (bytes[offset + 2].toUByte().toUInt() shl 16) or
            (bytes[offset + 3].toUByte().toUInt() shl 24)

    private fun uintToLittleEndian(value: UInt, bytes: ByteArray, offset: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
        bytes[offset + 2] = (value shr 16).toByte()
        bytes[offset + 3] = (value shr 24).toByte()
    }
}
