package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.biometric.BiometricType

/**
 * A composable that shows a biometric authentication option on the unlock screen.
 *
 * Displays an icon representing the biometric type (fingerprint/face), an
 * "Unlock with Biometrics" button, a fallback "Use Password" link, and
 * status text showing the current authentication state.
 *
 * This is a UI-only component with no ViewModel logic.
 *
 * @param biometricType The type of biometric available on this device.
 * @param isAuthenticating Whether biometric authentication is currently in progress.
 * @param onBiometricAuth Called when the user taps the biometric unlock button.
 * @param onUsePassword Called when the user taps the "Use Password" fallback link.
 * @param errorMessage Optional error or status message to display.
 */
@Composable
fun BiometricUnlockPrompt(
    biometricType: BiometricType,
    isAuthenticating: Boolean,
    onBiometricAuth: () -> Unit,
    onUsePassword: () -> Unit,
    errorMessage: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Biometric type icon
        val icon = when (biometricType) {
            BiometricType.FINGERPRINT -> Icons.Filled.Fingerprint
            BiometricType.FACE -> Icons.Filled.Face
            else -> Icons.Filled.Lock
        }
        val biometricLabel = when (biometricType) {
            BiometricType.FINGERPRINT -> "Fingerprint"
            BiometricType.FACE -> "Face ID"
            BiometricType.IRIS -> "Iris"
            BiometricType.UNKNOWN -> "Biometrics"
        }

        Icon(
            imageVector = icon,
            contentDescription = biometricLabel,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status text
        if (isAuthenticating) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticating...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unlock button
        Button(
            onClick = onBiometricAuth,
            enabled = !isAuthenticating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Unlock with $biometricLabel")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fallback link
        TextButton(onClick = onUsePassword) {
            Text("Use Password")
        }
    }
}
