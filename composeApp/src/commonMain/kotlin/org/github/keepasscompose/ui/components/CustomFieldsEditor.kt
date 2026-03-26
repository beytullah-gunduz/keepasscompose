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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.ui.screens.EntryEditorField

@Composable
fun CustomFieldsEditor(
    fields: List<EntryEditorField>,
    onFieldChanged: (index: Int, field: EntryEditorField) -> Unit,
    onFieldRemoved: (index: Int) -> Unit,
    onFieldAdded: () -> Unit,
    onFieldMoved: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Custom Fields", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onFieldAdded) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Field")
            }
        }

        fields.forEachIndexed { index, field ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            CustomFieldRow(
                field = field,
                index = index,
                isFirst = index == 0,
                isLast = index == fields.lastIndex,
                onChanged = { onFieldChanged(index, it) },
                onRemoved = { onFieldRemoved(index) },
                onMoveUp = { if (index > 0) onFieldMoved(index, index - 1) },
                onMoveDown = { if (index < fields.lastIndex) onFieldMoved(index, index + 1) },
            )
        }

        if (fields.isEmpty()) {
            Text(
                text = "No custom fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CustomFieldRow(
    field: EntryEditorField,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onChanged: (EntryEditorField) -> Unit,
    onRemoved: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = field.key,
                onValueChange = { onChanged(field.copy(key = it)) },
                label = { Text("Key") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = field.value,
                onValueChange = { onChanged(field.copy(value = it)) },
                label = { Text("Value") },
                singleLine = true,
                visualTransformation = if (field.isProtected) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Protected toggle
            IconButton(
                onClick = { onChanged(field.copy(isProtected = !field.isProtected)) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (field.isProtected) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (field.isProtected) "Protected" else "Not protected",
                    modifier = Modifier.size(16.dp),
                    tint = if (field.isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Reorder buttons
            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
            }

            // Remove
            IconButton(onClick = onRemoved, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove field",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
