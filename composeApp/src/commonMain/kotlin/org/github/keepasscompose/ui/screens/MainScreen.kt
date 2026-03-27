package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.ui.common.BackHandler

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    databaseName: String = "",
    selectedGroup: KdbxGroup? = null,
    hasParentGroup: Boolean = false,
    onGroupSelected: (KdbxGroup) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onCopyField: (String) -> Unit = {},
    onLockDatabase: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onNewEntry: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val entries = selectedGroup?.entries ?: emptyList()
    val subGroups = selectedGroup?.groups ?: emptyList()
    val entryCount = entries.size
    val selectedEntry = entries.find { it.uuid == navigator.currentDestination?.contentKey }

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
                                title = { Text(selectedGroup?.name ?: databaseName.ifEmpty { "KeePass Compose" }) },
                                navigationIcon = {
                                    if (hasParentGroup) {
                                        IconButton(onClick = onNavigateUp) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate up")
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(onClick = onSearch) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                    IconButton(onClick = onLockDatabase) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Lock Database")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            if (!isDetailHidden) {
                                StatusBar(databaseName = databaseName, entryCount = entryCount)
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
                            StatusBar(databaseName = databaseName, entryCount = entryCount)
                        }
                    },
                ) { padding ->
                    GroupEntryListPane(
                        subGroups = subGroups,
                        entries = entries,
                        selectedEntryUuid = navigator.currentDestination?.contentKey,
                        onGroupSelected = onGroupSelected,
                        onEntrySelected = { entry ->
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry.uuid)
                            }
                        },
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
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
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun GroupEntryListPane(
    subGroups: List<KdbxGroup>,
    entries: List<KdbxEntry>,
    selectedEntryUuid: String? = null,
    onGroupSelected: (KdbxGroup) -> Unit = {},
    onEntrySelected: (KdbxEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (subGroups.isEmpty() && entries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No entries",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(subGroups, key = { it.uuid }) { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupSelected(group) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val itemCount = group.groups.size + group.entries.size
                        if (itemCount > 0) {
                            Text(
                                text = "$itemCount items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
            items(entries, key = { it.uuid }) { entry ->
                val isSelected = entry.uuid == selectedEntryUuid
                val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .clickable { onEntrySelected(entry) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.Key,
                        contentDescription = null,
                        tint = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = entry.title.ifEmpty { "Untitled" }, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                        if (entry.userName.isNotEmpty()) {
                            Text(
                                text = entry.userName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

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
