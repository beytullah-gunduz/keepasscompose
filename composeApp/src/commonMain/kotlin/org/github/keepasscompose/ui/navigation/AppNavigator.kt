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
import org.github.keepasscompose.core.common.FilePicker
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.ui.screens.MainScreen
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
    val filePicker = remember { FilePicker() }
    val scope = rememberCoroutineScope()

    var currentScreen: AppScreen by rememberSaveable(stateSaver = AppScreenSaver) { mutableStateOf(AppScreen.Welcome) }
    val unlockState by unlockViewModel.state.collectAsState()

    // React to successful unlock
    when (val state = unlockState) {
        is UnlockState.Success -> {
            val path = (currentScreen as? AppScreen.Unlock)?.databasePath ?: ""
            databaseViewModel.setDatabase(state.database, path)
            groupNavViewModel.setRootGroup(state.database.rootGroup)
            unlockViewModel.resetState()
            currentScreen = AppScreen.Main
        }

        else -> {}
    }

    when (currentScreen) {
        is AppScreen.Welcome -> {
            WelcomeScreen(
                onOpenDatabase = {
                    scope.launch {
                        val path = filePicker.pickFile(listOf("kdbx"))
                        if (path != null) {
                            currentScreen = AppScreen.Unlock(path)
                        }
                    }
                },
                onNewDatabase = {},
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
            val selectedGroup by groupNavViewModel.selectedGroup.collectAsState()

            MainScreen(
                databaseName = database?.meta?.databaseName ?: "Database",
                entryCount = selectedGroup?.entries?.size ?: 0,
                groups = buildGroupNames(database?.rootGroup),
                entries = selectedGroup?.entries ?: emptyList(),
                onGroupSelected = { groupName ->
                    val root = database?.rootGroup ?: return@MainScreen
                    findGroup(root, groupName)?.let { groupNavViewModel.navigateTo(it) }
                },
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

private fun buildGroupNames(root: KdbxGroup?): List<String> {
    if (root == null) return emptyList()
    val names = mutableListOf<String>()
    collectGroupNames(root, names)
    return names
}

private fun collectGroupNames(group: KdbxGroup, names: MutableList<String>) {
    names.add(group.name)
    for (child in group.groups) {
        collectGroupNames(child, names)
    }
}

private fun findGroup(group: KdbxGroup, name: String): KdbxGroup? {
    if (group.name == name) return group
    for (child in group.groups) {
        findGroup(child, name)?.let { return it }
    }
    return null
}
