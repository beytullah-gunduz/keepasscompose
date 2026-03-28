package org.github.keepasscompose.ui.theme

import kotlin.math.pow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Minimum touch target size for interactive elements on mobile (WCAG 2.5.8).
 */
val MinTouchTargetSize = 48.dp

/**
 * Modifier that ensures minimum touch target size for interactive elements.
 */
fun Modifier.accessibleTouchTarget(): Modifier =
    defaultMinSize(minWidth = MinTouchTargetSize, minHeight = MinTouchTargetSize)

/**
 * Modifier that marks a composable as a heading for screen readers.
 */
fun Modifier.accessibleHeading(): Modifier =
    semantics { heading() }

/**
 * Modifier that provides a custom state description for screen readers.
 * Useful for toggle states, loading states, etc.
 */
fun Modifier.accessibleState(description: String): Modifier =
    semantics { stateDescription = description }

/**
 * WCAG AA contrast ratio threshold for normal text (4.5:1).
 */
const val WCAG_AA_NORMAL_TEXT_RATIO = 4.5

/**
 * WCAG AA contrast ratio threshold for large text (3:1).
 */
const val WCAG_AA_LARGE_TEXT_RATIO = 3.0

/**
 * Calculates the relative luminance of a color per WCAG 2.0.
 */
fun relativeLuminance(red: Float, green: Float, blue: Float): Double {
    fun linearize(c: Float): Double {
        val d = c.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
}

/**
 * Calculates the contrast ratio between two luminance values per WCAG 2.0.
 */
fun contrastRatio(luminance1: Double, luminance2: Double): Double {
    val lighter = maxOf(luminance1, luminance2)
    val darker = minOf(luminance1, luminance2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Checks whether two colors meet WCAG AA contrast requirements for normal text.
 */
fun meetsWcagAA(
    fgRed: Float, fgGreen: Float, fgBlue: Float,
    bgRed: Float, bgGreen: Float, bgBlue: Float,
): Boolean {
    val fgLum = relativeLuminance(fgRed, fgGreen, fgBlue)
    val bgLum = relativeLuminance(bgRed, bgGreen, bgBlue)
    return contrastRatio(fgLum, bgLum) >= WCAG_AA_NORMAL_TEXT_RATIO
}
