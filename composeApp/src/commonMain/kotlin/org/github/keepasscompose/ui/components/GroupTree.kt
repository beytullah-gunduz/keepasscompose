package org.github.keepasscompose.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

@Composable
fun GroupTree(
    rootGroup: KdbxGroup,
    selectedEntryUuid: String? = null,
    onEntrySelected: (KdbxEntry) -> Unit = {},
    onEditEntry: (KdbxEntry) -> Unit = {},
    onDeleteEntry: (KdbxEntry) -> Unit = {},
    onCopyField: (String) -> Unit = {},
    onNewEntryInGroup: (KdbxGroup) -> Unit = {},
    onNewSubGroup: (KdbxGroup) -> Unit = {},
    onDeleteGroup: (KdbxGroup) -> Unit = {},
    recycleBinUuid: String? = null,
    modifier: Modifier = Modifier,
) {
    val expandedGroups = remember { mutableStateListOf(rootGroup.uuid) }
    val flattenedItems = remember(rootGroup, expandedGroups.toList()) {
        buildFlatTreeList(rootGroup, expandedGroups.toSet(), depth = 0)
    }

    LazyColumn(modifier = modifier) {
        items(flattenedItems, key = { it.key }) { item ->
            when (item) {
                is TreeItem.Group -> GroupTreeItem(
                    group = item.group,
                    depth = item.depth,
                    isExpanded = item.group.uuid in expandedGroups,
                    hasChildren = item.group.groups.isNotEmpty() || item.group.entries.isNotEmpty(),
                    entryCount = item.group.entries.size,
                    onToggleExpand = {
                        if (item.group.uuid in expandedGroups) {
                            expandedGroups.remove(item.group.uuid)
                        } else {
                            expandedGroups.add(item.group.uuid)
                        }
                    },
                    onNewEntry = { onNewEntryInGroup(item.group) },
                    onNewSubGroup = { onNewSubGroup(item.group) },
                    onDeleteGroup = { onDeleteGroup(item.group) },
                    isRecycleBin = item.group.uuid == recycleBinUuid,
                )

                is TreeItem.Entry -> EntryTreeItem(
                    entry = item.entry,
                    depth = item.depth,
                    isSelected = item.entry.uuid == selectedEntryUuid,
                    onSelect = { onEntrySelected(item.entry) },
                    onEdit = { onEditEntry(item.entry) },
                    onDelete = { onDeleteEntry(item.entry) },
                    onCopyUsername = { onCopyField(item.entry.userName) },
                    onCopyPassword = { onCopyField(item.entry.password) },
                )
            }
        }
    }
}

private sealed class TreeItem {
    abstract val key: String
    abstract val depth: Int

    data class Group(val group: KdbxGroup, override val depth: Int) : TreeItem() {
        override val key: String get() = "g_${group.uuid}"
    }

    data class Entry(val entry: KdbxEntry, override val depth: Int) : TreeItem() {
        override val key: String get() = "e_${entry.uuid}"
    }
}

private fun buildFlatTreeList(group: KdbxGroup, expandedIds: Set<String>, depth: Int): List<TreeItem> = buildList {
    add(TreeItem.Group(group, depth))
    if (group.uuid in expandedIds) {
        for (child in group.groups) {
            addAll(buildFlatTreeList(child, expandedIds, depth + 1))
        }
        for (entry in group.entries) {
            add(TreeItem.Entry(entry, depth + 1))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupTreeItem(
    group: KdbxGroup,
    depth: Int,
    isExpanded: Boolean,
    hasChildren: Boolean,
    entryCount: Int,
    onToggleExpand: () -> Unit,
    onNewEntry: () -> Unit = {},
    onNewSubGroup: () -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    isRecycleBin: Boolean = false,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onToggleExpand,
                    onLongClick = { showContextMenu = true },
                )
                .padding(start = (16 + depth * 20).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            if (hasChildren) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                    Icon(
                        @Suppress("DEPRECATION")
                        if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                if (isExpanded && hasChildren) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (entryCount > 0) {
                Text(
                    text = "$entryCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        GroupContextMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            callbacks = GroupContextMenuCallbacks(
                onNewEntry = onNewEntry,
                onNewSubGroup = onNewSubGroup,
                onDelete = onDeleteGroup,
            ),
            isRecycleBin = isRecycleBin,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryTreeItem(
    entry: KdbxEntry,
    depth: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopyUsername: () -> Unit = {},
    onCopyPassword: () -> Unit = {},
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = { showContextMenu = true },
                )
                .padding(start = (16 + depth * 20 + 28).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                Icons.Filled.Key,
                contentDescription = null,
                tint = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = entry.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
                if (entry.userName.isNotEmpty()) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        EntryContextMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            callbacks = EntryContextMenuCallbacks(
                onCopyUsername = onCopyUsername,
                onCopyPassword = onCopyPassword,
                onEdit = onEdit,
                onDelete = onDelete,
            ),
        )
    }
}
