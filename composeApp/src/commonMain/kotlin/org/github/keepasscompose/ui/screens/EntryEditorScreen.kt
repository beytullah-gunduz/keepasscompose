package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

data class EntryEditorField(
    val key: String,
    val value: String,
    val isProtected: Boolean = false,
)

data class EntryEditorResult(
    val title: String,
    val userName: String,
    val password: String,
    val url: String,
    val notes: String,
    val iconIndex: Int,
    val tags: List<String>,
    val customFields: List<EntryEditorField>,
    val expires: Boolean,
    val expiryDate: String,
)

@Composable
fun EntryEditorScreen(
    initialTitle: String = "",
    initialUserName: String = "",
    initialPassword: String = "",
    initialUrl: String = "",
    initialNotes: String = "",
    initialIconIndex: Int = 0,
    initialTags: List<String> = emptyList(),
    initialCustomFields: List<EntryEditorField> = emptyList(),
    initialExpires: Boolean = false,
    initialExpiryDate: String = "",
    isEditMode: Boolean = false,
    onSave: (EntryEditorResult) -> Unit = {},
    onCancel: () -> Unit = {},
    onGeneratePassword: () -> Unit = {},
) {
    var title by remember { mutableStateOf(initialTitle) }
    var userName by remember { mutableStateOf(initialUserName) }
    var password by remember { mutableStateOf(initialPassword) }
    var url by remember { mutableStateOf(initialUrl) }
    var notes by remember { mutableStateOf(initialNotes) }
    var iconIndex by remember { mutableStateOf(initialIconIndex) }
    var tagInput by remember { mutableStateOf(initialTags.joinToString(", ")) }
    val customFields = remember { mutableStateListOf<EntryEditorField>().apply { addAll(initialCustomFields) } }
    var expires by remember { mutableStateOf(initialExpires) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = if (isEditMode) "Edit Entry" else "Create Entry",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Key,
                contentDescription = "Icon $iconIndex",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).clickable { /* icon picker wired later */ },
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        // Password with generate and show/hide
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onGeneratePassword) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate password")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        // Expiration
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = expires, onCheckedChange = { expires = it })
            Text("Expires", style = MaterialTheme.typography.bodyMedium)
        }
        if (expires) {
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text("Expiry Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Tags
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            label = { Text("Tags (comma-separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Custom fields
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Custom Fields", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { customFields.add(EntryEditorField("", "")) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add field")
            }
        }
        customFields.forEachIndexed { index, field ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = field.key,
                    onValueChange = { customFields[index] = field.copy(key = it) },
                    label = { Text("Key") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = field.value,
                    onValueChange = { customFields[index] = field.copy(value = it) },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { customFields.removeAt(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Action buttons
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onSave(
                        EntryEditorResult(
                            title = title,
                            userName = userName,
                            password = password,
                            url = url,
                            notes = notes,
                            iconIndex = iconIndex,
                            tags = tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            customFields = customFields.toList(),
                            expires = expires,
                            expiryDate = expiryDate,
                        ),
                    )
                },
                enabled = title.isNotBlank(),
            ) {
                Text(if (isEditMode) "Save" else "Create")
            }
        }
    }
}
