package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.TotpGenerator
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.ui.components.TotpDisplay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    entry: KdbxEntry,
    onCopyField: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onEditEntry: (String) -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = entry.title.ifEmpty { "Untitled" },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onEditEntry(entry.uuid) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit entry")
            }
            IconButton(onClick = { onDeleteEntry(entry.uuid) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Username
        DetailFieldRow(
            label = "Username",
            value = entry.userName,
            onCopy = { onCopyField(entry.userName) },
        )

        // Password
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Password",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (passwordVisible) entry.password else "••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Hide" else "Show",
                )
            }
            IconButton(onClick = { onCopyField(entry.password) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
        }

        // TOTP
        val totpParams = remember(entry) {
            val fieldMap = entry.fields.associate { it.key to it.value }
            TotpGenerator.parseKeePassAttributes(fieldMap)
        }
        if (totpParams != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TotpDisplay(
                params = totpParams,
                onCopy = { code -> onCopyField(code) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // URL
        DetailFieldRow(
            label = "URL",
            value = entry.url,
            onCopy = { onCopyField(entry.url) },
        )

        // Notes
        if (entry.notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Custom fields
        val standardKeys = setOf(
            KdbxEntry.FIELD_TITLE,
            KdbxEntry.FIELD_USER_NAME,
            KdbxEntry.FIELD_PASSWORD,
            KdbxEntry.FIELD_URL,
            KdbxEntry.FIELD_NOTES,
        )
        val customFields = entry.fields.filter { it.key !in standardKeys }
        if (customFields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Custom Fields", style = MaterialTheme.typography.titleSmall)
            customFields.forEach { field ->
                DetailFieldRow(
                    label = field.key,
                    value = if (field.isProtected) "••••••••" else field.value,
                    onCopy = { onCopyField(field.value) },
                )
            }
        }

        // Attachments
        if (entry.attachments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Attachments", style = MaterialTheme.typography.titleSmall)
            entry.attachments.forEach { attachment ->
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        // Tags
        if (entry.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tags", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        // Timestamps
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Timestamps", style = MaterialTheme.typography.titleSmall)
        entry.creationTime?.let { TimestampDetail("Created", it.toString().take(19)) }
        entry.lastModificationTime?.let { TimestampDetail("Modified", it.toString().take(19)) }
        entry.lastAccessTime?.let { TimestampDetail("Accessed", it.toString().take(19)) }
        if (entry.expires) {
            entry.expiryTime?.let { TimestampDetail("Expires", it.toString().take(19)) }
        }
    }
}

@Composable
private fun DetailFieldRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge)
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label")
            }
        }
    }
}

@Composable
private fun TimestampDetail(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.labelSmall)
    }
}
