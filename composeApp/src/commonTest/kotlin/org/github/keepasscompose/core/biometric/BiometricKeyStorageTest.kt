package org.github.keepasscompose.core.biometric

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BiometricKeyStorageTest {
    @Test
    fun isKeyStoredReturnsFalseByDefault() {
        val storage = BiometricKeyStorage()
        assertFalse(storage.isKeyStored("/path/to/database.kdbx"))
    }

    @Test
    fun isKeyStoredReturnsFalseForDifferentPaths() {
        val storage = BiometricKeyStorage()
        assertFalse(storage.isKeyStored("/path/to/db1.kdbx"))
        assertFalse(storage.isKeyStored("/path/to/db2.kdbx"))
        assertFalse(storage.isKeyStored(""))
    }

    @Test
    fun retrieveKeyReturnsNullWhenNoKeyStored() {
        val storage = BiometricKeyStorage()
        assertNull(storage.retrieveKey("/path/to/database.kdbx"))
    }

    @Test
    fun retrieveKeyReturnsNullForDifferentPaths() {
        val storage = BiometricKeyStorage()
        assertNull(storage.retrieveKey("/path/to/db1.kdbx"))
        assertNull(storage.retrieveKey("/path/to/db2.kdbx"))
    }

    @Test
    fun deleteKeyDoesNotThrow() {
        val storage = BiometricKeyStorage()
        // Should not throw even when no key is stored
        storage.deleteKey("/path/to/database.kdbx")
    }

    @Test
    fun deleteAllKeysDoesNotThrow() {
        val storage = BiometricKeyStorage()
        // Should not throw even when no keys are stored
        storage.deleteAllKeys()
    }

    @Test
    fun storeKeyDoesNotThrowOnJvm() {
        val storage = BiometricKeyStorage()
        // On JVM/iOS stubs, storeKey is a no-op but should not throw
        storage.storeKey("/path/to/database.kdbx", byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun multipleOperationsDoNotThrow() {
        val storage = BiometricKeyStorage()
        val path = "/path/to/test.kdbx"

        // Sequence of operations should not throw
        storage.storeKey(path, byteArrayOf(10, 20, 30))
        storage.isKeyStored(path)
        storage.retrieveKey(path)
        storage.deleteKey(path)
        storage.deleteAllKeys()
    }
}
