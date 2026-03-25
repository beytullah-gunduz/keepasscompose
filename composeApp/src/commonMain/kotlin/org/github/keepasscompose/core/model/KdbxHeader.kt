package org.github.keepasscompose.core.model

data class KdbxHeader(
    val cipher: CipherId = CipherId.AES_256,
    val compression: CompressionAlgorithm = CompressionAlgorithm.GZIP,
    val masterSeed: ByteArray = ByteArray(0),
    val encryptionIv: ByteArray = ByteArray(0),
    val kdfParameters: KdfParameters = KdfParameters.AesKdf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxHeader) return false
        return cipher == other.cipher &&
            compression == other.compression &&
            masterSeed.contentEquals(other.masterSeed) &&
            encryptionIv.contentEquals(other.encryptionIv) &&
            kdfParameters == other.kdfParameters
    }

    override fun hashCode(): Int {
        var result = cipher.hashCode()
        result = 31 * result + compression.hashCode()
        result = 31 * result + masterSeed.contentHashCode()
        result = 31 * result + encryptionIv.contentHashCode()
        result = 31 * result + kdfParameters.hashCode()
        return result
    }
}

enum class CipherId {
    AES_256,
    CHACHA20,
}

enum class CompressionAlgorithm {
    NONE,
    GZIP,
}

sealed class KdfParameters {
    data class AesKdf(
        val rounds: Long = 60000,
        val seed: ByteArray = ByteArray(0),
    ) : KdfParameters() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AesKdf) return false
            return rounds == other.rounds && seed.contentEquals(other.seed)
        }

        override fun hashCode(): Int {
            var result = rounds.hashCode()
            result = 31 * result + seed.contentHashCode()
            return result
        }
    }

    data class Argon2(
        val version: Int = 0x13,
        val salt: ByteArray = ByteArray(0),
        val parallelism: Int = 2,
        val memory: Long = 65536,
        val iterations: Long = 3,
    ) : KdfParameters() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Argon2) return false
            return version == other.version &&
                salt.contentEquals(other.salt) &&
                parallelism == other.parallelism &&
                memory == other.memory &&
                iterations == other.iterations
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + salt.contentHashCode()
            result = 31 * result + parallelism
            result = 31 * result + memory.hashCode()
            result = 31 * result + iterations.hashCode()
            return result
        }
    }
}
