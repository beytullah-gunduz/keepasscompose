package org.github.keepasscompose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxEntry

enum class SortColumn { TITLE, USERNAME, URL, MODIFIED }
enum class SortDirection { ASC, DESC }

@Composable
fun EntryList(
    entries: List<KdbxEntry>,
    selectedEntryUuid: String? = null,
    onEntrySelected: (KdbxEntry) -> Unit = {},
    onEntryDoubleClick: (KdbxEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var sortColumn by remember { mutableStateOf(SortColumn.TITLE) }
    var sortDirection by remember { mutableStateOf(SortDirection.ASC) }

    val sortedEntries = remember(entries, sortColumn, sortDirection) {
        val comparator: Comparator<KdbxEntry> = when (sortColumn) {
            SortColumn.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortColumn.USERNAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.userName }
            SortColumn.URL -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.url }
            SortColumn.MODIFIED -> compareBy { it.lastModificationTime }
        }
        val sorted = entries.sortedWith(comparator)
        if (sortDirection == SortDirection.DESC) sorted.reversed() else sorted
    }

    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No entries",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(modifier = modifier) {
        // Header row
        item(key = "header") {
            EntryListHeader(
                sortColumn = sortColumn,
                sortDirection = sortDirection,
                onSort = { column ->
                    if (sortColumn == column) {
                        sortDirection = if (sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                    } else {
                        sortColumn = column
                        sortDirection = SortDirection.ASC
                    }
                },
            )
        }

        // Entry rows
        itemsIndexed(sortedEntries, key = { _, entry -> entry.uuid }) { index, entry ->
            val isSelected = entry.uuid == selectedEntryUuid
            val rowBackground = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                index % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerLow
                else -> MaterialTheme.colorScheme.surface
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            EntryListRow(
                entry = entry,
                contentColor = contentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackground)
                    .clickable { onEntrySelected(entry) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun EntryListHeader(
    sortColumn: SortColumn,
    sortDirection: SortDirection,
    onSort: (SortColumn) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // Icon spacer
        Spacer(modifier = Modifier.width(28.dp))

        SortableHeader("Title", SortColumn.TITLE, sortColumn, sortDirection, onSort, Modifier.weight(2f))
        SortableHeader("Username", SortColumn.USERNAME, sortColumn, sortDirection, onSort, Modifier.weight(1.5f))
        SortableHeader("URL", SortColumn.URL, sortColumn, sortDirection, onSort, Modifier.weight(1.5f))
        SortableHeader("Modified", SortColumn.MODIFIED, sortColumn, sortDirection, onSort, Modifier.weight(1f))
    }
}

@Composable
private fun SortableHeader(
    label: String,
    column: SortColumn,
    currentColumn: SortColumn,
    direction: SortDirection,
    onSort: (SortColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onSort(column) }.padding(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (column == currentColumn) {
            Icon(
                if (direction == SortDirection.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EntryListRow(
    entry: KdbxEntry,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            Icons.Filled.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f),
        )
        Text(
            text = entry.userName,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f),
        )
        Text(
            text = entry.url,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f),
        )
        Text(
            text = entry.lastModificationTime?.toString()?.take(10) ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
    }
}
