package org.github.keepasscompose.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.github.keepasscompose.core.common.AppSettings
import org.github.keepasscompose.core.common.FilePicker
import org.github.keepasscompose.ui.screens.MainScreen
import org.github.keepasscompose.ui.screens.RecentDatabase
import org.github.keepasscompose.ui.screens.UnlockScreen
import org.github.keepasscompose.ui.screens.WelcomeScreen
import org.github.keepasscompose.viewmodel.DatabaseViewModel
import org.github.keepasscompose.viewmodel.GroupNavigationViewModel
import org.github.keepasscompose.viewmodel.UnlockState
import org.github.keepasscompose.viewmodel.UnlockViewModel
import org.koin.compose.koinInject

private sealed interface AppScreen {
    data object Welcome : AppScreen
    data class Unlock(val databasePath: String) : AppScreen
    data object Main : AppScreen
}

private val AppScreenSaver = Saver<AppScreen, List<String>>(
    save = { screen ->
        when (screen) {
            is AppScreen.Welcome -> listOf("welcome")
            is AppScreen.Unlock -> listOf("unlock", screen.databasePath)
            is AppScreen.Main -> listOf("main")
        }
    },
    restore = { list ->
        when (list[0]) {
            "unlock" -> AppScreen.Unlock(list[1])
            "main" -> AppScreen.Main
            else -> AppScreen.Welcome
        }
    },
)

@Composable
fun AppNavigator() {
    val unlockViewModel: UnlockViewModel = koinInject()
    val databaseViewModel: DatabaseViewModel = koinInject()
    val groupNavViewModel: GroupNavigationViewModel = koinInject()
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
            )
        }
    }
}

