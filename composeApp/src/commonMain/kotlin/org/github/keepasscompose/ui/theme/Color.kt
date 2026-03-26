package org.github.keepasscompose.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary – deep blue conveying trust and security
private val PrimaryLight = Color(0xFF2C5F8A)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFD0E4FF)
private val OnPrimaryContainerLight = Color(0xFF001D36)

// Secondary – teal accent
private val SecondaryLight = Color(0xFF4E6356)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFD0E8D7)
private val OnSecondaryContainerLight = Color(0xFF0B1F15)

// Tertiary – warm accent for visual variety
private val TertiaryLight = Color(0xFF6B5778)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFF3DAFF)
private val OnTertiaryContainerLight = Color(0xFF251431)

// Error
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

// Neutral surfaces
private val BackgroundLight = Color(0xFFF8F9FF)
private val OnBackgroundLight = Color(0xFF191C20)
private val SurfaceLight = Color(0xFFF8F9FF)
private val OnSurfaceLight = Color(0xFF191C20)
private val SurfaceVariantLight = Color(0xFFDFE2EB)
private val OnSurfaceVariantLight = Color(0xFF43474E)
private val OutlineLight = Color(0xFF73777F)
private val OutlineVariantLight = Color(0xFFC3C6CF)
private val SurfaceDimLight = Color(0xFFD8DAE0)
private val SurfaceBrightLight = Color(0xFFF8F9FF)
private val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
private val SurfaceContainerLowLight = Color(0xFFF2F3FA)
private val SurfaceContainerLight = Color(0xFFECEDF4)
private val SurfaceContainerHighLight = Color(0xFFE6E8EE)
private val SurfaceContainerHighestLight = Color(0xFFE1E2E8)

// Inverse
private val InverseSurfaceLight = Color(0xFF2E3135)
private val InverseOnSurfaceLight = Color(0xFFEFF0F7)
private val InversePrimaryLight = Color(0xFF9DCAFC)

// Dark theme colors
private val PrimaryDark = Color(0xFF9DCAFC)
private val OnPrimaryDark = Color(0xFF003258)
private val PrimaryContainerDark = Color(0xFF0F476F)
private val OnPrimaryContainerDark = Color(0xFFD0E4FF)

private val SecondaryDark = Color(0xFFB4CCBB)
private val OnSecondaryDark = Color(0xFF203529)
private val SecondaryContainerDark = Color(0xFF374B3F)
private val OnSecondaryContainerDark = Color(0xFFD0E8D7)

private val TertiaryDark = Color(0xFFD7BEE4)
private val OnTertiaryDark = Color(0xFF3B2948)
private val TertiaryContainerDark = Color(0xFF533F5F)
private val OnTertiaryContainerDark = Color(0xFFF3DAFF)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private val BackgroundDark = Color(0xFF111318)
private val OnBackgroundDark = Color(0xFFE1E2E8)
private val SurfaceDark = Color(0xFF111318)
private val OnSurfaceDark = Color(0xFFE1E2E8)
private val SurfaceVariantDark = Color(0xFF43474E)
private val OnSurfaceVariantDark = Color(0xFFC3C6CF)
private val OutlineDark = Color(0xFF8D9099)
private val OutlineVariantDark = Color(0xFF43474E)
private val SurfaceDimDark = Color(0xFF111318)
private val SurfaceBrightDark = Color(0xFF37393E)
private val SurfaceContainerLowestDark = Color(0xFF0C0E13)
private val SurfaceContainerLowDark = Color(0xFF191C20)
private val SurfaceContainerDark = Color(0xFF1D2024)
private val SurfaceContainerHighDark = Color(0xFF282A2F)
private val SurfaceContainerHighestDark = Color(0xFF33353A)

private val InverseSurfaceDark = Color(0xFFE1E2E8)
private val InverseOnSurfaceDark = Color(0xFF2E3135)
private val InversePrimaryDark = Color(0xFF2C5F8A)

val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        inverseSurface = InverseSurfaceLight,
        inverseOnSurface = InverseOnSurfaceLight,
        inversePrimary = InversePrimaryLight,
        surfaceDim = SurfaceDimLight,
        surfaceBright = SurfaceBrightLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
    )

val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        inverseSurface = InverseSurfaceDark,
        inverseOnSurface = InverseOnSurfaceDark,
        inversePrimary = InversePrimaryDark,
        surfaceDim = SurfaceDimDark,
        surfaceBright = SurfaceBrightDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
    )
