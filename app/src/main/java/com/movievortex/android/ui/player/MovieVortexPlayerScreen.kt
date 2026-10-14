package com.movievortex.android.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movievortex.android.ui.components.VortexIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.movievortex.android.controller.PlayerController
import com.movievortex.android.controller.PlayerStatus
import com.movievortex.android.ui.theme.VortexColors
import kotlinx.coroutines.delay

@Composable
fun MovieVortexPlayerScreen(
    playerController: PlayerController,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by playerController.uiState.collectAsState()
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showQualityPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Attach stream source to ExoPlayer
    LaunchedEffect(state.currentSource) {
        state.currentSource?.let { source ->
            try {
                val mediaItem = MediaItem.fromUri(source.url)
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                if (source.headers.isNotEmpty()) {
                    dataSourceFactory.setDefaultRequestProperties(source.headers)
                }
                val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(mediaItem)

                exoPlayer.setMediaSource(mediaSource)
                if (state.currentPositionMs > 0) {
                    exoPlayer.seekTo(state.currentPositionMs)
                }
                exoPlayer.prepare()
                exoPlayer.play()
                isPlaying = true
            } catch (_: Exception) {
                try {
                    val mediaItem = MediaItem.fromUri(source.url)
                    exoPlayer.setMediaItem(mediaItem)
                    if (state.currentPositionMs > 0) {
                        exoPlayer.seekTo(state.currentPositionMs)
                    }
                    exoPlayer.prepare()
                    exoPlayer.play()
                    isPlaying = true
                } catch (_: Exception) {}
            }
        }
    }

    // Periodic progress syncing with controller
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(500)
            if (exoPlayer.isPlaying || exoPlayer.currentPosition > 0) {
                val currentPos = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                playerController.updateProgress(currentPos, dur)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .clickable { playerController.toggleControlsVisibility() }
    ) {
        // Video Viewport (Full viewport)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (state.currentSource != null && state.status != PlayerStatus.ResolvingStreams && state.status != PlayerStatus.Error) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            when (state.status) {
                PlayerStatus.ResolvingStreams -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = VortexColors.CinematicRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = state.statusMessage,
                            color = VortexColors.PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                PlayerStatus.Error -> {
                    // PRD Rule 33: Never expose technical errors to normal users
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "This content is currently unavailable.",
                            color = VortexColors.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please try again.",
                            color = VortexColors.SecondaryText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { playerController.retryPlayback() },
                                colors = ButtonDefaults.buttonColors(containerColor = VortexColors.CinematicRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                            Button(
                                onClick = onCloseClick,
                                colors = ButtonDefaults.buttonColors(containerColor = VortexColors.CardSurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back", color = Color.White)
                            }
                        }
                    }
                }

                else -> {
                    // Video Content rendered here
                    // If offline, display emerald offline indicator
                    if (state.isOfflinePlayback) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .background(Color(0x3310B981), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = VortexIcons.WifiOff,
                                    contentDescription = "Offline",
                                    tint = VortexColors.EmeraldGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OFFLINE PLAYBACK",
                                    color = VortexColors.EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay Player Controls
        AnimatedVisibility(
            visible = state.isControlsVisible && state.status == PlayerStatus.Ready,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x73000000))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        try { exoPlayer.stop() } catch (_: Exception) {}
                        onCloseClick()
                    }) {
                        Icon(
                            imageVector = VortexIcons.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.mediaTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (state.mediaSubtitle.isNotBlank()) {
                            Text(
                                text = state.mediaSubtitle,
                                color = VortexColors.SecondaryText,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Quality Picker Button
                    IconButton(onClick = { showQualityPicker = !showQualityPicker }) {
                        Icon(
                            imageVector = VortexIcons.HighQuality,
                            contentDescription = "Quality",
                            tint = Color.White
                        )
                    }

                    // Subtitles Button
                    IconButton(onClick = { showSubtitlePicker = !showSubtitlePicker }) {
                        Icon(
                            imageVector = VortexIcons.Subtitles,
                            contentDescription = "Subtitles",
                            tint = Color.White
                        )
                    }
                }

                // Center Play/Pause & Seek Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            playerController.updateProgress(newPos, exoPlayer.duration.coerceAtLeast(0L))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(VortexColors.CinematicRed)
                            .clickable {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) VortexIcons.Pause else VortexIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                            exoPlayer.seekTo(newPos)
                            playerController.updateProgress(newPos, dur)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Forward10,
                            contentDescription = "Fast Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(state.durationMs),
                            color = VortexColors.SecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    val progressFraction = if (state.durationMs > 0) {
                        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            val dur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (frac * dur).toLong()
                            exoPlayer.seekTo(newPos)
                            playerController.updateProgress(newPos, dur)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = VortexColors.CinematicRed,
                            activeTrackColor = VortexColors.CinematicRed,
                            inactiveTrackColor = Color(0x66FFFFFF)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
