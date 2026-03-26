package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AppSettingsData(
    val startAtLogin: Boolean = false,
    val minimizeToTray: Boolean = false,
    val autoLockTimeout: Int = 300,
    val lockOnMinimize: Boolean = false,
    val clipboardClearTimeout: Int = 30,
    val theme: String = "system",
    val fontSize: String = "medium",
    val browserIntegrationEnabled: Boolean = false,
)

@Composable
fun AppSettingsScreen(
    settings: AppSettingsData = AppSettingsData(),
    onSave: (AppSettingsData) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Security", "Appearance", "Browser")

    var startAtLogin by remember { mutableStateOf(settings.startAtLogin) }
    var minimizeToTray by remember { mutableStateOf(settings.minimizeToTray) }
    var autoLockTimeout by remember { mutableStateOf(settings.autoLockTimeout.toString()) }
    var lockOnMinimize by remember { mutableStateOf(settings.lockOnMinimize) }
    var clipboardClearTimeout by remember { mutableStateOf(settings.clipboardClearTimeout.toString()) }
    var theme by remember { mutableStateOf(settings.theme) }
    var fontSize by remember { mutableStateOf(settings.fontSize) }
    var browserEnabled by remember { mutableStateOf(settings.browserIntegrationEnabled) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("App Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (selectedTab) {
                0 -> GeneralSettingsTab(
                    startAtLogin = startAtLogin,
                    minimizeToTray = minimizeToTray,
                    onStartAtLoginChange = { startAtLogin = it },
                    onMinimizeToTrayChange = { minimizeToTray = it },
                )
                1 -> SecuritySettingsTab(
                    autoLockTimeout = autoLockTimeout,
                    lockOnMinimize = lockOnMinimize,
                    clipboardClearTimeout = clipboardClearTimeout,
                    onAutoLockChange = { autoLockTimeout = it },
                    onLockOnMinimizeChange = { lockOnMinimize = it },
                    onClipboardClearChange = { clipboardClearTimeout = it },
                )
                2 -> AppearanceSettingsTab(
                    theme = theme,
                    fontSize = fontSize,
                    onThemeChange = { theme = it },
                    onFontSizeChange = { fontSize = it },
                )
                3 -> BrowserSettingsTab(
                    enabled = browserEnabled,
                    onEnabledChange = { browserEnabled = it },
                )
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onSave(
                    AppSettingsData(
                        startAtLogin = startAtLogin,
                        minimizeToTray = minimizeToTray,
                        autoLockTimeout = autoLockTimeout.toIntOrNull() ?: 300,
                        lockOnMinimize = lockOnMinimize,
                        clipboardClearTimeout = clipboardClearTimeout.toIntOrNull() ?: 30,
                        theme = theme,
                        fontSize = fontSize,
                        browserIntegrationEnabled = browserEnabled,
                    ),
                )
            }) { Text("Save") }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    startAtLogin: Boolean,
    minimizeToTray: Boolean,
    onStartAtLoginChange: (Boolean) -> Unit,
    onMinimizeToTrayChange: (Boolean) -> Unit,
) {
    Column {
        SettingSwitchRow("Start at login", startAtLogin, onStartAtLoginChange)
        SettingSwitchRow("Minimize to tray", minimizeToTray, onMinimizeToTrayChange)
    }
}

@Composable
private fun SecuritySettingsTab(
    autoLockTimeout: String,
    lockOnMinimize: Boolean,
    clipboardClearTimeout: String,
    onAutoLockChange: (String) -> Unit,
    onLockOnMinimizeChange: (Boolean) -> Unit,
    onClipboardClearChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = autoLockTimeout,
            onValueChange = { if (it.all { c -> c.isDigit() }) onAutoLockChange(it) },
            label = { Text("Auto-lock timeout (seconds)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingSwitchRow("Lock on minimize", lockOnMinimize, onLockOnMinimizeChange)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = clipboardClearTimeout,
            onValueChange = { if (it.all { c -> c.isDigit() }) onClipboardClearChange(it) },
            label = { Text("Clear clipboard after (seconds)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsTab(
    theme: String,
    fontSize: String,
    onThemeChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit,
) {
    Column {
        SettingDropdown("Theme", theme, listOf("system", "light", "dark"), onThemeChange)
        Spacer(modifier = Modifier.height(12.dp))
        SettingDropdown("Font Size", fontSize, listOf("small", "medium", "large"), onFontSizeChange)
    }
}

@Composable
private fun BrowserSettingsTab(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column {
        SettingSwitchRow("Enable browser integration", enabled, onEnabledChange)
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Browser extension will connect via native messaging.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingDropdown(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}
