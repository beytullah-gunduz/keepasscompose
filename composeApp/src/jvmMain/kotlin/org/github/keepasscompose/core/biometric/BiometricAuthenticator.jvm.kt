package org.github.keepasscompose.core.biometric

/**
 * Desktop (JVM) biometric authentication stub.
 *
 * Biometric authentication is not available on standard desktop platforms.
 */
actual class BiometricAuthenticator actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual fun getBiometricType(): BiometricType? = null

    actual fun authenticate(reason: String, callback: (BiometricResult) -> Unit) {
        callback(BiometricResult.NotAvailable)
    }

    actual fun cancel() {}
}
