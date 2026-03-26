package org.github.keepasscompose.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.github.keepasscompose.core.common.TotpGenerator

/**
 * Displays a TOTP code with a circular countdown timer, auto-refresh, and copy button.
 *
 * @param params The TOTP parameters (secret, algorithm, digits, period).
 * @param crypto Optional CryptoProvider for SHA-256/SHA-512 algorithms.
 * @param onCopy Called when the user taps the copy button with the current code.
 * @param modifier Modifier for the component.
 */
@Composable
fun TotpDisplay(
    params: TotpGenerator.TotpParams,
    crypto: org.github.keepasscompose.core.crypto.CryptoProvider? = null,
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    var secondsRemaining by remember { mutableStateOf(params.period) }

    // Refresh code and countdown every second
    LaunchedEffect(params) {
        while (true) {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            code = TotpGenerator.generateCode(params, crypto, now)
            secondsRemaining = TotpGenerator.secondsRemaining(params.period, now)
            delay(1000L)
        }
    }

    val progress = secondsRemaining.toFloat() / params.period.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "totp-countdown",
    )

    val isExpiring = secondsRemaining <= 5
    val timerColor = if (isExpiring) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Circular countdown timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp),
            ) {
                CountdownCircle(
                    progress = animatedProgress,
                    color = timerColor,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "$secondsRemaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = timerColor,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // TOTP code with large monospace font
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatCode(code),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = if (isExpiring) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = "${params.period}s · ${params.algorithm.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Copy button
            IconButton(onClick = { onCopy(code) }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy TOTP code",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Circular progress indicator for the TOTP countdown.
 */
@Composable
private fun CountdownCircle(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val trackColor = color.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset(
            (size.width - radius * 2) / 2,
            (size.height - radius * 2) / 2,
        )
        val arcSize = Size(radius * 2, radius * 2)

        // Background track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

/** Format a TOTP code with a space in the middle for readability (e.g., "123 456"). */
private fun formatCode(code: String): String {
    if (code.length <= 4) return code
    val mid = code.length / 2
    return "${code.substring(0, mid)} ${code.substring(mid)}"
}
