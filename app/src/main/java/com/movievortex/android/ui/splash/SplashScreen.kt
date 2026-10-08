package com.movievortex.android.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movievortex.android.ui.components.MovieVortexMark
import com.movievortex.android.ui.theme.VortexColors
import kotlinx.coroutines.delay

@Composable
fun MovieVortexSplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation stages: Black -> Aura -> Logo -> Wordmark -> Sweep -> Fade
    val auraAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val sweepPosition = remember { Animatable(-300f) }
    val fadeOutAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Step 1: Initial Black hold
        delay(200)

        // Step 2: Aura appears
        auraAlpha.animateTo(1f, tween(600, easing = LinearEasing))

        // Step 3: MovieVortex logo appears with subtle expansion
        logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))

        // Step 4: Wordmark appears
        wordmarkAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))

        // Step 5: Thin cinematic light sweep across brand mark
        sweepPosition.animateTo(600f, tween(700, easing = FastOutSlowInEasing))

        // Hold majestic state briefly
        delay(400)

        // Step 6: Fade into application
        fadeOutAlpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .alpha(fadeOutAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Background atmospheric aura (Red / Purple / Blue glowing radial)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width * 0.85f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VortexColors.CinematicRedGlow.copy(alpha = 0.45f * auraAlpha.value),
                        VortexColors.SubtlePurpleGlow.copy(alpha = 0.35f * auraAlpha.value),
                        VortexColors.CoolBlueGlow.copy(alpha = 0.20f * auraAlpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand mark with scale & alpha
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                MovieVortexMark(size = 96.dp, showGlow = true)

                // Light sweep across logo
                Canvas(modifier = Modifier.size(96.dp)) {
                    val sweepX = sweepPosition.value
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            start = Offset(sweepX - 40f, 0f),
                            end = Offset(sweepX + 40f, size.height)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cinematic Wordmark
            Column(
                modifier = Modifier.alpha(wordmarkAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MOVIEVORTEX",
                    color = VortexColors.PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NEXT-GENERATION STREAMING",
                    color = VortexColors.SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
