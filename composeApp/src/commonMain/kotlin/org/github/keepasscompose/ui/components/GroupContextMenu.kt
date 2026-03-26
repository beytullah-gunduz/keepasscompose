package org.github.keepasscompose.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class GroupContextMenuCallbacks(
    val onNewEntry: () -> Unit = {},
    val onNewSubGroup: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onEmptyRecycleBin: () -> Unit = {},
)

@Composable
fun GroupContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    callbacks: GroupContextMenuCallbacks,
    isRecycleBin: Boolean = false,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text("New Entry") },
            onClick = { callbacks.onNewEntry(); onDismissRequest() },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("New Sub-Group") },
            onClick = { callbacks.onNewSubGroup(); onDismissRequest() },
            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Edit Group") },
            onClick = { callbacks.onEdit(); onDismissRequest() },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Delete Group") },
            onClick = { callbacks.onDelete(); onDismissRequest() },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        )
        if (isRecycleBin) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Empty Recycle Bin") },
                onClick = { callbacks.onEmptyRecycleBin(); onDismissRequest() },
                leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
            )
        }
    }
}
