package org.github.keepasscompose.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.github.keepasscompose.core.model.Argon2Variant
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CCOperation
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA512
import platform.CoreCrypto.CC_SHA512_DIGEST_LENGTH
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.posix.size_tVar
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH as HMAC_SHA256_LENGTH

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
        val mac = UByteArray(HMAC_SHA256_LENGTH)
        // Pin all three arrays simultaneously for the CCHmac call
        val keyToPin = if (key.isEmpty()) byteArrayOf(0) else key
        val dataToPin = if (data.isEmpty()) byteArrayOf(0) else data
        mac.usePinned { macPin ->
            keyToPin.usePinned { keyPin ->
                dataToPin.usePinned { dataPin ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        if (key.isEmpty()) null else keyPin.addressOf(0),
                        key.size.convert(),
                        if (data.isEmpty()) null else dataPin.addressOf(0),
                        data.size.convert(),
                        macPin.addressOf(0),
                    )
                }
            }
        }
        return mac.toByteArray()
    }

    override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = ccCryptAes(kCCEncrypt, data, key, iv)

    override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = ccCryptAes(kCCDecrypt, data, key, iv)

    private fun ccCryptAes(operation: CCOperation, data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // Output buffer: encrypt may add up to one block of padding (16 bytes)
        val outputSize = data.size + 16
        val output = ByteArray(outputSize)
        memScoped {
            val dataOutMoved = alloc<size_tVar>()
            val status =
                data.usePinned { dataPin ->
                    key.usePinned { keyPin ->
                        iv.usePinned { ivPin ->
                            output.usePinned { outPin ->
                                CCCrypt(
                                    operation,
                                    kCCAlgorithmAES,
                                    kCCOptionPKCS7Padding,
                                    keyPin.addressOf(0),
                                    key.size.convert(),
                                    ivPin.addressOf(0),
                                    dataPin.addressOf(0),
                                    data.size.convert(),
                                    outPin.addressOf(0),
                                    outputSize.convert(),
                                    dataOutMoved.ptr,
                                )
                            }
                        }
                    }
                }
            if (status != kCCSuccess) {
                throw RuntimeException("CCCrypt failed with status: $status")
            }
            return output.copyOf(dataOutMoved.value.toInt())
        }
    }

    override fun aesKdf(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray = AesKdf.transform(key, seed, rounds)

    override fun argon2(
        password: ByteArray,
        salt: ByteArray,
        variant: Argon2Variant,
        version: Int,
        memory: Long,
        iterations: Long,
        parallelism: Int,
    ): ByteArray = Argon2.derive(password, salt, variant, version, memory, iterations, parallelism)

    override fun chaCha20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray = ChaCha20.encrypt(data, key, nonce)

    override fun salsa20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray = Salsa20.encrypt(data, key, nonce)

    override fun twofishEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = Twofish.encryptCbc(data, key, iv)

    override fun twofishDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray = Twofish.decryptCbc(data, key, iv)
}
