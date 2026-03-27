package org.github.keepasscompose.core.biometric

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiometricResultTest {
    @Test
    fun biometricTypeEnumHasExpectedValues() {
        val types = BiometricType.entries
        assertTrue(types.contains(BiometricType.FINGERPRINT))
        assertTrue(types.contains(BiometricType.FACE))
        assertTrue(types.contains(BiometricType.IRIS))
        assertTrue(types.contains(BiometricType.UNKNOWN))
        assertEquals(4, types.size)
    }

    @Test
    fun biometricResultSuccessIsDistinct() {
        val result: BiometricResult = BiometricResult.Success
        assertIs<BiometricResult.Success>(result)
    }

    @Test
    fun biometricResultFailedIsDistinct() {
        val result: BiometricResult = BiometricResult.Failed
        assertIs<BiometricResult.Failed>(result)
    }

    @Test
    fun biometricResultCancelledIsDistinct() {
        val result: BiometricResult = BiometricResult.Cancelled
        assertIs<BiometricResult.Cancelled>(result)
    }

    @Test
    fun biometricResultLockedOutCarriesPermanentFlag() {
        val temporary = BiometricResult.LockedOut(isPermanent = false)
        assertFalse(temporary.isPermanent)

        val permanent = BiometricResult.LockedOut(isPermanent = true)
        assertTrue(permanent.isPermanent)
    }

    @Test
    fun biometricResultErrorCarriesMessage() {
        val error = BiometricResult.Error("Test error message")
        assertEquals("Test error message", error.message)
    }

    @Test
    fun biometricPromptConfigDefaults() {
        val config = BiometricPromptConfig()
        assertEquals("Biometric Authentication", config.title)
        assertNull(config.subtitle)
        assertNull(config.description)
        assertEquals("Cancel", config.negativeButtonText)
        assertFalse(config.allowDeviceCredentialFallback)
    }

    @Test
    fun biometricPromptConfigCustomValues() {
        val config = BiometricPromptConfig(
            title = "Unlock",
            subtitle = "Verify identity",
            description = "Use fingerprint to unlock database",
            negativeButtonText = "Use Password",
            allowDeviceCredentialFallback = true,
        )
        assertEquals("Unlock", config.title)
        assertEquals("Verify identity", config.subtitle)
        assertEquals("Use fingerprint to unlock database", config.description)
        assertEquals("Use Password", config.negativeButtonText)
        assertTrue(config.allowDeviceCredentialFallback)
    }

    @Test
    fun jvmBiometricAuthenticatorNotAvailable() {
        val authenticator = BiometricAuthenticator()
        // On JVM (test env), biometrics are not available
        assertFalse(authenticator.isAvailable())
        assertNull(authenticator.getBiometricType())
    }

    @Test
    fun jvmAuthenticateReturnsNotAvailable() {
        val authenticator = BiometricAuthenticator()
        var result: BiometricResult? = null
        authenticator.authenticate("test") { result = it }
        assertIs<BiometricResult.NotAvailable>(result)
    }
}
