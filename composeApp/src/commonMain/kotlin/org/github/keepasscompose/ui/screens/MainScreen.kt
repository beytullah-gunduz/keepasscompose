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
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    databaseName: String = "",
    entryCount: Int = 0,
    onNewEntry: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onSaveDatabase: () -> Unit = {},
    onLockDatabase: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(databaseName.ifEmpty { "KeePass Compose" }) },
                actions = {
                    IconButton(onClick = onNewEntry) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New Entry")
                    }
                    IconButton(onClick = onOpenDatabase) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open Database")
                    }
                    IconButton(onClick = onSaveDatabase) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Database")
                    }
                    IconButton(onClick = onLockDatabase) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock Database")
                    }
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
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

@Composable
private fun GroupTreeSidebar(modifier: Modifier = Modifier) {
    val placeholderGroups = listOf("Root", "General", "Email", "Internet", "Banking")
    LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        items(placeholderGroups) { group ->
            Text(
                text = group,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
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
