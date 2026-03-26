package org.github.keepasscompose.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private const val THEME_ANIMATION_DURATION_MS = 300

@Composable
fun resolveIsDarkTheme(preference: String): Boolean = when (preference) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun KeePassTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val targetScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val animatedScheme = animateColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = KeePassTypography,
        shapes = KeePassShapes,
        content = content,
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<androidx.compose.ui.graphics.Color>(THEME_ANIMATION_DURATION_MS)
    return target.copy(
        primary = animateColorAsState(target.primary, spec).value,
        onPrimary = animateColorAsState(target.onPrimary, spec).value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec).value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec).value,
        secondary = animateColorAsState(target.secondary, spec).value,
        onSecondary = animateColorAsState(target.onSecondary, spec).value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec).value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec).value,
        tertiary = animateColorAsState(target.tertiary, spec).value,
        onTertiary = animateColorAsState(target.onTertiary, spec).value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, spec).value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, spec).value,
        error = animateColorAsState(target.error, spec).value,
        onError = animateColorAsState(target.onError, spec).value,
        errorContainer = animateColorAsState(target.errorContainer, spec).value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec).value,
        background = animateColorAsState(target.background, spec).value,
        onBackground = animateColorAsState(target.onBackground, spec).value,
        surface = animateColorAsState(target.surface, spec).value,
        onSurface = animateColorAsState(target.onSurface, spec).value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec).value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec).value,
        outline = animateColorAsState(target.outline, spec).value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec).value,
        inverseSurface = animateColorAsState(target.inverseSurface, spec).value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, spec).value,
        inversePrimary = animateColorAsState(target.inversePrimary, spec).value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec).value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec).value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec).value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec).value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec).value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec).value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec).value,
    )
}
