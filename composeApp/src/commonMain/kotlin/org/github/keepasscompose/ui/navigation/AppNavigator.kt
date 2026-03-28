package org.github.keepasscompose.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.PasswordHealthAnalyzer
import org.github.keepasscompose.core.common.SearchEngine
import org.github.keepasscompose.core.database.BitwardenImporter
import org.github.keepasscompose.core.database.CsvExporter
import org.github.keepasscompose.core.database.CsvImporter
import org.github.keepasscompose.core.database.HtmlExporter
import org.github.keepasscompose.core.database.LastPassImporter
import org.github.keepasscompose.core.database.OnePasswordImporter
import org.github.keepasscompose.core.database.RecycleBinManager
import org.github.keepasscompose.core.database.XmlExporter
import org.github.keepasscompose.ui.screens.AppSettingsScreen
import org.github.keepasscompose.ui.screens.ChangeKeyFileScreen
import org.github.keepasscompose.ui.screens.ChangePasswordScreen
import org.github.keepasscompose.ui.screens.CreateDatabaseScreen
import org.github.keepasscompose.ui.screens.DatabaseSettingsScreen
import org.github.keepasscompose.ui.screens.EntryEditorScreen
import org.github.keepasscompose.ui.screens.EntryHistoryScreen
import org.github.keepasscompose.ui.screens.ImportExportScreen
import org.github.keepasscompose.ui.screens.ImportExportState
import org.github.keepasscompose.ui.screens.MainScreen
import org.github.keepasscompose.ui.screens.RecentDatabase
import org.github.keepasscompose.ui.screens.ReportsScreen
import org.github.keepasscompose.ui.screens.SearchResultsScreen
import org.github.keepasscompose.ui.screens.UnlockScreen
import org.github.keepasscompose.ui.screens.WelcomeScreen
import org.github.keepasscompose.viewmodel.DatabaseViewModel
import org.github.keepasscompose.viewmodel.EntryEditorViewModel
import org.github.keepasscompose.viewmodel.GroupNavigationViewModel
import org.github.keepasscompose.viewmodel.UnlockState
import org.github.keepasscompose.viewmodel.UnlockViewModel
import org.koin.compose.koinInject
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

private fun AppScreen.depth(): Int = when (this) {
    is AppScreen.Welcome -> 0
    is AppScreen.CreateDatabase -> 1
    is AppScreen.Unlock -> 1
    is AppScreen.Main -> 2
    is AppScreen.Search -> 3
    is AppScreen.AppSettings -> 3
    is AppScreen.DatabaseSettings -> 3
    is AppScreen.ImportExport -> 3
    is AppScreen.Reports -> 3
    is AppScreen.ChangePassword -> 3
    is AppScreen.ChangeKeyFile -> 3
    is AppScreen.EntryEditor -> 3
    is AppScreen.EntryHistory -> 3
    is AppScreen.KdfConfig -> 3
    is AppScreen.TotpSetup -> 3
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

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AppNavigator() {
    val unlockViewModel: UnlockViewModel = koinInject()
    val databaseViewModel: DatabaseViewModel = koinInject()
    val groupNavViewModel: GroupNavigationViewModel = koinInject()
    val entryEditorViewModel: EntryEditorViewModel = koinInject()
    val appSettings: AppSettings = koinInject()
    val fileSystem: FileSystem = koinInject()
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

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val forward = targetState.depth() >= initialState.depth()
            val offsetFraction = { fullWidth: Int -> fullWidth / 4 }
            if (forward) {
                (slideInHorizontally { offsetFraction(it) } + fadeIn()) togetherWith
                    (slideOutHorizontally { -offsetFraction(it) } + fadeOut())
            } else {
                (slideInHorizontally { -offsetFraction(it) } + fadeIn()) togetherWith
                    (slideOutHorizontally { offsetFraction(it) } + fadeOut())
            } using SizeTransform(clip = false)
        },
        label = "screen_transition",
    ) { screen ->
    when (screen) {
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
                onNewDatabase = { currentScreen = AppScreen.CreateDatabase },
                onRecentDatabaseSelected = { db ->
                    currentScreen = AppScreen.Unlock(db.path)
                },
                onRemoveRecentDatabase = { db ->
                    scope.launch { appSettings.removeRecentDatabase(db.path) }
                },
            )
        }

        is AppScreen.Unlock -> {
            val unlockScreen = screen as AppScreen.Unlock
            val keyFilePath by unlockViewModel.keyFilePath.collectAsState()

            UnlockScreen(
                databasePath = unlockScreen.databasePath,
                keyFilePath = keyFilePath,
                isLoading = unlockState is UnlockState.Loading,
                errorMessage = (unlockState as? UnlockState.Error)?.message,
                onPasswordSubmit = { password -> unlockViewModel.unlock(unlockScreen.databasePath, password) },
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
            val dbPath by databaseViewModel.filePath.collectAsState()
            val databaseKey = dbPath?.let { appSettings.databaseKeyFromPath(it) } ?: ""
            val persistedExpandedGroups by appSettings.expandedGroupsForDatabase(databaseKey)
                .collectAsState(initial = null)

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
                onDeleteEntry = { uuid ->
                    databaseViewModel.deleteEntry(uuid)
                    database?.rootGroup?.let { groupNavViewModel.setRootGroup(it) }
                },
                onCreateGroup = { parentUuid, result ->
                    val newGroup = org.github.keepasscompose.core.model.KdbxGroup(
                        uuid = Base64.encode(Uuid.random().toByteArray()),
                        name = result.name,
                        icon = org.github.keepasscompose.core.model.KdbxIcon(standardIndex = result.iconIndex),
                        notes = result.notes,
                    )
                    databaseViewModel.addGroup(parentUuid, newGroup)
                    databaseViewModel.database.value?.rootGroup?.let { groupNavViewModel.setRootGroup(it) }
                },
                onDeleteGroup = { uuid ->
                    databaseViewModel.deleteGroup(uuid)
                    databaseViewModel.database.value?.rootGroup?.let { groupNavViewModel.setRootGroup(it) }
                },
                onSearch = { currentScreen = AppScreen.Search },
                onOpenSettings = { currentScreen = AppScreen.AppSettings },
                onOpenDatabaseSettings = { currentScreen = AppScreen.DatabaseSettings },
                onOpenImportExport = { currentScreen = AppScreen.ImportExport },
                onOpenReports = { currentScreen = AppScreen.Reports },
                onChangePassword = { currentScreen = AppScreen.ChangePassword },
                onChangeKeyFile = { currentScreen = AppScreen.ChangeKeyFile },
                hasRecycleBin = database?.meta?.recycleBinEnabled == true,
                recycleBinUuid = database?.meta?.recycleBinUuid,
                initialExpandedGroups = persistedExpandedGroups,
                onExpandedGroupsChanged = { expandedUuids ->
                    if (databaseKey.isNotEmpty()) {
                        scope.launch {
                            appSettings.setExpandedGroupsForDatabase(databaseKey, expandedUuids)
                        }
                    }
                },
            )
        }

        is AppScreen.EntryEditor -> {
            val editorScreen = screen as AppScreen.EntryEditor
            val database by databaseViewModel.database.collectAsState()
            val existingEntry = if (editorScreen.isEditMode && editorScreen.entryUuid != null) {
                database?.rootGroup?.let { RecycleBinManager.findEntry(it, editorScreen.entryUuid) }
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
                isEditMode = editorScreen.isEditMode,
                onSave = { result ->
                    val saved = entryEditorViewModel.save(result)
                    if (saved != null) {
                        val selectedGroup = groupNavViewModel.selectedGroup.value
                        val parentUuid = selectedGroup?.uuid ?: database?.rootGroup?.uuid ?: return@EntryEditorScreen
                        if (editorScreen.isEditMode) {
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
            CreateDatabaseScreen(
                onSelectLocation = {
                    scope.launch {
                        filePicker.pickSaveLocation("NewDatabase.kdbx", listOf("kdbx"))
                    }
                },
                onCancel = { currentScreen = AppScreen.Welcome },
                onCreate = { currentScreen = AppScreen.Welcome },
            )
        }

        is AppScreen.Search -> {
            val database by databaseViewModel.database.collectAsState()
            var searchQuery by remember { mutableStateOf("") }
            val results = remember(searchQuery, database) {
                if (searchQuery.length >= 2 && database?.rootGroup != null) {
                    SearchEngine.search(database!!.rootGroup, searchQuery)
                } else {
                    emptyList()
                }
            }

            SearchResultsScreen(
                results = results,
                query = searchQuery,
                onBack = { currentScreen = AppScreen.Main },
                onResultClick = { result ->
                    currentScreen = AppScreen.Main
                },
            )
        }

        is AppScreen.AppSettings -> {
            AppSettingsScreen(
                onCancel = { currentScreen = AppScreen.Main },
                onSave = { currentScreen = AppScreen.Main },
            )
        }

        is AppScreen.DatabaseSettings -> {
            val database by databaseViewModel.database.collectAsState()
            val meta = database?.meta ?: org.github.keepasscompose.core.model.KdbxMeta()

            DatabaseSettingsScreen(
                meta = meta,
                onCancel = { currentScreen = AppScreen.Main },
                onSave = { currentScreen = AppScreen.Main },
            )
        }

        is AppScreen.ImportExport -> {
            val database by databaseViewModel.database.collectAsState()
            var ieState by remember { mutableStateOf<ImportExportState>(ImportExportState.FormatSelection) }
            var selectedFilePath by remember { mutableStateOf<String?>(null) }

            ImportExportScreen(
                state = ieState,
                onBack = {
                    ieState = ImportExportState.FormatSelection
                    currentScreen = AppScreen.Main
                },
                onFormatSelected = { format ->
                    ieState = ImportExportState.FileSelection(format)
                },
                onPickFile = { format ->
                    scope.launch {
                        if (format.isImport) {
                            val extensions = when (format.id) {
                                "csv" -> listOf("csv")
                                "lastpass" -> listOf("csv")
                                "bitwarden" -> listOf("json", "csv")
                                "1password" -> listOf("csv", "1pif")
                                else -> listOf("csv")
                            }
                            val path = filePicker.pickFile(extensions)
                            if (path != null) {
                                selectedFilePath = path
                                try {
                                    val content = fileSystem.readFile(path).decodeToString()
                                    val (entryCount, groupCount) = countImportItems(format.id, content)
                                    ieState = ImportExportState.Preview(format, entryCount, groupCount)
                                } catch (e: Exception) {
                                    ieState = ImportExportState.Failed(format, e.message ?: "Failed to read file")
                                }
                            }
                        } else {
                            val ext = when (format.id) {
                                "csv-export" -> "csv"
                                "html-export" -> "html"
                                "xml-export" -> "xml"
                                else -> "txt"
                            }
                            val rootGroup = database?.rootGroup ?: return@launch
                            val entryCount = countAllEntries(rootGroup)
                            val groupCount = countAllGroups(rootGroup)
                            ieState = ImportExportState.Preview(format, entryCount, groupCount)
                        }
                    }
                },
                onConfirmImport = {
                    val format = (ieState as? ImportExportState.Preview)?.format ?: return@ImportExportScreen
                    val path = selectedFilePath ?: return@ImportExportScreen
                    scope.launch {
                        ieState = ImportExportState.InProgress(format, 0.5f)
                        try {
                            val content = fileSystem.readFile(path).decodeToString()
                            val (entries, groupTree) = executeImport(format.id, content)
                            val rootGroup = database?.rootGroup ?: return@launch
                            val mergedRoot = mergeImportedGroup(rootGroup, groupTree, entries)
                            val db = database ?: return@launch
                            databaseViewModel.setDatabase(db.copy(rootGroup = mergedRoot), databaseViewModel.filePath.value ?: "")
                            databaseViewModel.markDirty()
                            groupNavViewModel.setRootGroup(mergedRoot)
                            ieState = ImportExportState.Complete(format, entries.size, countAllGroups(groupTree))
                        } catch (e: Exception) {
                            ieState = ImportExportState.Failed(format, e.message ?: "Import failed")
                        }
                    }
                },
                onConfirmExport = {
                    val format = (ieState as? ImportExportState.Preview)?.format ?: return@ImportExportScreen
                    scope.launch {
                        ieState = ImportExportState.InProgress(format, 0.5f)
                        try {
                            val rootGroup = database?.rootGroup ?: return@launch
                            val output = executeExport(format.id, rootGroup)
                            val savePath = filePicker.pickSaveLocation(
                                "export.${format.id.removeSuffix("-export")}",
                                listOf(format.id.removeSuffix("-export")),
                            )
                            if (savePath != null) {
                                fileSystem.writeFile(savePath, output.encodeToByteArray())
                                ieState = ImportExportState.Complete(format, countAllEntries(rootGroup), countAllGroups(rootGroup))
                            } else {
                                ieState = ImportExportState.FormatSelection
                            }
                        } catch (e: Exception) {
                            ieState = ImportExportState.Failed(format, e.message ?: "Export failed")
                        }
                    }
                },
            )
        }

        is AppScreen.Reports -> {
            val database by databaseViewModel.database.collectAsState()
            val rootGroup = database?.rootGroup
            val report = remember(rootGroup) {
                if (rootGroup != null) PasswordHealthAnalyzer.analyze(rootGroup) else null
            }

            if (report != null && rootGroup != null) {
                ReportsScreen(
                    report = report,
                    totalGroups = countAllGroups(rootGroup),
                    totalAttachments = 0,
                    onWeakPasswords = { /* navigate to weak passwords sub-screen */ },
                    onReusedPasswords = { /* navigate to reused passwords sub-screen */ },
                    onExpiredEntries = { /* navigate to expired entries sub-screen */ },
                    onHibpReport = { /* navigate to HIBP sub-screen */ },
                    onBack = { currentScreen = AppScreen.Main },
                )
            } else {
                PlaceholderScreen("Reports") { currentScreen = AppScreen.Main }
            }
        }

        is AppScreen.ChangePassword -> {
            ChangePasswordScreen(
                onChangePassword = { _, _ -> currentScreen = AppScreen.Main },
                onCancel = { currentScreen = AppScreen.Main },
            )
        }

        is AppScreen.ChangeKeyFile -> {
            ChangeKeyFileScreen(
                onSelectKeyFile = {
                    scope.launch { filePicker.pickFile(listOf("keyx", "key")) }
                },
                onApply = { currentScreen = AppScreen.Main },
                onCancel = { currentScreen = AppScreen.Main },
            )
        }

        is AppScreen.EntryHistory -> {
            val historyScreen = screen as AppScreen.EntryHistory
            val database by databaseViewModel.database.collectAsState()
            val entry = database?.rootGroup?.let { RecycleBinManager.findEntry(it, historyScreen.entryUuid) }

            if (entry != null) {
                EntryHistoryScreen(
                    entry = entry,
                    onBack = { currentScreen = AppScreen.Main },
                    onRestoreVersion = { historicalEntry ->
                        databaseViewModel.updateEntry(historicalEntry)
                        database?.rootGroup?.let { groupNavViewModel.setRootGroup(it) }
                        currentScreen = AppScreen.Main
                    },
                )
            } else {
                PlaceholderScreen("Entry History") { currentScreen = AppScreen.Main }
            }
        }

        is AppScreen.KdfConfig -> {
            PlaceholderScreen("KDF Configuration") { currentScreen = AppScreen.Main }
        }

        is AppScreen.TotpSetup -> {
            PlaceholderScreen("TOTP Setup") { currentScreen = AppScreen.Main }
        }
    }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title — Coming soon")
    }
}

// -- Import/Export helpers --

private fun countImportItems(formatId: String, content: String): Pair<Int, Int> = when (formatId) {
    "csv" -> {
        val result = CsvImporter.parse(content)
        result.entries.size to countAllGroups(result.groupTree)
    }

    "lastpass" -> {
        val result = LastPassImporter.import(content)
        result.entries.size to countAllGroups(result.groupTree)
    }

    "bitwarden" -> {
        val result = if (content.trimStart().startsWith("{")) {
            BitwardenImporter.import(content)
        } else {
            BitwardenImporter.importCsv(content)
        }
        result.entries.size to countAllGroups(result.groupTree)
    }

    "1password" -> {
        val result = if (content.trimStart().startsWith("***")) {
            OnePasswordImporter.importOnePif(content)
        } else {
            OnePasswordImporter.importCsv(content)
        }
        result.entries.size to countAllGroups(result.groupTree)
    }

    else -> 0 to 0
}

private fun executeImport(
    formatId: String,
    content: String,
): Pair<List<org.github.keepasscompose.core.model.KdbxEntry>, org.github.keepasscompose.core.model.KdbxGroup> = when (formatId) {
    "csv" -> CsvImporter.parse(content).let { it.entries to it.groupTree }

    "lastpass" -> LastPassImporter.import(content).let { it.entries to it.groupTree }

    "bitwarden" -> {
        val r = if (content.trimStart().startsWith("{")) BitwardenImporter.import(content) else BitwardenImporter.importCsv(content)
        r.entries to r.groupTree
    }

    "1password" -> {
        val r = if (content.trimStart().startsWith("***")) OnePasswordImporter.importOnePif(content) else OnePasswordImporter.importCsv(content)
        r.entries to r.groupTree
    }

    else -> emptyList<org.github.keepasscompose.core.model.KdbxEntry>() to
        org.github.keepasscompose.core.model.KdbxGroup(uuid = "", name = "Imported")
}

private fun executeExport(formatId: String, rootGroup: org.github.keepasscompose.core.model.KdbxGroup): String = when (formatId) {
    "csv-export" -> CsvExporter.export(rootGroup)
    "html-export" -> HtmlExporter.export(rootGroup)
    "xml-export" -> XmlExporter.export(rootGroup)
    else -> ""
}

private fun mergeImportedGroup(
    rootGroup: org.github.keepasscompose.core.model.KdbxGroup,
    importedTree: org.github.keepasscompose.core.model.KdbxGroup,
    entries: List<org.github.keepasscompose.core.model.KdbxEntry>,
): org.github.keepasscompose.core.model.KdbxGroup {
    val importedGroups = if (importedTree.groups.isNotEmpty()) importedTree.groups else emptyList()
    val importedEntries = if (importedTree.entries.isNotEmpty()) importedTree.entries else entries
    return rootGroup.copy(
        groups = rootGroup.groups + importedGroups,
        entries = rootGroup.entries + importedEntries,
    )
}

private fun countAllEntries(group: org.github.keepasscompose.core.model.KdbxGroup): Int =
    group.entries.size + group.groups.sumOf { countAllEntries(it) }

private fun countAllGroups(group: org.github.keepasscompose.core.model.KdbxGroup): Int = group.groups.size + group.groups.sumOf { countAllGroups(it) }
