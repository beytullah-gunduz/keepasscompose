package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxMeta

data class DatabaseSettingsResult(
    val databaseName: String,
    val description: String,
    val defaultUserName: String,
    val encryptionAlgorithm: String,
    val kdfAlgorithm: String,
    val historyMaxItems: Int,
    val historyMaxSize: Long,
)

@Composable
fun DatabaseSettingsScreen(
    meta: KdbxMeta,
    encryptionAlgorithm: String = "AES-256",
    kdfAlgorithm: String = "Argon2d",
    onSave: (DatabaseSettingsResult) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Security", "Maintenance")

    var databaseName by remember { mutableStateOf(meta.databaseName) }
    var description by remember { mutableStateOf(meta.description) }
    var defaultUserName by remember { mutableStateOf(meta.defaultUserName) }
    var encryption by remember { mutableStateOf(encryptionAlgorithm) }
    var kdf by remember { mutableStateOf(kdfAlgorithm) }
    var historyMaxItems by remember { mutableStateOf(meta.historyMaxItems.toString()) }
    var historyMaxSize by remember { mutableStateOf((meta.historyMaxSize / (1024 * 1024)).toString()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Database Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            when (selectedTab) {
                0 -> GeneralTab(
                    databaseName = databaseName,
                    description = description,
                    defaultUserName = defaultUserName,
                    onNameChange = { databaseName = it },
                    onDescriptionChange = { description = it },
                    onDefaultUserNameChange = { defaultUserName = it },
                )
                1 -> SecurityTab(
                    encryption = encryption,
                    kdf = kdf,
                    onEncryptionChange = { encryption = it },
                    onKdfChange = { kdf = it },
                )
                2 -> MaintenanceTab(
                    historyMaxItems = historyMaxItems,
                    historyMaxSize = historyMaxSize,
                    onMaxItemsChange = { historyMaxItems = it },
                    onMaxSizeChange = { historyMaxSize = it },
                )
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Button(onClick = {
                onSave(
                    DatabaseSettingsResult(
                        databaseName = databaseName,
                        description = description,
                        defaultUserName = defaultUserName,
                        encryptionAlgorithm = encryption,
                        kdfAlgorithm = kdf,
                        historyMaxItems = historyMaxItems.toIntOrNull() ?: KdbxMeta.DEFAULT_HISTORY_MAX_ITEMS,
                        historyMaxSize = (historyMaxSize.toLongOrNull() ?: 6) * 1024 * 1024,
                    ),
                )
            }) { Text("Save") }
        }
    }
}

@Composable
private fun GeneralTab(
    databaseName: String,
    description: String,
    defaultUserName: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDefaultUserNameChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = databaseName,
            onValueChange = onNameChange,
            label = { Text("Database Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = defaultUserName,
            onValueChange = onDefaultUserNameChange,
            label = { Text("Default Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SecurityTab(
    encryption: String,
    kdf: String,
    onEncryptionChange: (String) -> Unit,
    onKdfChange: (String) -> Unit,
) {
    Column {
        Text("Encryption Algorithm", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = encryption,
            onValueChange = onEncryptionChange,
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Key Derivation Function", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = kdf,
            onValueChange = onKdfChange,
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use the KDF Configuration screen to change these settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MaintenanceTab(
    historyMaxItems: String,
    historyMaxSize: String,
    onMaxItemsChange: (String) -> Unit,
    onMaxSizeChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = historyMaxItems,
            onValueChange = { if (it.all { c -> c.isDigit() }) onMaxItemsChange(it) },
            label = { Text("History Max Items") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = historyMaxSize,
            onValueChange = { if (it.all { c -> c.isDigit() }) onMaxSizeChange(it) },
            label = { Text("History Max Size (MB)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
