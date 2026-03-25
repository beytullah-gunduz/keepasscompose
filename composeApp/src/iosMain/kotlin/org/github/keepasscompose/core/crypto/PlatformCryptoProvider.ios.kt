package org.github.keepasscompose.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.github.keepasscompose.core.model.Argon2Variant
import platform.CommonCrypto.CC_SHA256
import platform.CommonCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CommonCrypto.CC_SHA512
import platform.CommonCrypto.CC_SHA512_DIGEST_LENGTH

// iOS implementation will use a hybrid approach:
// - Apple CommonCrypto (via CInterop) for AES-CBC, SHA-256/512, HMAC-SHA-256
// - libsodium (via kotlin-libsodium or CInterop) for Argon2, ChaCha20, Salsa20
// - Twofish requires a pure Kotlin or C library via CInterop
@OptIn(ExperimentalForeignApi::class)
actual class PlatformCryptoProvider actual constructor() : CryptoProvider {

    override fun sha256(data: ByteArray): ByteArray {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        digest.usePinned { digestPin ->
            if (data.isEmpty()) {
                CC_SHA256(null, 0u, digestPin.addressOf(0))
            } else {
                data.usePinned { dataPin ->
                    CC_SHA256(dataPin.addressOf(0), data.size.convert(), digestPin.addressOf(0))
                }
            }
        }
        return digest.toByteArray()
    }

    override fun sha512(data: ByteArray): ByteArray {
        val digest = UByteArray(CC_SHA512_DIGEST_LENGTH)
        digest.usePinned { digestPin ->
            if (data.isEmpty()) {
                CC_SHA512(null, 0u, digestPin.addressOf(0))
            } else {
                data.usePinned { dataPin ->
                    CC_SHA512(dataPin.addressOf(0), data.size.convert(), digestPin.addressOf(0))
                }
            }
        }
        return digest.toByteArray()
    }

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        TODO("iOS: Implement via CommonCrypto CCHmac")
    }

    override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        TODO("iOS: Implement via CommonCrypto CCCrypt with kCCAlgorithmAES")
    }

    override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        TODO("iOS: Implement via CommonCrypto CCCrypt with kCCAlgorithmAES")
    }

    override fun aesKdf(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray {
        TODO("iOS: Implement via CommonCrypto CCCrypt with kCCAlgorithmAES in ECB mode")
    }

    override fun argon2(
        password: ByteArray,
        salt: ByteArray,
        variant: Argon2Variant,
        version: Int,
        memory: Long,
        iterations: Long,
        parallelism: Int,
    ): ByteArray {
        TODO("iOS: Implement via libsodium crypto_pwhash")
    }

    override fun chaCha20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        TODO("iOS: Implement via libsodium crypto_stream_chacha20_ietf_xor")
    }

    override fun salsa20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        TODO("iOS: Implement via libsodium crypto_stream_salsa20_xor")
    }

    override fun twofishEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        TODO("iOS: Implement via CInterop with a C Twofish library")
    }

    override fun twofishDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        TODO("iOS: Implement via CInterop with a C Twofish library")
    }
}
