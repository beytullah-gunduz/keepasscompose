package org.github.keepasscompose.core.crypto

import org.github.keepasscompose.core.model.Argon2Variant

interface CryptoProvider {
    // Hashing
    fun sha256(data: ByteArray): ByteArray

    fun sha512(data: ByteArray): ByteArray

    // HMAC
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    // AES-256-CBC
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

    fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

    // Key derivation
    fun aesKdf(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray

    fun argon2(
        password: ByteArray,
        salt: ByteArray,
        variant: Argon2Variant,
        version: Int,
        memory: Long,
        iterations: Long,
        parallelism: Int,
    ): ByteArray

    // Stream ciphers
    fun chaCha20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray

    fun salsa20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray

    // Twofish-CBC
    fun twofishEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

    fun twofishDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
}

expect class PlatformCryptoProvider() : CryptoProvider {
    override fun sha256(data: ByteArray): ByteArray
    override fun sha512(data: ByteArray): ByteArray
    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
    override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    override fun aesKdf(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray
    override fun argon2(
        password: ByteArray,
        salt: ByteArray,
        variant: Argon2Variant,
        version: Int,
        memory: Long,
        iterations: Long,
        parallelism: Int,
    ): ByteArray
    override fun chaCha20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray
    override fun salsa20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray
    override fun twofishEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    override fun twofishDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
}
