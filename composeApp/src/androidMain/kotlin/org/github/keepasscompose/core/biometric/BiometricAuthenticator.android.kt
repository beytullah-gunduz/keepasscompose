package org.github.keepasscompose.core.biometric

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.github.keepasscompose.core.common.AndroidContextHolder

/**
 * Android biometric authentication using BiometricPrompt API.
 *
 * Supports fingerprint, face, and iris authentication depending on
 * device capabilities and enrolled biometrics.
 */
actual class BiometricAuthenticator actual constructor() {
    private var cancellationSignal: androidx.core.os.CancellationSignal? = null

    actual fun isAvailable(): Boolean {
        val context = AndroidContextHolder.applicationContext ?: return false
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    actual fun getBiometricType(): BiometricType? {
        if (!isAvailable()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+, multiple biometric types may be available
            BiometricType.UNKNOWN
        } else {
            BiometricType.FINGERPRINT
        }
    }

    actual fun authenticate(reason: String, callback: (BiometricResult) -> Unit) {
        val context = AndroidContextHolder.applicationContext
        if (context == null) {
            callback(BiometricResult.NotAvailable)
            return
        }

        if (!isAvailable()) {
            callback(BiometricResult.NotAvailable)
            return
        }

        val activity = context as? FragmentActivity
        if (activity == null) {
            callback(BiometricResult.Error("Activity not available"))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    callback(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    callback(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val result = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        -> BiometricResult.Cancelled

                        BiometricPrompt.ERROR_LOCKOUT -> BiometricResult.LockedOut(isPermanent = false)

                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.LockedOut(isPermanent = true)

                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                        -> BiometricResult.NotAvailable

                        else -> BiometricResult.Error(errString.toString())
                    }
                    callback(result)
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Database")
            .setSubtitle(reason)
            .setNegativeButtonText("Use Password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    actual fun cancel() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}
