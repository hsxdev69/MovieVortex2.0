package com.movievortex.android.ui.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.movievortex.android.controller.PlayerController
import com.movievortex.android.controller.PlayerStatus
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.model.StreamSource
import com.movievortex.android.ui.components.VortexIcons
import com.movievortex.android.ui.theme.VortexColors
import kotlinx.coroutines.delay

@Composable
fun MovieVortexPlayerScreen(
    playerController: PlayerController,
    media: MovieVortexMedia? = null,
    recommendations: List<MovieVortexMedia> = emptyList(),
    onMediaClick: ((MovieVortexMedia) -> Unit)? = null,
    onCloseClick: () -> Unit,
    onPlayOnTvClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by playerController.uiState.collectAsState()
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showQualityPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var selectedServerName by remember { mutableStateOf("Turbo") }

    val activity = context as? Activity

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onCloseClick()
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.release()
            } catch (_: Exception) {}
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Attach stream source to ExoPlayer
    LaunchedEffect(state.currentSource) {
        state.currentSource?.let { source ->
            try {
                selectedServerName = source.extractorName.ifBlank { "Turbo" }
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

    // If Fullscreen mode is engaged
    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { playerController.toggleControlsVisibility() }
            )

            // Fullscreen Overlay Controls
            AnimatedVisibility(
                visible = state.isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PlayerControlsOverlay(
                    title = state.mediaTitle.ifBlank { media?.title ?: "MovieVortex" },
                    subtitle = state.mediaSubtitle,
                    isPlaying = isPlaying,
                    currentPos = state.currentPositionMs,
                    duration = state.durationMs,
                    onPlayPause = {
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                    onSeek = { frac ->
                        val dur = exoPlayer.duration.coerceAtLeast(0L)
                        val newPos = (frac * dur).toLong()
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, dur)
                    },
                    onRewind = {
                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, exoPlayer.duration.coerceAtLeast(0L))
                    },
                    onForward = {
                        val dur = exoPlayer.duration.coerceAtLeast(0L)
                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, dur)
                    },
                    onToggleFullscreen = {
                        isFullscreen = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    isFullscreen = true,
                    onBackClick = {
                        isFullscreen = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    onSubtitleClick = { showSubtitlePicker = !showSubtitlePicker },
                    onQualityClick = { showQualityPicker = !showQualityPicker }
                )
            }
        }
        return
    }

    // Normal Integrated View (Screenshot_20261014_180754.jpg)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
    ) {
        // 1. Embedded 16:9 Video Player View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { playerController.toggleControlsVisibility() }
            )

            // Loading / Resolving state indicator
            if (state.status == PlayerStatus.ResolvingStreams) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Embedded Overlay Controls
            AnimatedVisibility(
                visible = state.isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PlayerControlsOverlay(
                    title = state.mediaTitle.ifBlank { media?.title ?: "MovieVortex" },
                    subtitle = state.mediaSubtitle,
                    isPlaying = isPlaying,
                    currentPos = state.currentPositionMs,
                    duration = state.durationMs,
                    onPlayPause = {
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    },
                    onSeek = { frac ->
                        val dur = exoPlayer.duration.coerceAtLeast(0L)
                        val newPos = (frac * dur).toLong()
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, dur)
                    },
                    onRewind = {
                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, exoPlayer.duration.coerceAtLeast(0L))
                    },
                    onForward = {
                        val dur = exoPlayer.duration.coerceAtLeast(0L)
                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(dur)
                        exoPlayer.seekTo(newPos)
                        playerController.updateProgress(newPos, dur)
                    },
                    onToggleFullscreen = {
                        isFullscreen = true
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    isFullscreen = false,
                    onBackClick = {
                        try { exoPlayer.stop() } catch (_: Exception) {}
                        onCloseClick()
                    },
                    onSubtitleClick = { showSubtitlePicker = !showSubtitlePicker },
                    onQualityClick = { showQualityPicker = !showQualityPicker }
                )
            }
        }

        // 2. Scrollable Detail & Server Section Below Player
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Title, Rating, Year
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    val displayTitle = state.mediaTitle.ifBlank { media?.title ?: "Dune: Part Two" }
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ratingValue = media?.rating ?: 8.4f
                        Text(
                            text = "★ %.1f".format(ratingValue),
                            color = Color(0xFFFFC107),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = media?.year?.let { "$it" } ?: "2024",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row (Add to List, Play on TV, Share, Report, Download)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            ActionPillButton(
                                icon = VortexIcons.BookmarkBorder,
                                label = "Add to List",
                                onClick = {
                                    Toast.makeText(context, "Added to My List", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            ActionPillButton(
                                icon = VortexIcons.Cast,
                                label = "Play on TV",
                                onClick = {
                                    onPlayOnTvClick?.invoke() ?: run {
                                        Toast.makeText(context, "Scanning for TV displays...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        item {
                            ActionPillButton(
                                icon = VortexIcons.Share,
                                label = "Share",
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, displayTitle)
                                        putExtra(Intent.EXTRA_TEXT, "Watch $displayTitle on MovieVortex!")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                                }
                            )
                        }
                        item {
                            ActionPillButton(
                                icon = VortexIcons.Report,
                                label = "Report",
                                onClick = {
                                    Toast.makeText(context, "Feedback reported. Thank you!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            ActionPillButton(
                                icon = VortexIcons.Download,
                                label = "Download",
                                onClick = {
                                    Toast.makeText(context, "Download queued", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Server Selection Section (Screenshot_20261014_180754.jpg)
                    Text(
                        text = "If current server is not working, try a different one:",
                        color = Color(0xFFDDDDDD),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Servers Row: Turbo, Spiral, Rive, Core, AniVortex, IStreamFlare, MovieBox
                    val dynamicServers = if (state.streamSources.isNotEmpty()) {
                        state.streamSources.map { it.extractorName.ifBlank { it.quality } }.distinct()
                    } else {
                        listOf("Turbo", "Spiral", "Rive", "Core")
                    }
                    val allServers = (dynamicServers + listOf("AniVortex", "IStreamFlare", "MovieBox")).distinct()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Servers :",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allServers) { serverName ->
                                val isSelected = selectedServerName.equals(serverName, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color.White else Color(0xFF222222))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else Color(0xFF333333),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            selectedServerName = serverName
                                            val matchedSource = state.streamSources.firstOrNull {
                                                it.extractorName.equals(serverName, ignoreCase = true)
                                            }
                                            if (matchedSource != null) {
                                                playerController.selectSource(matchedSource)
                                            } else {
                                                Toast.makeText(context, "Switched to $serverName", Toast.LENGTH_SHORT).show()
                                                playerController.retryPlayback()
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = serverName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Comments Section Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Comments",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0",
                            color = Color(0xFFAAAAAA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "No comments yet",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to share your thoughts!",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // You May Also Like Header
                    Text(
                        text = "You May Also Like",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Recommendations Grid (2 columns)
            val fallbackRecommendations = listOf(
                MovieVortexMedia(
                    id = "dune-1",
                    title = "Dune: Part One",
                    poster = "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",
                    year = 2021,
                    rating = 8.0f
                ),
                MovieVortexMedia(
                    id = "eeaao",
                    title = "Everything Everywhere All at Once",
                    poster = "https://image.tmdb.org/t/p/w500/w3LxiVYPqEIIAqocRGSYQJzag4T.jpg",
                    year = 2022,
                    rating = 7.7f
                ),
                MovieVortexMedia(
                    id = "dawn-apes",
                    title = "Dawn of the Planet of the Apes",
                    poster = "https://image.tmdb.org/t/p/w500/kScdOzMeqm79jA280vjDkL6aWf3.jpg",
                    year = 2014,
                    rating = 7.6f
                ),
                MovieVortexMedia(
                    id = "wandering-earth",
                    title = "The Wandering Earth II",
                    poster = "https://image.tmdb.org/t/p/w500/4n743bJ3nC6c5XbXvT4d0eK3g1f.jpg",
                    year = 2023,
                    rating = 7.2f
                )
            )

            val displayList = if (recommendations.isNotEmpty()) recommendations else fallbackRecommendations
            val chunked = displayList.chunked(2)

            items(chunked) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        RecommendationCard(
                            media = item,
                            badge = if (item.id.contains("dune")) "MULTI" else if (item.id.contains("eeaao")) "ORI" else "DUAL",
                            onClick = { onMediaClick?.invoke(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecommendationCard(
    media: MovieVortexMedia,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222))
        ) {
            AsyncImage(
                model = media.poster,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Badge (MULTI / ORI / DUAL)
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = media.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Movie • ${media.year ?: 2024}",
                color = Color(0xFF888888),
                fontSize = 11.sp
            )
            media.rating?.let { r ->
                Text(
                    text = "★ %.1f".format(r),
                    color = Color(0xFFFFC107),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    currentPos: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isFullscreen: Boolean,
    onBackClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onQualityClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
    ) {
        // Top Controls Bar (Back, Title, CC, Tune)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = VortexIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            IconButton(onClick = onSubtitleClick) {
                Icon(
                    imageVector = VortexIcons.Subtitles,
                    contentDescription = "Subtitles",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(onClick = onQualityClick) {
                Icon(
                    imageVector = VortexIcons.Tune,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Center Controls (Prev/Rewind, Play/Pause, Next/Forward)
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRewind) {
                Icon(
                    imageVector = VortexIcons.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) VortexIcons.Pause else VortexIcons.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = onForward) {
                Icon(
                    imageVector = VortexIcons.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom Timeline Bar (0:00, seekbar, duration, PIP, Fullscreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPos),
                color = Color.White,
                fontSize = 11.sp
            )

            val progressFraction = if (duration > 0) {
                (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x66FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Text(
                text = formatTime(duration),
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )

            IconButton(onClick = { /* PIP */ }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = VortexIcons.PictureInPicture,
                    contentDescription = "PIP",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isFullscreen) VortexIcons.FullscreenExit else VortexIcons.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
