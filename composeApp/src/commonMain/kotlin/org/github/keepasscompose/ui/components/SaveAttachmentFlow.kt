package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxAttachment

sealed interface SaveAttachmentState {
    data object Idle : SaveAttachmentState
    data object PickingDestination : SaveAttachmentState
    data object Saving : SaveAttachmentState
    data class Success(val path: String, val openAfterSave: Boolean) : SaveAttachmentState
    data class Error(val message: String) : SaveAttachmentState
}

@Composable
fun SaveAttachmentDialog(attachment: KdbxAttachment, onSave: (openAfterSave: Boolean) -> Unit, onDismiss: () -> Unit) {
    var openAfterSave by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Attachment") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Save \"${attachment.name}\" to disk.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Size: ${formatFileSize(attachment.data.size.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = openAfterSave, onCheckedChange = { openAfterSave = it })
                    Text("Open after saving", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(openAfterSave) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
