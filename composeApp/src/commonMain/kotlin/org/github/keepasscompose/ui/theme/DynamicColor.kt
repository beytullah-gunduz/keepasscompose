package org.github.keepasscompose.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns a platform-specific dynamic color scheme if available (e.g., Material You on Android 12+),
 * or null to indicate the static scheme should be used instead.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
