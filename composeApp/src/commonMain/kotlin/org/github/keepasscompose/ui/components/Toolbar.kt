package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ToolbarCallbacks(
    val onNewDatabase: () -> Unit = {},
    val onOpenDatabase: () -> Unit = {},
    val onSaveDatabase: () -> Unit = {},
    val onLockDatabase: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onNewEntry: () -> Unit = {},
    val onEditEntry: () -> Unit = {},
    val onDeleteEntry: () -> Unit = {},
    val onCopyUsername: () -> Unit = {},
    val onCopyPassword: () -> Unit = {},
)

@Composable
fun Toolbar(callbacks: ToolbarCallbacks, hasSelectedEntry: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        // Database actions
        ToolbarButton(Icons.AutoMirrored.Filled.NoteAdd, "New Database", onClick = callbacks.onNewDatabase)
        ToolbarButton(Icons.Filled.FolderOpen, "Open Database", onClick = callbacks.onOpenDatabase)
        ToolbarButton(Icons.Filled.Save, "Save Database", onClick = callbacks.onSaveDatabase)
        ToolbarButton(Icons.Filled.Lock, "Lock Database", onClick = callbacks.onLockDatabase)
        ToolbarButton(Icons.Filled.Search, "Search", onClick = callbacks.onSearch)

        VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp))

        // Entry actions (enabled when an entry is selected)
        ToolbarButton(Icons.AutoMirrored.Filled.NoteAdd, "New Entry", onClick = callbacks.onNewEntry)
        ToolbarButton(Icons.Filled.Edit, "Edit Entry", onClick = callbacks.onEditEntry, enabled = hasSelectedEntry)
        ToolbarButton(
            Icons.Filled.Delete,
            "Delete Entry",
            onClick = callbacks.onDeleteEntry,
            enabled = hasSelectedEntry,
        )

        VerticalDivider(modifier = Modifier.padding(horizontal = 4.dp))

        // Quick copy actions
        ToolbarButton(
            Icons.Filled.Person,
            "Copy Username",
            onClick = callbacks.onCopyUsername,
            enabled = hasSelectedEntry,
        )
        ToolbarButton(Icons.Filled.Key, "Copy Password", onClick = callbacks.onCopyPassword, enabled = hasSelectedEntry)
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(icon: ImageVector, tooltip: String, onClick: () -> Unit, enabled: Boolean = true) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}
