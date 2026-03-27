package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.biometric.BiometricType

/**
 * Steps in the biometric enrollment flow.
 */
enum class BiometricEnrollmentStep {
    /** User must verify their database password first. */
    VERIFY_PASSWORD,

    /** User must authenticate with biometrics. */
    AUTHENTICATE_BIOMETRIC,

    /** Confirm enrollment before saving. */
    CONFIRM,

    /** Enrollment completed successfully. */
    COMPLETE,

    /** An error occurred during enrollment. */
    ERROR,
}

/**
 * A dialog composable for enabling or disabling biometric unlock.
 *
 * Guides the user through enrollment steps: verify password, authenticate
 * biometric, confirm enrollment. Also supports disabling biometric unlock
 * for already-enrolled databases.
 *
 * @param isEnrolled Whether biometric unlock is currently enabled for this database.
 * @param biometricType The type of biometric available on this device.
 * @param enrollmentStep The current step in the enrollment flow.
 * @param onVerifyPassword Called when the user submits their password for verification.
 * @param onAuthenticateBiometric Called to trigger biometric authentication.
 * @param onConfirmEnrollment Called when the user confirms enrollment.
 * @param onDisableBiometric Called when the user chooses to disable biometric unlock.
 * @param onDismiss Called when the dialog is dismissed.
 * @param errorMessage Optional error message to display in the ERROR step.
 */
@Composable
fun BiometricEnrollmentDialog(
    isEnrolled: Boolean,
    biometricType: BiometricType,
    enrollmentStep: BiometricEnrollmentStep,
    onVerifyPassword: (String) -> Unit,
    onAuthenticateBiometric: () -> Unit,
    onConfirmEnrollment: () -> Unit,
    onDisableBiometric: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
) {
    val biometricLabel = when (biometricType) {
        BiometricType.FINGERPRINT -> "Fingerprint"
        BiometricType.FACE -> "Face ID"
        BiometricType.IRIS -> "Iris"
        BiometricType.UNKNOWN -> "Biometric"
    }

    if (isEnrolled && enrollmentStep == BiometricEnrollmentStep.VERIFY_PASSWORD) {
        // Disable dialog
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Disable $biometricLabel Unlock") },
            text = {
                Text("Are you sure you want to disable $biometricLabel unlock? You will need to enter your password to unlock the database.")
            },
            confirmButton = {
                TextButton(onClick = onDisableBiometric) {
                    Text("Disable", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (enrollmentStep) {
                    BiometricEnrollmentStep.VERIFY_PASSWORD -> "Enable $biometricLabel Unlock"
                    BiometricEnrollmentStep.AUTHENTICATE_BIOMETRIC -> "Verify $biometricLabel"
                    BiometricEnrollmentStep.CONFIRM -> "Confirm Enrollment"
                    BiometricEnrollmentStep.COMPLETE -> "Setup Complete"
                    BiometricEnrollmentStep.ERROR -> "Enrollment Error"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (enrollmentStep) {
                    BiometricEnrollmentStep.VERIFY_PASSWORD -> VerifyPasswordContent(onVerifyPassword)

                    BiometricEnrollmentStep.AUTHENTICATE_BIOMETRIC -> AuthenticateBiometricContent(
                        biometricType = biometricType,
                        biometricLabel = biometricLabel,
                        onAuthenticate = onAuthenticateBiometric,
                    )

                    BiometricEnrollmentStep.CONFIRM -> ConfirmContent(biometricLabel)

                    BiometricEnrollmentStep.COMPLETE -> CompleteContent(biometricLabel)

                    BiometricEnrollmentStep.ERROR -> ErrorContent(errorMessage)
                }
            }
        },
        confirmButton = {
            when (enrollmentStep) {
                BiometricEnrollmentStep.CONFIRM -> {
                    TextButton(onClick = onConfirmEnrollment) {
                        Text("Enable")
                    }
                }

                BiometricEnrollmentStep.COMPLETE, BiometricEnrollmentStep.ERROR -> {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            when (enrollmentStep) {
                BiometricEnrollmentStep.VERIFY_PASSWORD,
                BiometricEnrollmentStep.AUTHENTICATE_BIOMETRIC,
                BiometricEnrollmentStep.CONFIRM,
                -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }

                else -> {}
            }
        },
    )
}

@Composable
private fun VerifyPasswordContent(onVerifyPassword: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    Text(
        text = "Enter your database password to enable biometric unlock.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { onVerifyPassword(password) },
        enabled = password.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Verify Password")
    }
}

@Composable
private fun AuthenticateBiometricContent(biometricType: BiometricType, biometricLabel: String, onAuthenticate: () -> Unit) {
    val icon = when (biometricType) {
        BiometricType.FINGERPRINT -> Icons.Filled.Fingerprint
        BiometricType.FACE -> Icons.Filled.Face
        else -> Icons.Filled.Lock
    }

    Icon(
        imageVector = icon,
        contentDescription = biometricLabel,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Authenticate with $biometricLabel to complete setup.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onAuthenticate,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Authenticate")
    }
}

@Composable
private fun ConfirmContent(biometricLabel: String) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Ready",
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Password verified and $biometricLabel authenticated. Enable $biometricLabel unlock for this database?",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CompleteContent(biometricLabel: String) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Complete",
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "$biometricLabel unlock is now enabled. You can unlock this database using $biometricLabel.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorContent(errorMessage: String?) {
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = "Error",
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = errorMessage ?: "An error occurred during enrollment. Please try again.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
