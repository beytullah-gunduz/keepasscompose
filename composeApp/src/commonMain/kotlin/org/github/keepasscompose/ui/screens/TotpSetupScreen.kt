package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import org.github.keepasscompose.core.common.TotpGenerator

/**
 * Dialog for setting up TOTP on an entry.
 *
 * Supports two modes:
 * 1. **Paste URI**: Paste an `otpauth://` URI to auto-fill all fields
 * 2. **Manual entry**: Enter secret key, algorithm, period, and digits manually
 *
 * @param initialParams Pre-filled params if editing an existing TOTP config.
 * @param onConfirm Called with the configured [TotpGenerator.TotpParams] when the user confirms.
 * @param onDismiss Called when the user cancels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpSetupDialog(initialParams: TotpGenerator.TotpParams? = null, onConfirm: (TotpGenerator.TotpParams) -> Unit, onDismiss: () -> Unit) {
    var uriInput by remember { mutableStateOf("") }
    var secretInput by remember { mutableStateOf(initialParams?.secret?.let { base32Encode(it) } ?: "") }
    var selectedAlgorithm by remember { mutableStateOf(initialParams?.algorithm ?: TotpGenerator.Algorithm.SHA1) }
    var digits by remember { mutableStateOf(initialParams?.digits?.toFloat() ?: 6f) }
    var period by remember { mutableStateOf(initialParams?.period?.toFloat() ?: 30f) }
    var uriError by remember { mutableStateOf<String?>(null) }
    var secretError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Up TOTP") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // URI paste section
                Text("Paste otpauth:// URI", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = uriInput,
                    onValueChange = { input ->
                        uriInput = input
                        uriError = null
                        if (input.startsWith("otpauth://")) {
                            try {
                                val parsed = TotpGenerator.parseOtpauthUri(input)
                                secretInput = base32Encode(parsed.secret)
                                selectedAlgorithm = parsed.algorithm
                                digits = parsed.digits.toFloat()
                                period = parsed.period.toFloat()
                                uriError = null
                            } catch (e: Exception) {
                                uriError = "Invalid URI: ${e.message}"
                            }
                        }
                    },
                    label = { Text("otpauth:// URI") },
                    isError = uriError != null,
                    supportingText = uriError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("— or enter manually —", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Secret key
                OutlinedTextField(
                    value = secretInput,
                    onValueChange = {
                        secretInput = it.uppercase().filter { c -> c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567= " }
                        secretError = null
                    },
                    label = { Text("Secret Key (Base32)") },
                    isError = secretError != null,
                    supportingText = secretError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Algorithm dropdown
                var algorithmExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = algorithmExpanded,
                    onExpandedChange = { algorithmExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedAlgorithm.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Algorithm") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = algorithmExpanded,
                        onDismissRequest = { algorithmExpanded = false },
                    ) {
                        TotpGenerator.Algorithm.entries.forEach { algorithm ->
                            DropdownMenuItem(
                                text = { Text(algorithm.name) },
                                onClick = {
                                    selectedAlgorithm = algorithm
                                    algorithmExpanded = false
                                },
                            )
                        }
                    }
                }

                // Digits slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Digits: ${digits.toInt()}", modifier = Modifier.width(80.dp))
                    Slider(
                        value = digits,
                        onValueChange = { digits = it },
                        valueRange = 6f..8f,
                        steps = 1,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Period slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Period: ${period.toInt()}s", modifier = Modifier.width(80.dp))
                    Slider(
                        value = period,
                        onValueChange = { period = it },
                        valueRange = 15f..60f,
                        steps = 2,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanSecret = secretInput.replace(" ", "").replace("=", "")
                    if (cleanSecret.isEmpty()) {
                        secretError = "Secret key is required"
                        return@TextButton
                    }
                    try {
                        val secret = TotpGenerator.base32Decode(cleanSecret)
                        if (secret.isEmpty()) {
                            secretError = "Invalid Base32 key"
                            return@TextButton
                        }
                        onConfirm(
                            TotpGenerator.TotpParams(
                                secret = secret,
                                algorithm = selectedAlgorithm,
                                digits = digits.toInt(),
                                period = period.toInt(),
                            ),
                        )
                    } catch (e: Exception) {
                        secretError = "Invalid key: ${e.message}"
                    }
                },
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

private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

private fun base32Encode(data: ByteArray): String {
    val sb = StringBuilder()
    var buffer = 0
    var bitsLeft = 0
    for (byte in data) {
        buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
        bitsLeft += 8
        while (bitsLeft >= 5) {
            bitsLeft -= 5
            sb.append(BASE32_ALPHABET[(buffer shr bitsLeft) and 0x1F])
        }
    }
    if (bitsLeft > 0) {
        sb.append(BASE32_ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
    }
    return sb.toString()
}
