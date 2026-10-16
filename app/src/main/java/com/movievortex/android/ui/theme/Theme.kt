package com.movievortex.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VortexColors {
    val PureBlack = Color(0xFF000000)
    val AppBackground = Color(0xFF0A0A0E)
    val SecondaryBackground = Color(0xFF0D0D11)
    val DarkGraphite = Color(0xFF14141A)
    val CardSurface = Color(0xFF181820)
    val CardSurfaceElevated = Color(0xFF22222A)
    val BorderSubtle = Color(0xFF282834)
    val Divider = Color(0xFF242430)

    val PrimaryText = Color(0xFFFFFFFF)
    val SecondaryText = Color(0xFFB5B5C3)
    val MutedText = Color(0xFF8E8E9A)

    // Primary Brand Accent: Golden Amber (from Figma Screenshot)
    val GoldenAmber = Color(0xFFF5A524)
    val GoldenAmberDark = Color(0xFFE59819)
    val GoldenAmberGlow = Color(0x66F5A524)
    val AmberLight = Color(0xFFFFB800)

    // Secondary Accents
    val CinematicRed = Color(0xFFE50914)
    val CinematicRedGlow = Color(0x66E50914)
    val SubtlePurple = Color(0xFF8B5CF6)
    val SubtlePurpleGlow = Color(0x4D8B5CF6)
    val CoolBlue = Color(0xFF3B82F6)
    val CoolBlueGlow = Color(0x4D3B82F6)
    val EmeraldGreen = Color(0xFF10B981)
    val WarmAmber = Color(0xFFF5A524)

    // Skeleton
    val SkeletonDark = Color(0xFF181822)
    val SkeletonShimmer = Color(0xFF282836)

    // Gradients
    val GoldenGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFB800),
            Color(0xFFF5A524)
        )
    )

    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x66000000),
            Color(0xCC000000),
            Color(0xFF0A0A0E)
        )
    )

    val TopBarGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE0A0A0E),
            Color(0x990A0A0E),
            Color.Transparent
        )
    )

    val SplashAuraGradient = Brush.radialGradient(
        colors = listOf(
            Color(0x80F5A524),
            Color(0x408B5CF6),
            Color(0x203B82F6),
            Color.Transparent
        )
    )

    val AccentSelectionGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFB800),
            Color(0xFFF5A524)
        )
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = VortexColors.GoldenAmber,
    onPrimary = Color.Black,
    secondary = VortexColors.SubtlePurple,
    onSecondary = VortexColors.PrimaryText,
    tertiary = VortexColors.CoolBlue,
    background = VortexColors.AppBackground,
    onBackground = VortexColors.PrimaryText,
    surface = VortexColors.CardSurface,
    onSurface = VortexColors.PrimaryText,
    surfaceVariant = VortexColors.CardSurfaceElevated,
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
