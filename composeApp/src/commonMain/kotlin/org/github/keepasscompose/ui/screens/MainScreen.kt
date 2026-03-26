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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import org.github.keepasscompose.ui.components.DatabaseTab
import org.github.keepasscompose.ui.components.DatabaseTabBar
import org.github.keepasscompose.ui.components.Toolbar
import org.github.keepasscompose.ui.components.ToolbarCallbacks
import org.github.keepasscompose.ui.shortcuts.keyboardShortcuts

@Composable
fun MainScreen(
    databaseName: String = "",
    entryCount: Int = 0,
    tabs: List<DatabaseTab> = emptyList(),
    selectedTabId: String = "",
    hasSelectedEntry: Boolean = false,
    toolbarCallbacks: ToolbarCallbacks = ToolbarCallbacks(),
    onTabSelected: (String) -> Unit = {},
    onTabClosed: (String) -> Unit = {},
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompact = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    if (isCompact) {
        MobileMainScreen(
            databaseName = databaseName,
            onNewEntry = toolbarCallbacks.onNewEntry,
            onOpenDatabase = toolbarCallbacks.onOpenDatabase,
            onLockDatabase = toolbarCallbacks.onLockDatabase,
            onSearch = toolbarCallbacks.onSearch,
        )
    } else {
        DesktopMainScreen(
            databaseName = databaseName,
            entryCount = entryCount,
            tabs = tabs,
            selectedTabId = selectedTabId,
            hasSelectedEntry = hasSelectedEntry,
            toolbarCallbacks = toolbarCallbacks,
            onTabSelected = onTabSelected,
            onTabClosed = onTabClosed,
        )
    }
}

// ---------------------------------------------------------------------------
// Desktop layout — split pane with sidebar, top bar, and status bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainScreen(
    databaseName: String,
    entryCount: Int,
    tabs: List<DatabaseTab>,
    selectedTabId: String,
    hasSelectedEntry: Boolean,
    toolbarCallbacks: ToolbarCallbacks,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.keyboardShortcuts(
            callbacks = toolbarCallbacks,
            hasSelectedEntry = hasSelectedEntry,
        ),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(databaseName.ifEmpty { "KeePass Compose" }) },
                    actions = {
                        Toolbar(
                            callbacks = toolbarCallbacks,
                            hasSelectedEntry = hasSelectedEntry,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                if (tabs.isNotEmpty()) {
                    DatabaseTabBar(
                        tabs = tabs,
                        selectedTabId = selectedTabId,
                        onTabSelected = onTabSelected,
                        onTabClosed = onTabClosed,
                        onNewTab = toolbarCallbacks.onOpenDatabase,
                    )
                }
            }
        },
        bottomBar = {
            StatusBar(databaseName = databaseName, entryCount = entryCount)
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GroupTreeSidebar(modifier = Modifier.width(260.dp).fillMaxHeight())
            VerticalDivider()
            EntryListPane(modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

// ---------------------------------------------------------------------------
// Mobile layout — navigation drawer, single pane, FAB, pull-to-refresh
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileMainScreen(
    databaseName: String,
    onNewEntry: () -> Unit,
    onOpenDatabase: () -> Unit,
    onLockDatabase: () -> Unit,
    onSearch: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedGroup by remember { mutableStateOf("Root") }

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
                    modifier = Modifier.fillMaxWidth(),
                    onGroupSelected = { group ->
                        selectedGroup = group
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedGroup) },
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
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNewEntry) {
                    Icon(Icons.Filled.Add, contentDescription = "New Entry")
                }
            },
        ) { padding ->
            var isRefreshing by remember { mutableStateOf(false) }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    // Placeholder: actual refresh logic will be wired later
                    isRefreshing = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EntryListPane(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun GroupTreeSidebar(
    modifier: Modifier = Modifier,
    onGroupSelected: (String) -> Unit = {},
) {
    val placeholderGroups = listOf("Root", "General", "Email", "Internet", "Banking")
    LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        items(placeholderGroups) { group ->
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
private fun EntryListPane(modifier: Modifier = Modifier) {
    val placeholderEntries = listOf("Entry 1", "Entry 2", "Entry 3")
    if (placeholderEntries.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No entries",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(placeholderEntries) { entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
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
