package org.github.keepasscompose.core.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AppSettings(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val LAST_OPENED_DATABASE_PATH = stringPreferencesKey("last_opened_database_path")
        val AUTO_LOCK_TIMEOUT_SECONDS = intPreferencesKey("auto_lock_timeout_seconds")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val CLIPBOARD_CLEAR_TIMEOUT_SECONDS = intPreferencesKey("clipboard_clear_timeout_seconds")
        val RECENT_DATABASES = stringPreferencesKey("recent_databases")
        const val MAX_RECENT_DATABASES = 10
    }

    @Serializable
    data class RecentDatabaseEntry(val path: String, val name: String, val lastOpened: String)

    object Defaults {
        const val AUTO_LOCK_TIMEOUT_SECONDS = 300 // 5 minutes
        const val THEME_PREFERENCE = "system" // "system", "light", "dark"
        const val CLIPBOARD_CLEAR_TIMEOUT_SECONDS = 12 // 12 seconds
    }

    @OptIn(ExperimentalEncodingApi::class)
    val lastOpenedDatabasePath: Flow<String?>
        get() = dataStore.data.map { prefs ->
            prefs[LAST_OPENED_DATABASE_PATH]?.let { encoded ->
                try {
                    Base64.decode(encoded).decodeToString()
                } catch (_: IllegalArgumentException) {
                    encoded // Fallback: read plaintext from pre-migration data
                }
            }
        }

    val autoLockTimeoutSeconds: Flow<Int>
        get() = dataStore.data.map { it[AUTO_LOCK_TIMEOUT_SECONDS] ?: Defaults.AUTO_LOCK_TIMEOUT_SECONDS }

    val themePreference: Flow<String>
        get() = dataStore.data.map { it[THEME_PREFERENCE] ?: Defaults.THEME_PREFERENCE }

    val clipboardClearTimeoutSeconds: Flow<Int>
        get() = dataStore.data.map { it[CLIPBOARD_CLEAR_TIMEOUT_SECONDS] ?: Defaults.CLIPBOARD_CLEAR_TIMEOUT_SECONDS }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun setLastOpenedDatabasePath(path: String?) {
        dataStore.edit { prefs ->
            if (path != null) {
                prefs[LAST_OPENED_DATABASE_PATH] = Base64.encode(path.encodeToByteArray())
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

    val recentDatabases: Flow<List<RecentDatabaseEntry>>
        get() = dataStore.data.map { prefs ->
            prefs[RECENT_DATABASES]?.let { json ->
                try {
                    Json.decodeFromString<List<RecentDatabaseEntry>>(json)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun addRecentDatabase(path: String, name: String, timestamp: String) {
        val current = recentDatabases.first().toMutableList()
        current.removeAll { it.path == path }
        current.add(0, RecentDatabaseEntry(path = path, name = name, lastOpened = timestamp))
        if (current.size > MAX_RECENT_DATABASES) {
            current.subList(MAX_RECENT_DATABASES, current.size).clear()
        }
        dataStore.edit { it[RECENT_DATABASES] = Json.encodeToString(current.toList()) }
    }

    suspend fun removeRecentDatabase(path: String) {
        val current = recentDatabases.first().toMutableList()
        current.removeAll { it.path == path }
        dataStore.edit { it[RECENT_DATABASES] = Json.encodeToString(current.toList()) }
    }
}
