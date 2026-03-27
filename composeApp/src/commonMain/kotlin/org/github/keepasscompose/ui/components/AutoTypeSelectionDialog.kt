package org.github.keepasscompose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A matched entry for auto-type selection.
 */
data class AutoTypeCandidate(
    val entryTitle: String,
    val userName: String,
    val url: String,
    val groupPath: String,
    val sequence: String,
    val entryUuid: String,
)

/**
 * Dialog for selecting which entry to auto-type when multiple entries
 * match the active window title.
 *
 * Shows a list of matching entries with their title, username, and group
 * path. The user selects one entry to perform auto-type with.
 *
 * @param windowTitle The active window title that triggered auto-type.
 * @param candidates List of matching entries.
 * @param onSelect Called when the user selects an entry.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
fun AutoTypeSelectionDialog(windowTitle: String, candidates: List<AutoTypeCandidate>, onSelect: (AutoTypeCandidate) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Auto-Type", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = windowTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    text = "No matching entries found for this window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn {
                    items(candidates, key = { it.entryUuid }) { candidate ->
                        AutoTypeCandidateRow(
                            candidate = candidate,
                            onClick = { onSelect(candidate) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AutoTypeCandidateRow(candidate: AutoTypeCandidate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.entryTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (candidate.userName.isNotBlank()) {
                Text(
                    text = candidate.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (candidate.groupPath.isNotBlank()) {
                Text(
                    text = candidate.groupPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (candidate.url.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = candidate.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.5f),
            )
        }
    }
}
