package org.github.keepasscompose.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.github.keepasscompose.core.model.KdbxGroup

@Composable
fun DeleteGroupDialog(group: KdbxGroup, hasRecycleBin: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val totalEntries = countEntriesRecursive(group)
    val totalSubGroups = countSubGroupsRecursive(group)

    val actionText = if (hasRecycleBin) "moved to the Recycle Bin" else "permanently deleted"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Group") },
        text = {
            Text(
                buildString {
                    append("Are you sure you want to delete \"${group.name}\"?")
                    if (totalEntries > 0 || totalSubGroups > 0) {
                        append("\n\nThis group contains ")
                        val parts = mutableListOf<String>()
                        if (totalSubGroups > 0) {
                            parts.add("$totalSubGroups sub-group${if (totalSubGroups > 1) "s" else ""}")
                        }
                        if (totalEntries > 0) {
                            parts.add("$totalEntries entr${if (totalEntries > 1) "ies" else "y"}")
                        }
                        append(parts.joinToString(" and "))
                        append(".")
                    }
                    append("\n\nAll contents will be $actionText.")
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun countEntriesRecursive(group: KdbxGroup): Int = group.entries.size + group.groups.sumOf { countEntriesRecursive(it) }

private fun countSubGroupsRecursive(group: KdbxGroup): Int = group.groups.size + group.groups.sumOf { countSubGroupsRecursive(it) }
