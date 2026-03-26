package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.PasswordHealthAnalyzer

@Composable
fun ExpiredEntriesScreen(
    expiredEntries: List<PasswordHealthAnalyzer.EntryHealth>,
    expiringSoonEntries: List<PasswordHealthAnalyzer.EntryHealth>,
    onEntryClick: (PasswordHealthAnalyzer.EntryHealth) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val total = expiredEntries.size + expiringSoonEntries.size

    Column(modifier = modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
        Text("Expired Entries", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${expiredEntries.size} expired, ${expiringSoonEntries.size} expiring soon",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (total == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No expired entries!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        LazyColumn {
            if (expiredEntries.isNotEmpty()) {
                item(key = "expired_header") {
                    Text(
                        text = "Expired (${expiredEntries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(expiredEntries, key = { "expired_${it.entry.uuid}" }) { entryHealth ->
                    ExpiredEntryRow(entryHealth = entryHealth, isExpired = true, onClick = { onEntryClick(entryHealth) })
                    HorizontalDivider()
                }
            }

            if (expiringSoonEntries.isNotEmpty()) {
                item(key = "expiring_header") {
                    Text(
                        text = "Expiring Soon (${expiringSoonEntries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = if (expiredEntries.isNotEmpty()) 16.dp else 0.dp, bottom = 4.dp),
                    )
                }
                items(expiringSoonEntries, key = { "expiring_${it.entry.uuid}" }) { entryHealth ->
                    ExpiredEntryRow(entryHealth = entryHealth, isExpired = false, onClick = { onEntryClick(entryHealth) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ExpiredEntryRow(entryHealth: PasswordHealthAnalyzer.EntryHealth, isExpired: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entryHealth.entry.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entryHealth.groupPath.joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val expiryText = entryHealth.entry.expiryTime?.toString()?.take(10) ?: "Unknown"
            Text(
                text = "Expires: $expiryText",
                style = MaterialTheme.typography.labelSmall,
                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
        }
        Icon(
            if (isExpired) Icons.Filled.Error else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}
