package org.github.keepasscompose.core.browser

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Cryptographic operations for the KeePassXC-Browser protocol.
 *
 * The protocol uses TweetNaCl-compatible public-key authenticated encryption
 * (X25519 + XSalsa20-Poly1305) for all message payloads after the initial
 * key exchange. This class provides an abstraction over the underlying
 * crypto primitives so each platform can supply a concrete implementation.
 */
interface BrowserCrypto {
    /**
     * Generate a new X25519 key pair.
     * @return Pair of (publicKey, secretKey), each 32 bytes.
     */
    fun generateKeyPair(): Pair<ByteArray, ByteArray>

    /**
     * Encrypt a message using NaCl crypto_box (X25519 + XSalsa20-Poly1305).
     *
     * @param message The plaintext bytes to encrypt.
     * @param nonce 24-byte nonce.
     * @param theirPublicKey The recipient's 32-byte public key.
     * @param mySecretKey Our 32-byte secret key.
     * @return The encrypted + authenticated ciphertext.
     */
    fun boxEncrypt(message: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray): ByteArray

    /**
     * Decrypt a message using NaCl crypto_box_open.
     *
     * @param ciphertext The encrypted bytes.
     * @param nonce 24-byte nonce used during encryption.
     * @param theirPublicKey The sender's 32-byte public key.
     * @param mySecretKey Our 32-byte secret key.
     * @return The decrypted plaintext bytes, or null if authentication fails.
     */
    fun boxDecrypt(ciphertext: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, mySecretKey: ByteArray): ByteArray?

    companion object {
        const val KEY_SIZE = 32
        const val NONCE_SIZE = 24

        /**
         * Generate a random 24-byte nonce.
         */
        fun generateNonce(): ByteArray = Random.nextBytes(NONCE_SIZE)

        /**
         * Increment a nonce by 1 (little-endian) for the response nonce.
         */
        fun incrementNonce(nonce: ByteArray): ByteArray {
            val result = nonce.copyOf()
            for (i in result.indices) {
                result[i]++
                if (result[i] != 0.toByte()) break
            }
            return result
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun encodeBase64(data: ByteArray): String = Base64.encode(data)

        @OptIn(ExperimentalEncodingApi::class)
        fun decodeBase64(data: String): ByteArray = Base64.decode(data)
    }
}
