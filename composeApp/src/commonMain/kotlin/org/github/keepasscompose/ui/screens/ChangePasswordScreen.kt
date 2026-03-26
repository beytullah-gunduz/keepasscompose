package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ChangePasswordScreen(
    isProcessing: Boolean = false,
    errorMessage: String? = null,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit = { _, _ -> },
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword
    val canSubmit = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && passwordsMatch && !isProcessing

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("Change Master Password", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current Password") },
            singleLine = true,
            enabled = !isProcessing,
            visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            singleLine = true,
            enabled = !isProcessing,
            visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPasswords = !showPasswords }) {
                    Icon(
                        if (showPasswords) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Strength indicator
        if (newPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val strength = computeStrength(newPassword)
            LinearProgressIndicator(
                progress = { strength.first },
                modifier = Modifier.fillMaxWidth(),
                color = strength.third,
            )
            Text(strength.second, style = MaterialTheme.typography.labelSmall, color = strength.third)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password") },
            singleLine = true,
            enabled = !isProcessing,
            visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                { Text("Passwords do not match") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !isProcessing) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onChangePassword(currentPassword, newPassword) }, enabled = canSubmit) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-encrypting...")
                } else {
                    Text("Change Password")
                }
            }
        }
    }
}

@Composable
private fun computeStrength(password: String): Triple<Float, String, androidx.compose.ui.graphics.Color> {
    val length = password.length
    val variety = listOf(
        password.any { it.isUpperCase() },
        password.any { it.isLowerCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() },
    ).count { it }
    val score = ((length.coerceAtMost(20) / 20f) * 0.5f + (variety / 4f) * 0.5f).coerceIn(0f, 1f)
    return when {
        score < 0.3f -> Triple(score, "Weak", MaterialTheme.colorScheme.error)
        score < 0.6f -> Triple(score, "Fair", MaterialTheme.colorScheme.tertiary)
        score < 0.8f -> Triple(score, "Good", MaterialTheme.colorScheme.primary)
        else -> Triple(score, "Strong", MaterialTheme.colorScheme.primary)
    }
}
