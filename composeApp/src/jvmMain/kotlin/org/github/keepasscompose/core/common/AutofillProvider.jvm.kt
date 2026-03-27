package org.github.keepasscompose.core.common

/**
 * Desktop (JVM) autofill credential provider stub.
 *
 * Desktop platforms do not have a native autofill framework;
 * this class exists only to satisfy the expect/actual contract.
 */
actual class AutofillProvider actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual fun provideCredentials(serviceIdentifier: String): List<AutofillCredential> = emptyList()
}
