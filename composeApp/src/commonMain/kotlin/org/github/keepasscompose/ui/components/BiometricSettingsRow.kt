package org.github.keepasscompose.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.biometric.BiometricType

/**
 * A settings row composable that shows biometric unlock status and a toggle switch.
 *
 * Displays the biometric type name, enrollment status, and a switch
 * to enable/disable biometric unlock for the current database.
 *
 * @param biometricType The type of biometric available on this device.
 * @param isEnrolled Whether biometric unlock is currently enabled.
 * @param isAvailable Whether biometric hardware is available and configured.
 * @param onToggle Called when the user toggles the biometric unlock switch.
 */
@Composable
fun BiometricSettingsRow(biometricType: BiometricType, isEnrolled: Boolean, isAvailable: Boolean, onToggle: () -> Unit) {
    val biometricLabel = when (biometricType) {
        BiometricType.FINGERPRINT -> "Fingerprint"
        BiometricType.FACE -> "Face ID"
        BiometricType.IRIS -> "Iris"
        BiometricType.UNKNOWN -> "Biometric"
    }

    val icon = when (biometricType) {
        BiometricType.FINGERPRINT -> Icons.Filled.Fingerprint
        BiometricType.FACE -> Icons.Filled.Face
        else -> Icons.Filled.Lock
    }

    val statusText = when {
        !isAvailable -> "Not available"
        isEnrolled -> "Enabled"
        else -> "Disabled"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = biometricLabel,
            modifier = Modifier.size(24.dp),
            tint = if (isAvailable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$biometricLabel Unlock",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }

        Switch(
            checked = isEnrolled,
            onCheckedChange = { onToggle() },
            enabled = isAvailable,
        )
    }
}
