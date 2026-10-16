package com.movievortex.android.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun MovieVortexSplashScreen(
    onSplashFinished: () -> Unit
) {
    // Exact Figma Screen 1: Angled 3D Poster Wall + Bottom Sheet Card
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
    ) {
        // 1. Tilted Poster Wall Collage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.75f)
                .graphicsLayer {
                    rotationZ = -8f
                    scaleX = 1.25f
                    scaleY = 1.25f
                    translationY = -80f
                }
        ) {
            PosterWallCollage()
        }

        // Dark gradient overlay over the poster wall fading to bottom sheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x660A0A0E),
                            Color(0xDD0A0A0E),
                            Color(0xFF0A0A0E)
                        ),
                        startY = 150f
                    )
                )
        )

        // 2. Bottom Sheet Card (Matching Screen 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xF0141418))
                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title: Your Entertainment Starts Right Here Today
                Text(
                    text = """Your Entertainment
Starts Right Here Today""",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subtitle
                Text(
                    text = """Discover movies, series, sports, and Originals
Stream anytime, anywhere effortlessly.""",
                    color = Color(0xFF9E9EA8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Primary Golden Pill Button: Get Started
                Button(
                    onClick = onSplashFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VortexColors.GoldenAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Outlined Pill Button: Sign In
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                        .clickable { onSplashFinished() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Home indicator pill bar
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x40FFFFFF))
                )
            }
        }
    }
}

@Composable
private fun PosterWallCollage() {
    val samplePostersCol1 = listOf(
        "https://image.tmdb.org/t/p/w500/9PFonB99qQeFLMFiRqfl97udDr4.jpg", // Wednesday
        "https://image.tmdb.org/t/p/w500/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg", // Kung Fu Panda
        "https://image.tmdb.org/t/p/w500/1E5baAaEse26fej7uHcjOgEE2t2.jpg"  // Fast X
    )
    val samplePostersCol2 = listOf(
        "https://image.tmdb.org/t/p/w500/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg", // Dune 2
        "https://image.tmdb.org/t/p/w500/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg", // Avengers
        "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg"  // Oppenheimer
    )
    val samplePostersCol3 = listOf(
        "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg", // Wicked
        "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg", // Dark Knight
        "https://image.tmdb.org/t/p/w500/6KErczPBROQty7QoIsxGmTMxbg0.jpg"  // Mulan
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            samplePostersCol1.forEach { url ->
                PosterCell(url)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(y = (-30).dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            samplePostersCol2.forEach { url ->
                PosterCell(url)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            samplePostersCol3.forEach { url ->
                PosterCell(url)
            }
        }
    }
}

@Composable
private fun PosterCell(url: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E26))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
