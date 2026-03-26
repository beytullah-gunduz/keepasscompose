package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxAttachment

@Composable
fun AttachmentList(
    attachments: List<KdbxAttachment>,
    onPreview: (KdbxAttachment) -> Unit = {},
    onSave: (KdbxAttachment) -> Unit = {},
    onDelete: (KdbxAttachment) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (attachments.isEmpty()) {
            Text(
                text = "No attachments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            attachments.forEach { attachment ->
                AttachmentItem(
                    attachment = attachment,
                    onPreview = { onPreview(attachment) },
                    onSave = { onSave(attachment) },
                    onDelete = { onDelete(attachment) },
                )
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: KdbxAttachment,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = getFileIcon(attachment.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatFileSize(attachment.data.size.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isPreviewable(attachment.name)) {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Filled.Visibility, contentDescription = "Preview")
                }
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Save, contentDescription = "Save")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun getFileIcon(name: String): ImageVector {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "png", "jpg", "jpeg", "gif", "svg", "bmp", "webp" -> Icons.Filled.Image
        "txt", "md", "csv", "log", "json", "xml" -> Icons.Filled.TextSnippet
        else -> Icons.Filled.InsertDriveFile
    }
}

internal fun isPreviewable(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("png", "jpg", "jpeg", "gif", "svg", "bmp", "webp", "txt", "md", "csv", "log", "json", "xml")
}

internal fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}
