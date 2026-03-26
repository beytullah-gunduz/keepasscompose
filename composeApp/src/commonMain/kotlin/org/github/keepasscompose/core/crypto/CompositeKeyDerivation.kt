package org.github.keepasscompose.core.crypto

import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdfParameters

/**
 * Implements KeePass composite key derivation.
 *
 * The derivation combines password, key file, and optional hardware key into a
 * single master key suitable for encrypting/decrypting a KDBX database.
 *
 * Flow:
 * 1. **Composite key hash**: SHA-256 of concatenated per-credential hashes
 * 2. **Transformed key**: Apply KDF (AES-KDF or Argon2) to the composite key hash
 * 3. **Master key**: SHA-256(masterSeed || transformedKey)
 *
 * For KDBX 4.x HMAC verification, the base key is derived separately:
 *   SHA-512(masterSeed || transformedKey || 0x01)
 */
class CompositeKeyDerivation(private val crypto: CryptoProvider) {
    /**
     * Result of the full key derivation, containing both the master key
     * (used for payload encryption) and the transformed key (used for HMAC).
     */
    data class DerivedKeys(val masterKey: ByteArray, val transformedKey: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DerivedKeys) return false
            return masterKey.contentEquals(other.masterKey) &&
                transformedKey.contentEquals(other.transformedKey)
        }

        override fun hashCode(): Int {
            var result = masterKey.contentHashCode()
            result = 31 * result + transformedKey.contentHashCode()
            return result
        }
    }

    /**
     * Derive the master key and transformed key from a composite key and header parameters.
     *
     * @param compositeKey The user's credentials (password, key file, hardware key).
     * @param masterSeed The 32-byte random seed from the KDBX header.
     * @param kdfParameters The KDF algorithm and its parameters from the header.
     * @return [DerivedKeys] containing the master key and transformed key.
     * @throws IllegalArgumentException if the composite key has no credentials.
     */
    fun deriveKeys(compositeKey: CompositeKey, masterSeed: ByteArray, kdfParameters: KdfParameters): DerivedKeys {
        val compositeKeyHash = deriveCompositeKeyHash(compositeKey)
        val transformedKey = deriveTransformedKey(compositeKeyHash, kdfParameters)
        val masterKey = crypto.sha256(masterSeed + transformedKey)
        return DerivedKeys(masterKey, transformedKey)
    }

    /**
     * Build the composite key hash: SHA-256 of the concatenation of each credential's
     * individual hash. Password: SHA-256(utf8Bytes). Key file: SHA-256(data).
     * Hardware key challenge is included as-is.
     */
    fun deriveCompositeKeyHash(compositeKey: CompositeKey): ByteArray {
        val parts = mutableListOf<ByteArray>()

        compositeKey.password?.let { password ->
            parts.add(crypto.sha256(password.encodeToByteArray()))
        }
        compositeKey.keyFileData?.let { keyData ->
            parts.add(crypto.sha256(keyData))
        }
        compositeKey.hardwareKeyChallenge?.let { challenge ->
            parts.add(challenge)
        }

        require(parts.isNotEmpty()) { "Composite key must contain at least one credential" }

        val concatenated = parts.fold(ByteArray(0)) { acc, part -> acc + part }
        return crypto.sha256(concatenated)
    }

    /**
     * Apply the KDF to the composite key hash to produce the transformed key.
     */
    fun deriveTransformedKey(compositeKeyHash: ByteArray, kdfParameters: KdfParameters): ByteArray = when (kdfParameters) {
        is KdfParameters.AesKdf -> {
            val transformed = crypto.aesKdf(compositeKeyHash, kdfParameters.seed, kdfParameters.rounds)
            crypto.sha256(transformed)
        }

        is KdfParameters.Argon2 -> {
            crypto.argon2(
                password = compositeKeyHash,
                salt = kdfParameters.salt,
                variant = kdfParameters.variant,
                version = kdfParameters.version,
                memory = kdfParameters.memory,
                iterations = kdfParameters.iterations,
                parallelism = kdfParameters.parallelism,
            )
        }
    }

    /**
     * Compute the HMAC base key for KDBX 4.x header and block verification.
     */
    fun computeHmacBaseKey(masterSeed: ByteArray, transformedKey: ByteArray): ByteArray =
        crypto.sha512(masterSeed + transformedKey + byteArrayOf(0x01))
}
