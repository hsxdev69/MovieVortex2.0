package com.movievortex.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VortexColors {
    val PureBlack = Color(0xFF000000)
    val SecondaryBackground = Color(0xFF0D0D11)
    val DarkGraphite = Color(0xFF16161C)
    val CardSurface = Color(0xFF1C1C24)
    val BorderSubtle = Color(0xFF262630)
    val Divider = Color(0xFF2A2A35)

    val PrimaryText = Color(0xFFFFFFFF)
    val SecondaryText = Color(0xFFB5B5C3)
    val MutedText = Color(0xFF757585)

    // Accents
    val CinematicRed = Color(0xFFE50914)
    val CinematicRedGlow = Color(0x66E50914)
    val SubtlePurple = Color(0xFF8B5CF6)
    val SubtlePurpleGlow = Color(0x4D8B5CF6)
    val CoolBlue = Color(0xFF3B82F6)
    val CoolBlueGlow = Color(0x4D3B82F6)
    val EmeraldGreen = Color(0xFF10B981)
    val WarmAmber = Color(0xFFF59E0B)

    // Skeleton
    val SkeletonDark = Color(0xFF1A1A22)
    val SkeletonShimmer = Color(0xFF2C2C38)

    // Gradients
    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x66000000),
            Color(0xCC000000),
            Color(0xFF000000)
        )
    )

    val TopBarGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xCC000000),
            Color(0x66000000),
            Color.Transparent
        )
    )

    val SplashAuraGradient = Brush.radialGradient(
        colors = listOf(
            Color(0x80E50914),
            Color(0x408B5CF6),
            Color(0x203B82F6),
            Color.Transparent
        )
    )

    val AccentSelectionGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFE50914),
            Color(0xFF8B5CF6)
        )
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = VortexColors.CinematicRed,
    onPrimary = VortexColors.PrimaryText,
    secondary = VortexColors.SubtlePurple,
    onSecondary = VortexColors.PrimaryText,
    tertiary = VortexColors.CoolBlue,
    background = VortexColors.PureBlack,
    onBackground = VortexColors.PrimaryText,
    surface = VortexColors.DarkGraphite,
    onSurface = VortexColors.PrimaryText,
    surfaceVariant = VortexColors.CardSurface,
    onSurfaceVariant = VortexColors.SecondaryText,
    outline = VortexColors.BorderSubtle
)

@Composable
fun MovieVortexTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
