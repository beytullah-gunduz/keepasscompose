package org.github.keepasscompose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxGroup

@Composable
fun GroupTree(rootGroup: KdbxGroup, selectedGroupUuid: String? = null, onGroupSelected: (KdbxGroup) -> Unit = {}, modifier: Modifier = Modifier) {
    val expandedGroups = remember { mutableStateListOf(rootGroup.uuid) }
    val flattenedItems = remember(rootGroup, expandedGroups.toList()) {
        buildFlatGroupList(rootGroup, expandedGroups.toSet(), depth = 0)
    }

    LazyColumn(modifier = modifier) {
        items(flattenedItems, key = { it.group.uuid }) { item ->
            GroupTreeItem(
                group = item.group,
                depth = item.depth,
                isExpanded = item.group.uuid in expandedGroups,
                isSelected = item.group.uuid == selectedGroupUuid,
                hasChildren = item.group.groups.isNotEmpty(),
                entryCount = item.group.entries.size,
                onToggleExpand = {
                    if (item.group.uuid in expandedGroups) {
                        expandedGroups.remove(item.group.uuid)
                    } else {
                        expandedGroups.add(item.group.uuid)
                    }
                },
                onSelect = { onGroupSelected(item.group) },
            )
        }
    }
}

private data class FlatGroupItem(val group: KdbxGroup, val depth: Int)

private fun buildFlatGroupList(group: KdbxGroup, expandedIds: Set<String>, depth: Int): List<FlatGroupItem> = buildList {
    add(FlatGroupItem(group, depth))
    if (group.uuid in expandedIds) {
        for (child in group.groups) {
            addAll(buildFlatGroupList(child, expandedIds, depth + 1))
        }
    }
}

@Composable
private fun GroupTreeItem(
    group: KdbxGroup,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    entryCount: Int,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
) {
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(start = (16 + depth * 20).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    ) {
        // Expand/collapse arrow
        if (hasChildren) {
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Folder icon
        Icon(
            if (isExpanded && hasChildren) Icons.Filled.FolderOpen else Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Group name
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )

        // Entry count badge
        if (entryCount > 0) {
            Text(
                text = "$entryCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
