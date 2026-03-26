package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.PasswordHealthAnalyzer
import org.github.keepasscompose.core.common.PasswordStrength

@Composable
fun ReportsScreen(
    report: PasswordHealthAnalyzer.HealthReport,
    totalGroups: Int,
    totalAttachments: Int,
    onWeakPasswords: () -> Unit = {},
    onReusedPasswords: () -> Unit = {},
    onExpiredEntries: () -> Unit = {},
    onHibpReport: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Database Reports", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Summary statistics
        Text("Overview", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                icon = Icons.Filled.Key,
                label = "Entries",
                value = report.totalEntries.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Folder,
                label = "Groups",
                value = totalGroups.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Attachment,
                label = "Attachments",
                value = totalAttachments.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        // Password health score
        Spacer(modifier = Modifier.height(24.dp))
        Text("Password Health", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        val strongCount = report.entries.count {
            it.entry.password.isNotEmpty() &&
                it.strength.level >= PasswordStrength.Level.STRONG
        }
        val healthScore = if (report.entriesWithPasswords > 0) {
            strongCount.toFloat() / report.entriesWithPasswords
        } else {
            0f
        }
        val healthColor = when {
            healthScore >= 0.8f -> MaterialTheme.colorScheme.primary
            healthScore >= 0.5f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${(healthScore * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = healthColor,
                    )
                    Text(
                        text = "Average: ${report.averageStrength.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { healthScore },
                    modifier = Modifier.fillMaxWidth(),
                    color = healthColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$strongCount of ${report.entriesWithPasswords} passwords are strong or better",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Strength distribution
        Spacer(modifier = Modifier.height(16.dp))
        Text("Strength Distribution", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        val distribution = PasswordStrength.Level.entries.associateWith { level ->
            report.entries.count {
                it.entry.password.isNotEmpty() && it.strength.level == level
            }
        }

        distribution.forEach { (level, count) ->
            val fraction = if (report.entriesWithPasswords > 0) {
                count.toFloat() / report.entriesWithPasswords
            } else {
                0f
            }
            val barColor = when (level) {
                PasswordStrength.Level.VERY_WEAK -> MaterialTheme.colorScheme.error
                PasswordStrength.Level.WEAK -> MaterialTheme.colorScheme.error
                PasswordStrength.Level.FAIR -> MaterialTheme.colorScheme.tertiary
                PasswordStrength.Level.STRONG -> MaterialTheme.colorScheme.primary
                PasswordStrength.Level.VERY_STRONG -> MaterialTheme.colorScheme.primary
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                Text(
                    text = level.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(80.dp),
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = barColor,
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(32.dp).padding(start = 8.dp),
                )
            }
        }

        // Report cards
        Spacer(modifier = Modifier.height(24.dp))
        Text("Reports", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        ReportCard(
            icon = Icons.Filled.Warning,
            title = "Weak Passwords",
            count = report.weakPasswords.size,
            color = MaterialTheme.colorScheme.error,
            onClick = onWeakPasswords,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReportCard(
            icon = Icons.Filled.Lock,
            title = "Reused Passwords",
            count = report.reusedPasswords.values.sumOf { it.size },
            color = MaterialTheme.colorScheme.tertiary,
            onClick = onReusedPasswords,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReportCard(
            icon = Icons.Filled.Event,
            title = "Expired Entries",
            count = report.expiredEntries.size + report.expiringSoonEntries.size,
            color = MaterialTheme.colorScheme.error,
            onClick = onExpiredEntries,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReportCard(
            icon = Icons.Filled.Shield,
            title = "HIBP Breach Check",
            count = null,
            color = MaterialTheme.colorScheme.secondary,
            onClick = onHibpReport,
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportCard(icon: ImageVector, title: String, count: Int?, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
