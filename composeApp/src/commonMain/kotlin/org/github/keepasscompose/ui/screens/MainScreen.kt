package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.ui.common.BackHandler
import org.github.keepasscompose.ui.components.CreateGroupDialog
import org.github.keepasscompose.ui.components.CreateGroupResult
import org.github.keepasscompose.ui.components.DeleteEntryDialog
import org.github.keepasscompose.ui.components.DeleteGroupDialog
import org.github.keepasscompose.ui.components.GroupTree

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    databaseName: String = "",
    rootGroup: KdbxGroup? = null,
    onCopyField: (String) -> Unit = {},
    onLockDatabase: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onNewEntry: () -> Unit = {},
    onEditEntry: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDatabaseSettings: () -> Unit = {},
    onOpenImportExport: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onChangeKeyFile: () -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onCreateGroup: (parentGroupUuid: String, CreateGroupResult) -> Unit = { _, _ -> },
    onDeleteGroup: (String) -> Unit = {},
    hasRecycleBin: Boolean = false,
    recycleBinUuid: String? = null,
    initialExpandedGroups: Set<String>? = null,
    onExpandedGroupsChanged: (Set<String>) -> Unit = {},
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val selectedEntry = rootGroup?.let { findEntry(it, navigator.currentDestination?.contentKey) }
    var entryPendingDelete by remember { mutableStateOf<KdbxEntry?>(null) }
    var groupPendingDelete by remember { mutableStateOf<KdbxGroup?>(null) }
    var groupForNewSubGroup by remember { mutableStateOf<KdbxGroup?>(null) }

    entryPendingDelete?.let { entry ->
        DeleteEntryDialog(
            entries = listOf(entry),
            hasRecycleBin = hasRecycleBin,
            onDismiss = { entryPendingDelete = null },
            onConfirm = {
                onDeleteEntry(entry.uuid)
                entryPendingDelete = null
                scope.launch { navigator.navigateBack() }
            },
        )
    }

    groupPendingDelete?.let { group ->
        DeleteGroupDialog(
            group = group,
            hasRecycleBin = hasRecycleBin,
            onDismiss = { groupPendingDelete = null },
            onConfirm = {
                onDeleteGroup(group.uuid)
                groupPendingDelete = null
            },
        )
    }

    groupForNewSubGroup?.let { parentGroup ->
        CreateGroupDialog(
            parentGroupName = parentGroup.name,
            onDismiss = { groupForNewSubGroup = null },
            onCreate = { result ->
                onCreateGroup(parentGroup.uuid, result)
                groupForNewSubGroup = null
            },
        )
    }

    val isListHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val isDetailHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden

    BackHandler(navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text(databaseName.ifEmpty { "KeePass Compose" }) },
                                actions = {
                                    IconButton(onClick = onSearch) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                    IconButton(onClick = onLockDatabase) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Lock Database")
                                    }
                                    OverflowMenu(
                                        onOpenSettings = onOpenSettings,
                                        onOpenDatabaseSettings = onOpenDatabaseSettings,
                                        onOpenImportExport = onOpenImportExport,
                                        onOpenReports = onOpenReports,
                                        onChangePassword = onChangePassword,
                                        onChangeKeyFile = onChangeKeyFile,
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            if (!isDetailHidden) {
                                StatusBar(databaseName = databaseName, entryCount = rootGroup?.let { countEntries(it) } ?: 0)
                            }
                        }
                    },
                    floatingActionButton = {
                        if (isDetailHidden) {
                            FloatingActionButton(onClick = onNewEntry) {
                                Icon(Icons.Filled.Add, contentDescription = "New Entry")
                            }
                        }
                    },
                    bottomBar = {
                        if (isDetailHidden) {
                            StatusBar(databaseName = databaseName, entryCount = rootGroup?.let { countEntries(it) } ?: 0)
                        }
                    },
                ) { padding ->
                    if (rootGroup != null) {
                        GroupTree(
                            rootGroup = rootGroup,
                            initialExpandedGroups = initialExpandedGroups,
                            onExpandedGroupsChanged = onExpandedGroupsChanged,
                            selectedEntryUuid = navigator.currentDestination?.contentKey,
                            onEntrySelected = { entry ->
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry.uuid)
                                }
                            },
                            onEditEntry = { entry -> onEditEntry(entry.uuid) },
                            onDeleteEntry = { entry -> entryPendingDelete = entry },
                            onCopyField = onCopyField,
                            onNewEntryInGroup = { onNewEntry() },
                            onNewSubGroup = { group -> groupForNewSubGroup = group },
                            onDeleteGroup = { group -> groupPendingDelete = group },
                            recycleBinUuid = recycleBinUuid,
                            modifier = Modifier.fillMaxSize().padding(padding),
                        )
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val entry = selectedEntry
                    if (entry != null) {
                        Scaffold(
                            topBar = {
                                if (isListHidden) {
                                    TopAppBar(
                                        title = { Text(entry.title.ifEmpty { "Entry" }) },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = { scope.launch { navigator.navigateBack() } },
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }
                            },
                        ) { padding ->
                            EntryDetailScreen(
                                entry = entry,
                                onCopyField = onCopyField,
                                onEditEntry = onEditEntry,
                                onDeleteEntry = { _ -> entryPendingDelete = entry },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Overflow menu
// ---------------------------------------------------------------------------

@Composable
private fun OverflowMenu(
    onOpenSettings: () -> Unit,
    onOpenDatabaseSettings: () -> Unit,
    onOpenImportExport: () -> Unit,
    onOpenReports: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeKeyFile: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Import / Export") },
            onClick = {
                expanded = false
                onOpenImportExport()
            },
        )
        DropdownMenuItem(
            text = { Text("Reports") },
            onClick = {
                expanded = false
                onOpenReports()
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Database Settings") },
            onClick = {
                expanded = false
                onOpenDatabaseSettings()
            },
        )
        DropdownMenuItem(
            text = { Text("Change Password") },
            onClick = {
                expanded = false
                onChangePassword()
            },
        )
        DropdownMenuItem(
            text = { Text("Change Key File") },
            onClick = {
                expanded = false
                onChangeKeyFile()
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                expanded = false
                onOpenSettings()
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun findEntry(group: KdbxGroup, uuid: String?): KdbxEntry? {
    if (uuid == null) return null
    group.entries.find { it.uuid == uuid }?.let { return it }
    for (child in group.groups) {
        findEntry(child, uuid)?.let { return it }
    }
    return null
}

private fun countEntries(group: KdbxGroup): Int = group.entries.size + group.groups.sumOf { countEntries(it) }

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun StatusBar(databaseName: String, entryCount: Int) {
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = databaseName.ifEmpty { "No database open" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$entryCount entries",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
