package org.github.keepasscompose.core.common

/**
 * iOS autofill credential provider stub.
 *
 * A full implementation would integrate with ASCredentialProviderViewController
 * to surface KeePass entries in the iOS AutoFill QuickType bar.
 */
actual class AutofillProvider actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual fun provideCredentials(serviceIdentifier: String): List<AutofillCredential> = emptyList()
}
