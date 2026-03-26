package org.github.keepasscompose.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class FontScale(val factor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.2f),
}

val LocalFontScale = compositionLocalOf { FontScale.MEDIUM }

@Composable
fun ProvideFontScale(fontScale: FontScale, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFontScale provides fontScale, content = content)
}

fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge = displayLarge.scaledStyle(factor),
    displayMedium = displayMedium.scaledStyle(factor),
    displaySmall = displaySmall.scaledStyle(factor),
    headlineLarge = headlineLarge.scaledStyle(factor),
    headlineMedium = headlineMedium.scaledStyle(factor),
    headlineSmall = headlineSmall.scaledStyle(factor),
    titleLarge = titleLarge.scaledStyle(factor),
    titleMedium = titleMedium.scaledStyle(factor),
    titleSmall = titleSmall.scaledStyle(factor),
    bodyLarge = bodyLarge.scaledStyle(factor),
    bodyMedium = bodyMedium.scaledStyle(factor),
    bodySmall = bodySmall.scaledStyle(factor),
    labelLarge = labelLarge.scaledStyle(factor),
    labelMedium = labelMedium.scaledStyle(factor),
    labelSmall = labelSmall.scaledStyle(factor),
)

private fun TextStyle.scaledStyle(factor: Float): TextStyle = copy(
    fontSize = fontSize.scaled(factor),
    lineHeight = lineHeight.scaled(factor),
)

private fun TextUnit.scaled(factor: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else (this.value * factor).sp
