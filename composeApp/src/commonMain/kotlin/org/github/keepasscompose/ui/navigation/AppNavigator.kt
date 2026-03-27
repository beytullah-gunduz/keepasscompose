package org.github.keepasscompose.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.github.keepasscompose.core.common.AppSettings
import org.github.keepasscompose.core.common.FilePicker
import org.github.keepasscompose.core.database.RecycleBinManager
import org.github.keepasscompose.ui.screens.EntryEditorScreen
import org.github.keepasscompose.ui.screens.MainScreen
import org.github.keepasscompose.ui.screens.RecentDatabase
import org.github.keepasscompose.ui.screens.UnlockScreen
import org.github.keepasscompose.ui.screens.WelcomeScreen
import org.github.keepasscompose.viewmodel.DatabaseViewModel
import org.github.keepasscompose.viewmodel.EntryEditorViewModel
import org.github.keepasscompose.viewmodel.GroupNavigationViewModel
import org.github.keepasscompose.viewmodel.UnlockState
import org.github.keepasscompose.viewmodel.UnlockViewModel
import org.koin.compose.koinInject

private sealed interface AppScreen {
    data object Welcome : AppScreen
    data class Unlock(val databasePath: String) : AppScreen
    data object Main : AppScreen
    data class EntryEditor(val isEditMode: Boolean, val entryUuid: String?) : AppScreen
    data object CreateDatabase : AppScreen
    data object Search : AppScreen
    data object AppSettings : AppScreen
    data object DatabaseSettings : AppScreen
    data object ImportExport : AppScreen
    data object Reports : AppScreen
    data object ChangePassword : AppScreen
    data object ChangeKeyFile : AppScreen
    data class EntryHistory(val entryUuid: String) : AppScreen
    data object KdfConfig : AppScreen
    data object TotpSetup : AppScreen
}

private val AppScreenSaver = Saver<AppScreen, List<String>>(
    save = { screen ->
        when (screen) {
            is AppScreen.Welcome -> listOf("welcome")
            is AppScreen.Unlock -> listOf("unlock", screen.databasePath)
            is AppScreen.Main -> listOf("main")
            is AppScreen.EntryEditor -> listOf("entry_editor", screen.isEditMode.toString(), screen.entryUuid ?: "")
            is AppScreen.CreateDatabase -> listOf("create_database")
            is AppScreen.Search -> listOf("search")
            is AppScreen.AppSettings -> listOf("app_settings")
            is AppScreen.DatabaseSettings -> listOf("database_settings")
            is AppScreen.ImportExport -> listOf("import_export")
            is AppScreen.Reports -> listOf("reports")
            is AppScreen.ChangePassword -> listOf("change_password")
            is AppScreen.ChangeKeyFile -> listOf("change_key_file")
            is AppScreen.EntryHistory -> listOf("entry_history", screen.entryUuid)
            is AppScreen.KdfConfig -> listOf("kdf_config")
            is AppScreen.TotpSetup -> listOf("totp_setup")
        }
    },
    restore = { list ->
        when (list[0]) {
            "unlock" -> AppScreen.Unlock(list[1])
            "main" -> AppScreen.Main
            "entry_editor" -> AppScreen.EntryEditor(list[1].toBoolean(), list[2].ifEmpty { null })
            "create_database" -> AppScreen.CreateDatabase
            "search" -> AppScreen.Search
            "app_settings" -> AppScreen.AppSettings
            "database_settings" -> AppScreen.DatabaseSettings
            "import_export" -> AppScreen.ImportExport
            "reports" -> AppScreen.Reports
            "change_password" -> AppScreen.ChangePassword
            "change_key_file" -> AppScreen.ChangeKeyFile
            "entry_history" -> AppScreen.EntryHistory(list[1])
            "kdf_config" -> AppScreen.KdfConfig
            "totp_setup" -> AppScreen.TotpSetup
            else -> AppScreen.Welcome
        }
    },
)

@Composable
fun AppNavigator() {
    val unlockViewModel: UnlockViewModel = koinInject()
    val databaseViewModel: DatabaseViewModel = koinInject()
    val groupNavViewModel: GroupNavigationViewModel = koinInject()
    val entryEditorViewModel: EntryEditorViewModel = koinInject()
    val appSettings: AppSettings = koinInject()
    val filePicker = remember { FilePicker() }
    val scope = rememberCoroutineScope()

    var currentScreen: AppScreen by rememberSaveable(stateSaver = AppScreenSaver) { mutableStateOf(AppScreen.Welcome) }
    val unlockState by unlockViewModel.state.collectAsState()
    val recentEntries by appSettings.recentDatabases.collectAsState(initial = emptyList())

    // React to successful unlock
    when (val state = unlockState) {
        is UnlockState.Success -> {
            val path = (currentScreen as? AppScreen.Unlock)?.databasePath ?: ""
            val dbName = state.database.meta.databaseName.ifBlank {
                path.substringAfterLast('/').substringBeforeLast('.')
            }
            databaseViewModel.setDatabase(state.database, path)
            groupNavViewModel.setRootGroup(state.database.rootGroup)
            unlockViewModel.resetState()
            currentScreen = AppScreen.Main
            scope.launch {
                val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val timestamp = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} " +
                    "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
                appSettings.addRecentDatabase(path, dbName, timestamp)
            }
        }

        else -> {}
    }

    when (currentScreen) {
        is AppScreen.Welcome -> {
            WelcomeScreen(
                recentDatabases = recentEntries.map { RecentDatabase(it.name, it.path, it.lastOpened) },
                onOpenDatabase = {
                    scope.launch {
                        val path = filePicker.pickFile(listOf("kdbx"))
                        if (path != null) {
                            currentScreen = AppScreen.Unlock(path)
                        }
                    }
                },
                onNewDatabase = {},
                onRecentDatabaseSelected = { db ->
                    currentScreen = AppScreen.Unlock(db.path)
                },
                onRemoveRecentDatabase = { db ->
                    scope.launch { appSettings.removeRecentDatabase(db.path) }
                },
            )
        }

        is AppScreen.Unlock -> {
            val screen = currentScreen as AppScreen.Unlock
            val keyFilePath by unlockViewModel.keyFilePath.collectAsState()

            UnlockScreen(
                databasePath = screen.databasePath,
                keyFilePath = keyFilePath,
                isLoading = unlockState is UnlockState.Loading,
                errorMessage = (unlockState as? UnlockState.Error)?.message,
                onPasswordSubmit = { password -> unlockViewModel.unlock(screen.databasePath, password) },
                onSelectKeyFile = {
                    scope.launch {
                        val path = filePicker.pickFile(listOf("keyx", "key"))
                        if (path != null) unlockViewModel.setKeyFilePath(path)
                    }
                },
                onClearKeyFile = { unlockViewModel.clearKeyFile() },
            )
        }

        is AppScreen.Main -> {
            val database by databaseViewModel.database.collectAsState()

            MainScreen(
                databaseName = database?.meta?.databaseName ?: "Database",
                rootGroup = database?.rootGroup,
                onLockDatabase = {
                    databaseViewModel.lock()
                    unlockViewModel.resetState()
                    currentScreen = AppScreen.Welcome
                },
                onOpenDatabase = {
                    scope.launch {
                        val path = filePicker.pickFile(listOf("kdbx"))
                        if (path != null) {
                            databaseViewModel.close()
                            currentScreen = AppScreen.Unlock(path)
                        }
                    }
                },
                onNewEntry = {
                    entryEditorViewModel.startCreating()
                    currentScreen = AppScreen.EntryEditor(isEditMode = false, entryUuid = null)
                },
                onEditEntry = { uuid ->
                    val entry = database?.rootGroup?.let { RecycleBinManager.findEntry(it, uuid) }
                    if (entry != null) {
                        entryEditorViewModel.startEditing(entry)
                        currentScreen = AppScreen.EntryEditor(isEditMode = true, entryUuid = uuid)
                    }
                },
            )
        }

        is AppScreen.EntryEditor -> {
            val screen = currentScreen as AppScreen.EntryEditor
            val database by databaseViewModel.database.collectAsState()
            val existingEntry = if (screen.isEditMode && screen.entryUuid != null) {
                database?.rootGroup?.let { RecycleBinManager.findEntry(it, screen.entryUuid) }
            } else {
                null
            }

            EntryEditorScreen(
                initialTitle = existingEntry?.title ?: "",
                initialUserName = existingEntry?.userName ?: "",
                initialPassword = existingEntry?.password ?: "",
                initialUrl = existingEntry?.url ?: "",
                initialNotes = existingEntry?.notes ?: "",
                initialIconIndex = existingEntry?.icon?.standardIndex ?: 0,
                initialTags = existingEntry?.tags ?: emptyList(),
                isEditMode = screen.isEditMode,
                onSave = { result ->
                    val saved = entryEditorViewModel.save(result)
                    if (saved != null) {
                        val selectedGroup = groupNavViewModel.selectedGroup.value
                        val parentUuid = selectedGroup?.uuid ?: database?.rootGroup?.uuid ?: return@EntryEditorScreen
                        if (screen.isEditMode) {
                            databaseViewModel.updateEntry(saved)
                        } else {
                            databaseViewModel.addEntry(parentUuid, saved)
                        }
                        // Refresh group navigation with updated root
                        databaseViewModel.database.value?.rootGroup?.let {
                            groupNavViewModel.setRootGroup(it)
                        }
                        entryEditorViewModel.resetState()
                        currentScreen = AppScreen.Main
                    }
                },
                onCancel = {
                    entryEditorViewModel.resetState()
                    currentScreen = AppScreen.Main
                },
            )
        }

        is AppScreen.CreateDatabase -> {
            PlaceholderScreen("Create Database") { currentScreen = AppScreen.Welcome }
        }

        is AppScreen.Search -> {
            PlaceholderScreen("Search") { currentScreen = AppScreen.Main }
        }

        is AppScreen.AppSettings -> {
            PlaceholderScreen("App Settings") { currentScreen = AppScreen.Main }
        }

        is AppScreen.DatabaseSettings -> {
            PlaceholderScreen("Database Settings") { currentScreen = AppScreen.Main }
        }

        is AppScreen.ImportExport -> {
            PlaceholderScreen("Import / Export") { currentScreen = AppScreen.Main }
        }

        is AppScreen.Reports -> {
            PlaceholderScreen("Reports") { currentScreen = AppScreen.Main }
        }

        is AppScreen.ChangePassword -> {
            PlaceholderScreen("Change Password") { currentScreen = AppScreen.Main }
        }

        is AppScreen.ChangeKeyFile -> {
            PlaceholderScreen("Change Key File") { currentScreen = AppScreen.Main }
        }

        is AppScreen.EntryHistory -> {
            PlaceholderScreen("Entry History") { currentScreen = AppScreen.Main }
        }

        is AppScreen.KdfConfig -> {
            PlaceholderScreen("KDF Configuration") { currentScreen = AppScreen.Main }
        }

        is AppScreen.TotpSetup -> {
            PlaceholderScreen("TOTP Setup") { currentScreen = AppScreen.Main }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title — Coming soon")
    }
}
