package org.github.keepasscompose.core.biometric

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeTouchID
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

/**
 * iOS biometric authentication using LocalAuthentication framework.
 *
 * Supports Face ID and Touch ID depending on device capabilities.
 */
actual class BiometricAuthenticator actual constructor() {
    private var context: LAContext? = null

    @OptIn(ExperimentalForeignApi::class)
    actual fun isAvailable(): Boolean {
        val ctx = LAContext()
        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            ctx.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error = error.ptr,
            )
        }
    }

    actual fun getBiometricType(): BiometricType? {
        val ctx = LAContext()
        return when (ctx.biometryType) {
            LABiometryTypeFaceID -> BiometricType.FACE
            LABiometryTypeTouchID -> BiometricType.FINGERPRINT
            else -> null
        }
    }

    actual fun authenticate(reason: String, callback: (BiometricResult) -> Unit) {
        if (!isAvailable()) {
            callback(BiometricResult.NotAvailable)
            return
        }

        val ctx = LAContext()
        context = ctx

        ctx.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason,
        ) { success, error ->
            val result = when {
                success -> BiometricResult.Success

                error != null -> {
                    when (error.code) {
                        -2L -> BiometricResult.Cancelled

                        // LAErrorUserCancel
                        -1L -> BiometricResult.Failed

                        // LAErrorAuthenticationFailed
                        -8L -> BiometricResult.LockedOut(isPermanent = true)

                        // LAErrorBiometryLockout
                        else -> BiometricResult.Error(error.localizedDescription ?: "Unknown error")
                    }
                }

                else -> BiometricResult.Failed
            }
            callback(result)
        }
    }

    actual fun cancel() {
        context?.invalidate()
        context = null
    }
}
