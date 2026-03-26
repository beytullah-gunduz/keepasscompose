package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val LARGE_FILE_WARNING_BYTES = 10 * 1024 * 1024L // 10 MB

sealed interface AddAttachmentState {
    data object Idle : AddAttachmentState
    data object PickingFile : AddAttachmentState
    data class Reading(val fileName: String) : AddAttachmentState
    data class SizeWarning(val fileName: String, val sizeBytes: Long) : AddAttachmentState
    data class Success(val fileName: String) : AddAttachmentState
    data class Error(val message: String) : AddAttachmentState
}

fun shouldWarnAboutSize(sizeBytes: Long): Boolean = sizeBytes > LARGE_FILE_WARNING_BYTES

@Composable
fun AddAttachmentProgressDialog(fileName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Adding Attachment") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Reading $fileName...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
fun AttachmentSizeWarningDialog(fileName: String, sizeBytes: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Large File Warning") },
        text = {
            Text(
                "\"$fileName\" is ${formatFileSize(sizeBytes)}. " +
                    "Large attachments increase the database file size significantly. " +
                    "Are you sure you want to add this file?",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Add Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
