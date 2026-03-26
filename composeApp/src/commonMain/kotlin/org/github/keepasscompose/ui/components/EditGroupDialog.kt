package org.github.keepasscompose.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxGroup

data class EditGroupResult(
    val name: String,
    val iconIndex: Int,
    val notes: String,
)

@Composable
fun EditGroupDialog(
    group: KdbxGroup,
    createdTime: String = "",
    modifiedTime: String = "",
    onDismiss: () -> Unit,
    onSave: (EditGroupResult) -> Unit,
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedIcon by remember { mutableIntStateOf(group.icon.standardIndex) }
    var notes by remember { mutableStateOf(group.notes) }
    var showIconPicker by remember { mutableStateOf(false) }

    if (showIconPicker) {
        IconPickerDialog(
            selectedIndex = selectedIcon,
            onSelect = { selectedIcon = it; showIconPicker = false },
            onDismiss = { showIconPicker = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = "Icon $selectedIcon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { showIconPicker = true }
                        .padding(8.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (createdTime.isNotEmpty() || modifiedTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (createdTime.isNotEmpty()) {
                        TimestampRow("Created", createdTime)
                    }
                    if (modifiedTime.isNotEmpty()) {
                        TimestampRow("Modified", modifiedTime)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(EditGroupResult(name, selectedIcon, notes)) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TimestampRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
