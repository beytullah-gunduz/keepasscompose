package org.github.keepasscompose.core.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettings(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val LAST_OPENED_DATABASE_PATH = stringPreferencesKey("last_opened_database_path")
        val AUTO_LOCK_TIMEOUT_SECONDS = intPreferencesKey("auto_lock_timeout_seconds")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val CLIPBOARD_CLEAR_TIMEOUT_SECONDS = intPreferencesKey("clipboard_clear_timeout_seconds")
    }

    object Defaults {
        const val AUTO_LOCK_TIMEOUT_SECONDS = 300       // 5 minutes
        const val THEME_PREFERENCE = "system"            // "system", "light", "dark"
        const val CLIPBOARD_CLEAR_TIMEOUT_SECONDS = 30   // 30 seconds
    }

    val lastOpenedDatabasePath: Flow<String?>
        get() = dataStore.data.map { it[LAST_OPENED_DATABASE_PATH] }

    val autoLockTimeoutSeconds: Flow<Int>
        get() = dataStore.data.map { it[AUTO_LOCK_TIMEOUT_SECONDS] ?: Defaults.AUTO_LOCK_TIMEOUT_SECONDS }

    val themePreference: Flow<String>
        get() = dataStore.data.map { it[THEME_PREFERENCE] ?: Defaults.THEME_PREFERENCE }

    val clipboardClearTimeoutSeconds: Flow<Int>
        get() = dataStore.data.map { it[CLIPBOARD_CLEAR_TIMEOUT_SECONDS] ?: Defaults.CLIPBOARD_CLEAR_TIMEOUT_SECONDS }

    suspend fun setLastOpenedDatabasePath(path: String?) {
        dataStore.edit { prefs ->
            if (path != null) {
                prefs[LAST_OPENED_DATABASE_PATH] = path
            } else {
                prefs.remove(LAST_OPENED_DATABASE_PATH)
            }
        }
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        dataStore.edit { it[AUTO_LOCK_TIMEOUT_SECONDS] = seconds }
    }

    suspend fun setThemePreference(theme: String) {
        dataStore.edit { it[THEME_PREFERENCE] = theme }
    }

    suspend fun setClipboardClearTimeoutSeconds(seconds: Int) {
        dataStore.edit { it[CLIPBOARD_CLEAR_TIMEOUT_SECONDS] = seconds }
    }
}
