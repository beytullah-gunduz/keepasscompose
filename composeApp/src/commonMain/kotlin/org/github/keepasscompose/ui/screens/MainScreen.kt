package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    databaseName: String = "",
    entryCount: Int = 0,
    groups: List<String> = emptyList(),
    entries: List<KdbxEntry> = emptyList(),
    onGroupSelected: (String) -> Unit = {},
    onCopyField: (String) -> Unit = {},
    onLockDatabase: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onNewEntry: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<KdbxEntry>()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedGroup by remember { mutableStateOf("Root") }

    val isListHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val isDetailHidden = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
                GroupTreeSidebar(
                    groups = groups,
                    modifier = Modifier.fillMaxWidth(),
                    onGroupSelected = { group ->
                        selectedGroup = group
                        onGroupSelected(group)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
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
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
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
                        EntryListPane(
                            entries = entries,
                            selectedEntryUuid = navigator.currentDestination?.contentKey?.uuid,
                            onEntrySelected = { entry ->
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry)
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
                        val entry = navigator.currentDestination?.contentKey
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
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Select an entry",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun GroupTreeSidebar(groups: List<String>, modifier: Modifier = Modifier, onGroupSelected: (String) -> Unit = {}) {
    LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        items(groups) { group ->
            Text(
                text = group,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun EntryListPane(
    entries: List<KdbxEntry>,
    selectedEntryUuid: String? = null,
    onEntrySelected: (KdbxEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No entries",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(entries) { entry ->
                val isSelected = entry.uuid == selectedEntryUuid
                val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .clickable { onEntrySelected(entry) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(text = entry.title.ifEmpty { "Untitled" }, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                    if (entry.userName.isNotEmpty()) {
                        Text(
                            text = entry.userName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
