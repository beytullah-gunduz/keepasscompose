package org.github.keepasscompose.core.common

/**
 * Android autofill credential provider stub.
 *
 * Credential filling on Android is handled by [KeePassAutofillService];
 * this class exists only to satisfy the expect/actual contract.
 */
actual class AutofillProvider actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual fun provideCredentials(serviceIdentifier: String): List<AutofillCredential> = emptyList()
}
