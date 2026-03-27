package org.github.keepasscompose.core.biometric

/**
 * Platform-abstracted biometric authentication.
 *
 * Provides a unified interface for biometric authentication across platforms:
 * - **Android**: Uses `BiometricPrompt` (fingerprint, face, iris)
 * - **iOS**: Uses `LAContext` (Face ID, Touch ID)
 * - **Desktop (JVM)**: Stub (biometrics not available)
 */
expect class BiometricAuthenticator() {
    /**
     * Whether biometric authentication is available on this device.
     * Checks for hardware capability and enrolled biometrics.
     */
    fun isAvailable(): Boolean

    /**
     * The type of biometric available (e.g., "Fingerprint", "Face ID").
     * Returns null if no biometric is available.
     */
    fun getBiometricType(): BiometricType?

    /**
     * Authenticate the user using biometrics.
     *
     * @param reason A description of why authentication is needed, shown to the user.
     * @param callback Called with the result of the authentication attempt.
     */
    fun authenticate(reason: String, callback: (BiometricResult) -> Unit)

    /**
     * Cancel any in-progress authentication.
     */
    fun cancel()
}

/**
 * Types of biometric authentication available.
 */
enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS,
    UNKNOWN,
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    /** Authentication succeeded. */
    data object Success : BiometricResult()

    /** Authentication failed (wrong biometric). */
    data object Failed : BiometricResult()

    /** User cancelled the authentication. */
    data object Cancelled : BiometricResult()

    /** Biometric is not available or not enrolled. */
    data object NotAvailable : BiometricResult()

    /** Too many failed attempts — biometric is locked out. */
    data class LockedOut(val isPermanent: Boolean) : BiometricResult()

    /** An error occurred during authentication. */
    data class Error(val message: String) : BiometricResult()
}

/**
 * Configuration for biometric prompts.
 */
data class BiometricPromptConfig(
    val title: String = "Biometric Authentication",
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButtonText: String = "Cancel",
    val allowDeviceCredentialFallback: Boolean = false,
)
