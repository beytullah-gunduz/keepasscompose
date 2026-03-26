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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CreateDatabaseScreen(
    onSelectLocation: () -> Unit = {},
    onSelectKeyFile: () -> Unit = {},
    onCreate: (CreateDatabaseConfig) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var currentStep by remember { mutableIntStateOf(0) }

    // Step 1: Location
    var databaseName by remember { mutableStateOf("") }
    var databasePath by remember { mutableStateOf("") }

    // Step 2: Password
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Step 3: Key file
    var keyFilePath by remember { mutableStateOf("") }

    // Step 4: Settings
    var encryptionAlgorithm by remember { mutableStateOf("AES-256") }
    var kdfAlgorithm by remember { mutableStateOf("Argon2d") }
    var kdfIterations by remember { mutableStateOf("10") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Create New Database",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Step ${currentStep + 1} of 5",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { (currentStep + 1) / 5f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (currentStep) {
            0 -> StepLocation(
                databaseName = databaseName,
                databasePath = databasePath,
                onNameChange = { databaseName = it },
                onSelectLocation = onSelectLocation,
                onPathChange = { databasePath = it },
            )
            1 -> StepPassword(
                password = password,
                confirmPassword = confirmPassword,
                passwordVisible = passwordVisible,
                onPasswordChange = { password = it },
                onConfirmPasswordChange = { confirmPassword = it },
                onToggleVisibility = { passwordVisible = !passwordVisible },
            )
            2 -> StepKeyFile(
                keyFilePath = keyFilePath,
                onSelectKeyFile = onSelectKeyFile,
                onClearKeyFile = { keyFilePath = "" },
                onKeyFilePathChange = { keyFilePath = it },
            )
            3 -> StepSettings(
                encryptionAlgorithm = encryptionAlgorithm,
                kdfAlgorithm = kdfAlgorithm,
                kdfIterations = kdfIterations,
                onEncryptionChange = { encryptionAlgorithm = it },
                onKdfChange = { kdfAlgorithm = it },
                onIterationsChange = { kdfIterations = it },
            )
            4 -> StepConfirmation(
                databaseName = databaseName,
                databasePath = databasePath,
                hasKeyFile = keyFilePath.isNotEmpty(),
                encryption = encryptionAlgorithm,
                kdf = kdfAlgorithm,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = { currentStep-- }) {
                    Text("Back")
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }

            if (currentStep < 4) {
                val canProceed = when (currentStep) {
                    0 -> databaseName.isNotBlank() && databasePath.isNotBlank()
                    1 -> password.isNotEmpty() && password == confirmPassword
                    else -> true
                }
                Button(onClick = { currentStep++ }, enabled = canProceed) {
                    Text("Next")
                }
            } else {
                Button(onClick = {
                    onCreate(
                        CreateDatabaseConfig(
                            name = databaseName,
                            path = databasePath,
                            password = password,
                            keyFilePath = keyFilePath.ifEmpty { null },
                            encryptionAlgorithm = encryptionAlgorithm,
                            kdfAlgorithm = kdfAlgorithm,
                            kdfIterations = kdfIterations.toIntOrNull() ?: 10,
                        ),
                    )
                }) {
                    Text("Create")
                }
            }
        }
    }
}

data class CreateDatabaseConfig(
    val name: String,
    val path: String,
    val password: String,
    val keyFilePath: String?,
    val encryptionAlgorithm: String,
    val kdfAlgorithm: String,
    val kdfIterations: Int,
)

// Step 1
@Composable
private fun StepLocation(
    databaseName: String,
    databasePath: String,
    onNameChange: (String) -> Unit,
    onSelectLocation: () -> Unit,
    onPathChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Choose File Location", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = databaseName,
            onValueChange = onNameChange,
            label = { Text("Database Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = databasePath,
                onValueChange = onPathChange,
                label = { Text("Save Location") },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onSelectLocation) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Browse")
            }
        }
    }
}

// Step 2
@Composable
private fun StepPassword(
    password: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Set Master Password", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Password strength indicator
        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val strength = computePasswordStrength(password)
            LinearProgressIndicator(
                progress = { strength.score },
                modifier = Modifier.fillMaxWidth(),
                color = strength.color,
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.labelSmall,
                color = strength.color,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                { Text("Passwords do not match") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class PasswordStrength(
    val score: Float,
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
private fun computePasswordStrength(password: String): PasswordStrength {
    val length = password.length
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val variety = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }

    val score = ((length.coerceAtMost(20) / 20f) * 0.5f + (variety / 4f) * 0.5f).coerceIn(0f, 1f)
    return when {
        score < 0.3f -> PasswordStrength(score, "Weak", MaterialTheme.colorScheme.error)
        score < 0.6f -> PasswordStrength(score, "Fair", MaterialTheme.colorScheme.tertiary)
        score < 0.8f -> PasswordStrength(score, "Good", MaterialTheme.colorScheme.primary)
        else -> PasswordStrength(score, "Strong", MaterialTheme.colorScheme.primary)
    }
}

// Step 3
@Composable
private fun StepKeyFile(
    keyFilePath: String,
    onSelectKeyFile: () -> Unit,
    onClearKeyFile: () -> Unit,
    onKeyFilePathChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Key File (Optional)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A key file provides an additional layer of security. You can use it alongside your master password.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = keyFilePath,
                onValueChange = onKeyFilePathChange,
                label = { Text("Key File Path") },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = if (keyFilePath.isEmpty()) onSelectKeyFile else onClearKeyFile) {
                Text(if (keyFilePath.isEmpty()) "Browse" else "Clear")
            }
        }
    }
}

// Step 4
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepSettings(
    encryptionAlgorithm: String,
    kdfAlgorithm: String,
    kdfIterations: String,
    onEncryptionChange: (String) -> Unit,
    onKdfChange: (String) -> Unit,
    onIterationsChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Database Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        SettingsDropdown(
            label = "Encryption Algorithm",
            value = encryptionAlgorithm,
            options = listOf("AES-256", "ChaCha20", "Twofish"),
            onValueChange = onEncryptionChange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsDropdown(
            label = "Key Derivation Function",
            value = kdfAlgorithm,
            options = listOf("Argon2d", "Argon2id", "AES-KDF"),
            onValueChange = onKdfChange,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = kdfIterations,
            onValueChange = { if (it.all { c -> c.isDigit() }) onIterationsChange(it) },
            label = { Text("KDF Iterations") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
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
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onValueChange(option); expanded = false },
                )
            }
        }
    }
}

// Step 5
@Composable
private fun StepConfirmation(
    databaseName: String,
    databasePath: String,
    hasKeyFile: Boolean,
    encryption: String,
    kdf: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Confirm & Create", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        SummaryRow("Database Name", databaseName)
        SummaryRow("Location", databasePath)
        SummaryRow("Key File", if (hasKeyFile) "Yes" else "None")
        SummaryRow("Encryption", encryption)
        SummaryRow("KDF", kdf)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
