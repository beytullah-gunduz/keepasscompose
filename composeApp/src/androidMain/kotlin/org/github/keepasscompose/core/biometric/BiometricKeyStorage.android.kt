package org.github.keepasscompose.core.biometric

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.github.keepasscompose.core.common.AndroidContextHolder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android biometric key storage using Android KeyStore.
 *
 * Generates an AES-256-GCM key protected by biometric authentication and
 * uses it to encrypt/decrypt composite key data. Encrypted data (IV + ciphertext)
 * is stored in SharedPreferences.
 */
actual class BiometricKeyStorage actual constructor() {
    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS_PREFIX = "keepass_biometric_"
        const val PREFS_NAME = "biometric_key_storage"
        const val AES_GCM_TAG_LENGTH = 128
        const val GCM_IV_LENGTH = 12
    }

    actual fun isKeyStored(databasePath: String): Boolean {
        val prefs = getPreferences() ?: return false
        return prefs.contains(storageKey(databasePath))
    }

    actual fun storeKey(databasePath: String, compositeKeyData: ByteArray) {
        val prefs = getPreferences() ?: return
        val secretKey = getOrCreateKey(databasePath)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(compositeKeyData)

        // Store IV + encrypted data as a single Base64-encoded blob
        val combined = ByteArray(GCM_IV_LENGTH + encrypted.size)
        iv.copyInto(combined, 0, 0, GCM_IV_LENGTH)
        encrypted.copyInto(combined, GCM_IV_LENGTH)

        prefs.edit()
            .putString(storageKey(databasePath), android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP))
            .apply()
    }

    actual fun retrieveKey(databasePath: String): ByteArray? {
        val prefs = getPreferences() ?: return null
        val encoded = prefs.getString(storageKey(databasePath), null) ?: return null

        val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        if (combined.size <= GCM_IV_LENGTH) return null

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val secretKey = loadKey(databasePath) ?: return null

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(AES_GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    actual fun deleteKey(databasePath: String) {
        // Remove from SharedPreferences
        getPreferences()?.edit()?.remove(storageKey(databasePath))?.apply()
        // Remove from KeyStore
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.deleteEntry(keyAlias(databasePath))
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
    }

    actual fun deleteAllKeys() {
        // Clear all preferences
        getPreferences()?.edit()?.clear()?.apply()
        // Remove all biometric keys from KeyStore
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.aliases().toList()
                .filter { it.startsWith(KEY_ALIAS_PREFIX) }
                .forEach { keyStore.deleteEntry(it) }
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun getPreferences(): android.content.SharedPreferences? {
        val context = AndroidContextHolder.applicationContext ?: return null
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun storageKey(databasePath: String): String = "encrypted_key_${databasePath.hashCode()}"

    private fun keyAlias(databasePath: String): String = "$KEY_ALIAS_PREFIX${databasePath.hashCode()}"

    private fun getOrCreateKey(databasePath: String): SecretKey {
        loadKey(databasePath)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias(databasePath),
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun loadKey(databasePath: String): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        val entry = keyStore.getEntry(keyAlias(databasePath), null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }
}
