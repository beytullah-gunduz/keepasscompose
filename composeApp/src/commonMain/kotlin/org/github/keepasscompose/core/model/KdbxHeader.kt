package org.github.keepasscompose.core.model

data class KdbxVersion(val major: Int, val minor: Int) {
    val isV3: Boolean get() = major == 3
    val isV4: Boolean get() = major == 4
}

data class KdbxHeader(
    val version: KdbxVersion = KdbxVersion(4, 0),
    val cipher: CipherId = CipherId.AES_256,
    val compression: CompressionAlgorithm = CompressionAlgorithm.GZIP,
    val masterSeed: ByteArray = ByteArray(0),
    val encryptionIv: ByteArray = ByteArray(0),
    val kdfParameters: KdfParameters = KdfParameters.AesKdf(),
    val innerRandomStreamKey: ByteArray = ByteArray(0),
    val innerRandomStreamId: InnerStreamCipher = InnerStreamCipher.CHACHA20,
    val streamStartBytes: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdbxHeader) return false
        return version == other.version &&
            cipher == other.cipher &&
            compression == other.compression &&
            masterSeed.contentEquals(other.masterSeed) &&
            encryptionIv.contentEquals(other.encryptionIv) &&
            kdfParameters == other.kdfParameters &&
            innerRandomStreamKey.contentEquals(other.innerRandomStreamKey) &&
            innerRandomStreamId == other.innerRandomStreamId &&
            streamStartBytes.contentEquals(other.streamStartBytes)
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + cipher.hashCode()
        result = 31 * result + compression.hashCode()
        result = 31 * result + masterSeed.contentHashCode()
        result = 31 * result + encryptionIv.contentHashCode()
        result = 31 * result + kdfParameters.hashCode()
        result = 31 * result + innerRandomStreamKey.contentHashCode()
        result = 31 * result + innerRandomStreamId.hashCode()
        result = 31 * result + streamStartBytes.contentHashCode()
        return result
    }
}

enum class CipherId {
    AES_256,
    TWOFISH,
    CHACHA20,
}

enum class CompressionAlgorithm {
    NONE,
    GZIP,
}

enum class InnerStreamCipher {
    NONE,
    ARC4,
    SALSA20,
    CHACHA20,
}

enum class Argon2Variant {
    ARGON2D,
    ARGON2ID,
}

sealed class KdfParameters {
    data class AesKdf(val rounds: Long = 60000, val seed: ByteArray = ByteArray(0)) : KdfParameters() {
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
        val variant: Argon2Variant = Argon2Variant.ARGON2D,
        val version: Int = 0x13,
        val salt: ByteArray = ByteArray(0),
        val parallelism: Int = 2,
        val memory: Long = 65536,
        val iterations: Long = 3,
    ) : KdfParameters() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Argon2) return false
            return variant == other.variant &&
                version == other.version &&
                salt.contentEquals(other.salt) &&
                parallelism == other.parallelism &&
                memory == other.memory &&
                iterations == other.iterations
        }

        override fun hashCode(): Int {
            var result = variant.hashCode()
            result = 31 * result + version
            result = 31 * result + salt.contentHashCode()
            result = 31 * result + parallelism
            result = 31 * result + memory.hashCode()
            result = 31 * result + iterations.hashCode()
            return result
        }
    }
}
