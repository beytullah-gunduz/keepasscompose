package org.github.keepasscompose.core.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.engines.TwofishEngine
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.github.keepasscompose.core.model.Argon2Variant
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class PlatformCryptoProvider actual constructor() : CryptoProvider {
    override fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    override fun sha512(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-512").digest(data)

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    override fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher =
            PaddedBufferedBlockCipher(
                CBCBlockCipher.newInstance(AESEngine.newInstance()),
                PKCS7Padding(),
            )
        cipher.init(true, ParametersWithIV(KeyParameter(key), iv))
        val output = ByteArray(cipher.getOutputSize(data.size))
        val len = cipher.processBytes(data, 0, data.size, output, 0)
        cipher.doFinal(output, len)
        return output
    }

    override fun aesDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher =
            PaddedBufferedBlockCipher(
                CBCBlockCipher.newInstance(AESEngine.newInstance()),
                PKCS7Padding(),
            )
        cipher.init(false, ParametersWithIV(KeyParameter(key), iv))
        val output = ByteArray(cipher.getOutputSize(data.size))
        val len = cipher.processBytes(data, 0, data.size, output, 0)
        val finalLen = cipher.doFinal(output, len)
        return output.copyOf(len + finalLen)
    }

    override fun aesKdf(key: ByteArray, seed: ByteArray, rounds: Long): ByteArray {
        val aes = AESEngine.newInstance()
        aes.init(true, KeyParameter(seed))
        val result = key.copyOf()
        for (i in 0 until rounds) {
            aes.processBlock(result, 0, result, 0)
            aes.processBlock(result, 16, result, 16)
        }
        return result
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
        val type =
            when (variant) {
                Argon2Variant.ARGON2D -> Argon2Parameters.ARGON2_d
                Argon2Variant.ARGON2ID -> Argon2Parameters.ARGON2_id
            }
        val params =
            Argon2Parameters
                .Builder(type)
                .withVersion(version)
                .withSalt(salt)
                .withParallelism(parallelism)
                .withMemoryAsKB(memory.toInt())
                .withIterations(iterations.toInt())
                .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val result = ByteArray(32)
        generator.generateBytes(password, result)
        return result
    }

    override fun chaCha20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val engine = ChaCha7539Engine()
        engine.init(true, ParametersWithIV(KeyParameter(key), nonce))
        val output = ByteArray(data.size)
        engine.processBytes(data, 0, data.size, output, 0)
        return output
    }

    override fun salsa20(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val engine = Salsa20Engine()
        engine.init(true, ParametersWithIV(KeyParameter(key), nonce))
        val output = ByteArray(data.size)
        engine.processBytes(data, 0, data.size, output, 0)
        return output
    }

    override fun twofishEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher =
            PaddedBufferedBlockCipher(
                CBCBlockCipher.newInstance(TwofishEngine()),
                PKCS7Padding(),
            )
        cipher.init(true, ParametersWithIV(KeyParameter(key), iv))
        val output = ByteArray(cipher.getOutputSize(data.size))
        val len = cipher.processBytes(data, 0, data.size, output, 0)
        cipher.doFinal(output, len)
        return output
    }

    override fun twofishDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher =
            PaddedBufferedBlockCipher(
                CBCBlockCipher.newInstance(TwofishEngine()),
                PKCS7Padding(),
            )
        cipher.init(false, ParametersWithIV(KeyParameter(key), iv))
        val output = ByteArray(cipher.getOutputSize(data.size))
        val len = cipher.processBytes(data, 0, data.size, output, 0)
        val finalLen = cipher.doFinal(output, len)
        return output.copyOf(len + finalLen)
    }
}
