package org.github.keepasscompose.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class BrowserInstallStatus(val browserName: String, val installed: Boolean)

data class AppSettingsData(
    val startAtLogin: Boolean = false,
    val minimizeToTray: Boolean = false,
    val autoLockTimeout: Int = 300,
    val lockOnMinimize: Boolean = false,
    val clipboardClearTimeout: Int = 30,
    val theme: String = "system",
    val fontSize: String = "medium",
    val browserIntegrationEnabled: Boolean = false,
    val browserMatchStrategy: String = "domain",
    val browserShowNotifications: Boolean = true,
    val browserSortByTitle: Boolean = true,
    val browserMinimizeOnAutofill: Boolean = false,
    val browserInstalledBrowsers: List<BrowserInstallStatus> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var browserMatchStrategy by remember { mutableStateOf(settings.browserMatchStrategy) }
    var browserShowNotifications by remember { mutableStateOf(settings.browserShowNotifications) }
    var browserSortByTitle by remember { mutableStateOf(settings.browserSortByTitle) }
    var browserMinimizeOnAutofill by remember { mutableStateOf(settings.browserMinimizeOnAutofill) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
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
                        matchStrategy = browserMatchStrategy,
                        onMatchStrategyChange = { browserMatchStrategy = it },
                        showNotifications = browserShowNotifications,
                        onShowNotificationsChange = { browserShowNotifications = it },
                        sortByTitle = browserSortByTitle,
                        onSortByTitleChange = { browserSortByTitle = it },
                        minimizeOnAutofill = browserMinimizeOnAutofill,
                        onMinimizeOnAutofillChange = { browserMinimizeOnAutofill = it },
                        installedBrowsers = settings.browserInstalledBrowsers,
                        onInstallBrowserSupport = {},
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
                            browserMatchStrategy = browserMatchStrategy,
                            browserShowNotifications = browserShowNotifications,
                            browserSortByTitle = browserSortByTitle,
                            browserMinimizeOnAutofill = browserMinimizeOnAutofill,
                        ),
                    )
                }) { Text("Save") }
            }
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
private fun AppearanceSettingsTab(theme: String, fontSize: String, onThemeChange: (String) -> Unit, onFontSizeChange: (String) -> Unit) {
    Column {
        SettingDropdown("Theme", theme, listOf("system", "light", "dark"), onThemeChange)
        Spacer(modifier = Modifier.height(12.dp))
        SettingDropdown("Font Size", fontSize, listOf("small", "medium", "large"), onFontSizeChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserSettingsTab(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    matchStrategy: String,
    onMatchStrategyChange: (String) -> Unit,
    showNotifications: Boolean,
    onShowNotificationsChange: (Boolean) -> Unit,
    sortByTitle: Boolean,
    onSortByTitleChange: (Boolean) -> Unit,
    minimizeOnAutofill: Boolean,
    onMinimizeOnAutofillChange: (Boolean) -> Unit,
    installedBrowsers: List<BrowserInstallStatus>,
    onInstallBrowserSupport: () -> Unit,
) {
    Column {
        SettingSwitchRow("Enable browser integration", enabled, onEnabledChange)

        if (enabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Credential Matching", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            SettingDropdown(
                label = "URL match strategy",
                value = matchStrategy,
                options = listOf("exact", "domain", "host", "starts_with"),
                onValueChange = onMatchStrategyChange,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (matchStrategy) {
                    "exact" -> "Only match entries with the exact same URL."
                    "domain" -> "Match entries sharing the same domain (e.g. login.site.com matches site.com)."
                    "host" -> "Match entries with the exact same hostname."
                    "starts_with" -> "Match entries whose URL is a prefix of the page URL."
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Behavior", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitchRow("Show notifications on autofill", showNotifications, onShowNotificationsChange)
            SettingSwitchRow("Sort results by title", sortByTitle, onSortByTitleChange)
            SettingSwitchRow("Minimize after autofill", minimizeOnAutofill, onMinimizeOnAutofillChange)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Browser Support", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (installedBrowsers.isEmpty()) {
                Text(
                    text = "Click below to install native messaging support for detected browsers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        installedBrowsers.forEach { browser ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (browser.installed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = if (browser.installed) "Installed" else "Not installed",
                                    tint = if (browser.installed) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = browser.browserName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (browser.installed) "Installed" else "Not installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (browser.installed) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onInstallBrowserSupport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Install / Update Browser Support")
            }
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
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onValueChange(option)
                    expanded = false
                })
            }
        }
    }
}
