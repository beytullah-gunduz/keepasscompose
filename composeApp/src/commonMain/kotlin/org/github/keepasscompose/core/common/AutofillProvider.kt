package org.github.keepasscompose.core.common

/** A credential returned by the platform autofill provider. */
data class AutofillCredential(val username: String, val password: String, val serviceIdentifier: String)

/**
 * Platform-specific autofill provider abstraction.
 *
 * Each platform implements the ability to query whether autofill is available
 * and to supply credentials for a given service identifier (URL / app package).
 */
expect class AutofillProvider() {
    /** Returns `true` when the platform autofill integration is available. */
    fun isAvailable(): Boolean

    /** Returns matching credentials for the given [serviceIdentifier]. */
    fun provideCredentials(serviceIdentifier: String): List<AutofillCredential>
}
