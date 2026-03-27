package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import org.github.keepasscompose.core.autotype.AutoTypeSequenceParser

/**
 * Auto-type configuration data for an entry.
 */
data class AutoTypeConfig(
    val enabled: Boolean = true,
    val defaultSequence: String = AutoTypeSequenceParser.DEFAULT_SEQUENCE,
    val windowAssociations: List<WindowAssociation> = emptyList(),
    val obfuscation: Boolean = false,
)

/**
 * Association between a window title pattern and an auto-type sequence.
 */
data class WindowAssociation(val windowPattern: String, val sequence: String = AutoTypeSequenceParser.DEFAULT_SEQUENCE)

/**
 * Editor component for configuring auto-type settings on an entry.
 *
 * Allows the user to:
 * - Enable/disable auto-type for this entry
 * - Set the default auto-type sequence
 * - Add/remove window title associations with custom sequences
 * - Toggle two-channel auto-type obfuscation
 *
 * @param config Current auto-type configuration.
 * @param onConfigChange Called when any configuration value changes.
 * @param modifier Modifier for the root layout.
 */
@Composable
fun AutoTypeConfigEditor(config: AutoTypeConfig, onConfigChange: (AutoTypeConfig) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("Auto-Type", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Enable/disable toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enable auto-type", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = config.enabled,
                onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
            )
        }

        if (config.enabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Default sequence
            Text("Default Sequence", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = config.defaultSequence,
                onValueChange = { onConfigChange(config.copy(defaultSequence = it)) },
                label = { Text("Sequence") },
                placeholder = { Text(AutoTypeSequenceParser.DEFAULT_SEQUENCE) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Placeholders: {USERNAME}, {PASSWORD}, {TAB}, {ENTER}, {DELAY X}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Window associations
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Window Associations", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val updated = config.copy(
                        windowAssociations = config.windowAssociations + WindowAssociation("", AutoTypeSequenceParser.DEFAULT_SEQUENCE),
                    )
                    onConfigChange(updated)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (config.windowAssociations.isEmpty()) {
                Text(
                    text = "No window associations. The entry title will be used for matching.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                config.windowAssociations.forEachIndexed { index, association ->
                    WindowAssociationRow(
                        association = association,
                        onUpdate = { updated ->
                            val list = config.windowAssociations.toMutableList()
                            list[index] = updated
                            onConfigChange(config.copy(windowAssociations = list))
                        },
                        onDelete = {
                            val list = config.windowAssociations.toMutableList()
                            list.removeAt(index)
                            onConfigChange(config.copy(windowAssociations = list))
                        },
                    )
                    if (index < config.windowAssociations.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Obfuscation toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Two-channel auto-type obfuscation", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Uses clipboard and keyboard to make keylogger capture harder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = config.obfuscation,
                    onCheckedChange = { onConfigChange(config.copy(obfuscation = it)) },
                )
            }
        }
    }
}

@Composable
private fun WindowAssociationRow(association: WindowAssociation, onUpdate: (WindowAssociation) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = association.windowPattern,
                    onValueChange = { onUpdate(association.copy(windowPattern = it)) },
                    label = { Text("Window title pattern") },
                    placeholder = { Text("*Browser - Login*") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = association.sequence,
                onValueChange = { onUpdate(association.copy(sequence = it)) },
                label = { Text("Sequence (optional)") },
                placeholder = { Text(AutoTypeSequenceParser.DEFAULT_SEQUENCE) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
