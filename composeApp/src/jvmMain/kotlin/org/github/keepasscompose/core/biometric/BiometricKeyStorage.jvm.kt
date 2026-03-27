package org.github.keepasscompose.core.biometric

/**
 * Desktop (JVM) biometric key storage stub.
 *
 * Biometric key storage is not available on standard desktop platforms.
 */
actual class BiometricKeyStorage actual constructor() {
    actual fun isKeyStored(databasePath: String): Boolean = false

    actual fun storeKey(databasePath: String, compositeKeyData: ByteArray) {
        // Stub: biometric key storage not available on desktop
    }

    actual fun retrieveKey(databasePath: String): ByteArray? = null

    actual fun deleteKey(databasePath: String) {
        // Stub
    }

    actual fun deleteAllKeys() {
        // Stub
    }
}
