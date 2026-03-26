package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of ChaCha20 (IETF variant, RFC 8439).
 * 256-bit key, 96-bit (12-byte) nonce, 32-bit block counter.
 *
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's ChaCha7539Engine instead.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object ChaCha20 {
    private const val STATE_SIZE = 16 // 16 x 32-bit words
    private const val BLOCK_SIZE = 64 // 64 bytes per block

    // "expand 32-byte k" as little-endian 32-bit words
    private const val SIGMA0 = 0x61707865u
    private const val SIGMA1 = 0x3320646eu
    private const val SIGMA2 = 0x79622d32u
    private const val SIGMA3 = 0x6b206574u

    fun encrypt(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        require(key.size == 32) { "ChaCha20 key must be 32 bytes, got ${key.size}" }
        require(nonce.size == 12) { "ChaCha20 nonce must be 12 bytes, got ${nonce.size}" }

        if (data.isEmpty()) return ByteArray(0)

        val output = ByteArray(data.size)
        val state = UIntArray(STATE_SIZE)
        val workingState = UIntArray(STATE_SIZE)
        val keyStream = ByteArray(BLOCK_SIZE)

        // Initialize state
        state[0] = SIGMA0
        state[1] = SIGMA1
        state[2] = SIGMA2
        state[3] = SIGMA3
        // Key words (little-endian)
        for (i in 0 until 8) {
            state[4 + i] = littleEndianToUInt(key, i * 4)
        }
        // Counter starts at 0 for IETF variant used by KeePass
        state[12] = 0u
        // Nonce words (little-endian)
        for (i in 0 until 3) {
            state[13 + i] = littleEndianToUInt(nonce, i * 4)
        }

        var offset = 0
        while (offset < data.size) {
            // Copy state to working state
            state.copyInto(workingState)

            // 20 rounds (10 double-rounds)
            for (i in 0 until 10) {
                // Column rounds
                quarterRound(workingState, 0, 4, 8, 12)
                quarterRound(workingState, 1, 5, 9, 13)
                quarterRound(workingState, 2, 6, 10, 14)
                quarterRound(workingState, 3, 7, 11, 15)
                // Diagonal rounds
                quarterRound(workingState, 0, 5, 10, 15)
                quarterRound(workingState, 1, 6, 11, 12)
                quarterRound(workingState, 2, 7, 8, 13)
                quarterRound(workingState, 3, 4, 9, 14)
            }

            // Add original state
            for (i in 0 until STATE_SIZE) {
                workingState[i] += state[i]
            }

            // Serialize to key stream bytes (little-endian)
            for (i in 0 until STATE_SIZE) {
                uintToLittleEndian(workingState[i], keyStream, i * 4)
            }

            // XOR with data
            val bytesToProcess = minOf(BLOCK_SIZE, data.size - offset)
            for (i in 0 until bytesToProcess) {
                output[offset + i] = (data[offset + i].toInt() xor keyStream[i].toInt()).toByte()
            }

            offset += BLOCK_SIZE
            // Increment counter
            state[12]++
        }

        return output
    }

    private fun quarterRound(state: UIntArray, a: Int, b: Int, c: Int, d: Int) {
        state[a] += state[b]
        state[d] = (state[d] xor state[a]).rotateLeft(16)
        state[c] += state[d]
        state[b] = (state[b] xor state[c]).rotateLeft(12)
        state[a] += state[b]
        state[d] = (state[d] xor state[a]).rotateLeft(8)
        state[c] += state[d]
        state[b] = (state[b] xor state[c]).rotateLeft(7)
    }

    private fun littleEndianToUInt(bytes: ByteArray, offset: Int): UInt = (bytes[offset].toUByte().toUInt()) or
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
