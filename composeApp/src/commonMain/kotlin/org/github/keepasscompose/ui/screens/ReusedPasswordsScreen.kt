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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.PasswordHealthAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReusedPasswordsScreen(
    reusedGroups: Map<String, List<PasswordHealthAnalyzer.EntryHealth>>,
    onEntryClick: (PasswordHealthAnalyzer.EntryHealth) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val totalEntries = reusedGroups.values.sumOf { it.size }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Reused Passwords") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalEntries ${if (totalEntries == 1) "entry" else "entries"} sharing " +
                    "${reusedGroups.size} ${if (reusedGroups.size == 1) "password" else "passwords"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (reusedGroups.isEmpty()) {
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
                            "No reused passwords!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Scaffold
            }

            val sortedGroups = reusedGroups.entries.sortedByDescending { it.value.size }

            LazyColumn {
                sortedGroups.forEachIndexed { groupIndex, (_, entries) ->
                    item(key = "header_$groupIndex") {
                        Text(
                            text = "Shared by ${entries.size} entries",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = if (groupIndex > 0) 16.dp else 0.dp, bottom = 4.dp),
                        )
                    }

                    items(entries, key = { it.entry.uuid }) { entryHealth ->
                        ReusedPasswordRow(entryHealth = entryHealth, onClick = { onEntryClick(entryHealth) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReusedPasswordRow(entryHealth: PasswordHealthAnalyzer.EntryHealth, onClick: () -> Unit) {
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
            if (entryHealth.entry.userName.isNotEmpty()) {
                Text(
                    text = entryHealth.entry.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entryHealth.groupPath.joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
