package org.github.keepasscompose.core.biometric

/**
 * Platform-abstracted secure key storage for biometric unlock.
 *
 * Stores the composite key encrypted with a biometric-protected key so that
 * the database can be unlocked using biometric authentication alone.
 *
 * - **Android**: Uses Android KeyStore for AES key generation with biometric
 *   authentication binding, and SharedPreferences for encrypted data storage.
 * - **iOS**: Stub (real implementation would use Keychain with biometric ACL).
 * - **Desktop (JVM)**: Stub (biometric key storage is not available).
 */
expect class BiometricKeyStorage() {
    /**
     * Returns `true` if a biometric-protected key is stored for the given database.
     *
     * @param databasePath Path identifying the database.
     */
    fun isKeyStored(databasePath: String): Boolean

    /**
     * Stores the composite key data encrypted with a biometric-protected key.
     *
     * @param databasePath Path identifying the database.
     * @param compositeKeyData The raw composite key bytes to encrypt and store.
     */
    fun storeKey(databasePath: String, compositeKeyData: ByteArray)

    /**
     * Retrieves and decrypts the stored composite key data.
     *
     * @param databasePath Path identifying the database.
     * @return The decrypted composite key bytes, or `null` if no key is stored.
     */
    fun retrieveKey(databasePath: String): ByteArray?

    /**
     * Deletes the stored key for the given database.
     *
     * @param databasePath Path identifying the database.
     */
    fun deleteKey(databasePath: String)

    /**
     * Deletes all stored biometric keys for all databases.
     */
    fun deleteAllKeys()
}
