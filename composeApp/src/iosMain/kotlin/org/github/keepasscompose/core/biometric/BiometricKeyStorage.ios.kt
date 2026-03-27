package org.github.keepasscompose.core.biometric

/**
 * iOS biometric key storage stub.
 *
 * A full implementation would use the iOS Keychain with a biometric
 * access control (kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
 * + SecAccessControlCreateFlags.biometryCurrentSet) to store and
 * retrieve composite keys protected by Face ID or Touch ID.
 */
actual class BiometricKeyStorage actual constructor() {
    actual fun isKeyStored(databasePath: String): Boolean = false

    actual fun storeKey(databasePath: String, compositeKeyData: ByteArray) {
        // Stub: real implementation would use Keychain with biometric ACL
    }

    actual fun retrieveKey(databasePath: String): ByteArray? = null

    actual fun deleteKey(databasePath: String) {
        // Stub
    }

    actual fun deleteAllKeys() {
        // Stub
    }
}
