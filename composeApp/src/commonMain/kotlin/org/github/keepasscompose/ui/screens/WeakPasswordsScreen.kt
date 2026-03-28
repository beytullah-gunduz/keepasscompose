package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
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
import org.github.keepasscompose.core.common.PasswordStrength

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakPasswordsScreen(
    weakEntries: List<PasswordHealthAnalyzer.EntryHealth>,
    onEntryClick: (PasswordHealthAnalyzer.EntryHealth) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weak Passwords") },
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
                text = "${weakEntries.size} ${if (weakEntries.size == 1) "entry" else "entries"} with weak passwords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (weakEntries.isEmpty()) {
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
                            "All passwords are strong!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Scaffold
            }

            val sorted = weakEntries.sortedBy { it.strength.level.ordinal }

            LazyColumn {
                items(sorted, key = { it.entry.uuid }) { entryHealth ->
                    WeakPasswordRow(entryHealth = entryHealth, onClick = { onEntryClick(entryHealth) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun WeakPasswordRow(entryHealth: PasswordHealthAnalyzer.EntryHealth, onClick: () -> Unit) {
    val strengthColor = when (entryHealth.strength.level) {
        PasswordStrength.Level.VERY_WEAK -> MaterialTheme.colorScheme.error
        PasswordStrength.Level.WEAK -> MaterialTheme.colorScheme.error
        PasswordStrength.Level.FAIR -> MaterialTheme.colorScheme.tertiary
        PasswordStrength.Level.STRONG, PasswordStrength.Level.VERY_STRONG -> MaterialTheme.colorScheme.primary
    }
    val strengthFraction = when (entryHealth.strength.level) {
        PasswordStrength.Level.VERY_WEAK -> 0.1f
        PasswordStrength.Level.WEAK -> 0.3f
        PasswordStrength.Level.FAIR -> 0.5f
        PasswordStrength.Level.STRONG -> 0.75f
        PasswordStrength.Level.VERY_STRONG -> 1.0f
    }

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
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { strengthFraction },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = strengthColor,
                )
                Text(
                    text = entryHealth.strength.level.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor,
                )
            }
        }
    }
}
