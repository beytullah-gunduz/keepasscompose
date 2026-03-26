package org.github.keepasscompose.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.github.keepasscompose.core.model.KdbxEntry

@Composable
fun DeleteEntryDialog(
    entries: List<KdbxEntry>,
    hasRecycleBin: Boolean,
    isPermanent: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isBatch = entries.size > 1
    val actionText = when {
        isPermanent -> "permanently deleted"
        hasRecycleBin -> "moved to the Recycle Bin"
        else -> "permanently deleted"
    }

    val titleText = if (isBatch) "Delete ${entries.size} Entries" else "Delete Entry"
    val messageText = if (isBatch) {
        "Are you sure you want to delete ${entries.size} entries?\n\nAll selected entries will be $actionText."
    } else {
        val entryTitle = entries.firstOrNull()?.title ?: "Untitled"
        "Are you sure you want to delete \"$entryTitle\"?\n\nThis entry will be $actionText."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = { Text(messageText) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (isPermanent) "Delete Permanently" else "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
