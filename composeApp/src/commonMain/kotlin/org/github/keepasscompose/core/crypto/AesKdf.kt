package org.github.keepasscompose.core.crypto

/**
 * Pure Kotlin implementation of the AES-KDF key derivation used by KeePass (KDBX 3.1).
 *
 * The algorithm repeatedly AES-ECB encrypts a 32-byte composite key
 * using a transform seed, for a configurable number of rounds.
 *
 * Used on iOS where BouncyCastle is not available.
 * JVM/Android use BouncyCastle's AESEngine instead.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object AesKdf {

    /**
     * Derive a key by repeatedly AES-ECB encrypting [key] with [seed] for [rounds] iterations.
     *
     * @param key 32-byte composite key (two AES blocks)
     * @param seed 32-byte transform seed (used as AES-256 key)
     * @param rounds number of encryption rounds
     * @return the transformed 32-byte key
     */
    fun transform(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray {
        val result = key.copyOf()
        val roundKeys = Aes.expandKey(seed)
        for (i in 0 until rounds) {
            Aes.encryptBlock(result, 0, roundKeys)
            Aes.encryptBlock(result, 16, roundKeys)
        }
        return result
    }
}
