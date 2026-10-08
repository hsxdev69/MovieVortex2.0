package com.movievortex.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun PosterSkeleton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    aspectRatio: Float = 2f / 3f
) {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")

    // Silver shimmer translation
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    // Subtle breathing animation for centered logo
    val breathScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            VortexColors.SkeletonDark,
            VortexColors.SkeletonShimmer,
            Color(0xFF383848),
            VortexColors.SkeletonShimmer,
            VortexColors.SkeletonDark
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim + 200f, translateAnim + 200f)
    )

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush),
        contentAlignment = Alignment.Center
    ) {
        // Centered MovieVortex logo at ~1/3 width with breathing animation
        Box(
            modifier = Modifier.fillMaxWidth(0.35f * breathScale),
            contentAlignment = Alignment.Center
        ) {
            MovieVortexMark(
                size = 38.dp,
                showGlow = false
            )
        }
    }
}
