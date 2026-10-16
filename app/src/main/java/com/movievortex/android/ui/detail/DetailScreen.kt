package com.movievortex.android.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.movievortex.android.controller.DetailController
import com.movievortex.android.model.MediaEpisode
import com.movievortex.android.model.MediaType
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.ui.components.DisneyPlusBadgeCard
import com.movievortex.android.ui.components.NetflixBadgeCard
import com.movievortex.android.ui.components.PrimeVideoBadgeCard
import com.movievortex.android.ui.components.PosterCard
import com.movievortex.android.ui.components.VortexIcons
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun DetailScreen(
    detailController: DetailController,
    onPlayMovie: (MovieVortexMedia) -> Unit,
    onPlayEpisode: (MovieVortexMedia, MediaEpisode) -> Unit,
    onDownloadClick: (MovieVortexMedia, MediaEpisode?) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by detailController.uiState.collectAsState()
    val media = state.media
    var selectedCategory by remember { mutableStateOf("Recent") }
    var scrubberPosition by remember { mutableFloatStateOf(0.45f) }

    if (state.isLoading && media == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VortexColors.AppBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = VortexColors.GoldenAmber,
                strokeWidth = 3.dp
            )
        }
        return
    }

    if (media == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VortexColors.AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.errorMessage ?: "Content details unavailable",
                    color = VortexColors.PrimaryText,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = VortexColors.GoldenAmber)
                ) {
                    Text("Go Back", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Top Video Player Area (Matching Screen 3)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                        .clickable { onPlayMovie(media) }
                ) {
                    // Video Viewport Backdrop Image
                    AsyncImage(
                        model = media.backdrop ?: media.poster,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cinematic Gradient Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x99000000),
                                        Color.Transparent,
                                        Color(0xBB000000)
                                    )
                                )
                            )
                    )

                    // Top Back Chevron Button
                    Box(
                        modifier = Modifier
                            .padding(top = 36.dp, start = 16.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VortexIcons.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center Play Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .border(1.dp, Color(0x60FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VortexIcons.Play,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Bottom Player Controls (Timestamps + Golden Scrubber + Action Icons)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "26:57",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Scrubber Line with Golden Amber glow and dot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x55FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(scrubberPosition)
                                        .height(3.dp)
                                        .background(VortexColors.GoldenAmber)
                                )
                            }

                            Text(
                                text = "-32:49",
                                color = Color(0xFFB5B5C3),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Action Icons: Subtitles, Cast, Fullscreen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = VortexIcons.Subtitles,
                                    contentDescription = "Subtitles",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    imageVector = VortexIcons.Cast,
                                    contentDescription = "Cast",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    imageVector = VortexIcons.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Rounded Dark Sheet Below Player (Screen 3)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF121217))
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    // Category Filter Pills: Recent, Movies, Sports, Reality
                    val categories = listOf("Recent", "Movies", "Sports", "Reality")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) VortexColors.GoldenAmber else Color(0x3325252E))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else Color(0x28FFFFFF),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Streaming Badges Row: Disney+, NETFLIX, prime video
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item { DisneyPlusBadgeCard() }
                        item { NetflixBadgeCard() }
                        item { PrimeVideoBadgeCard() }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title & Actions Row (Watch Now + In-App Download)
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${media.year ?: 2026} • ★ %.1f".format(media.rating ?: 8.2f),
                                color = VortexColors.GoldenAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ${media.providerApiName}",
                                color = Color(0xFF8E8E9A),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Watch Now & Download Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (media.type == MediaType.TvSeries || media.type == MediaType.Anime) {
                                        state.currentSeasonEpisodes.firstOrNull()?.let { ep ->
                                            onPlayEpisode(media, ep)
                                        } ?: onPlayMovie(media)
                                    } else {
                                        onPlayMovie(media)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VortexColors.GoldenAmber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(
                                    imageVector = VortexIcons.Play,
                                    contentDescription = "Watch Now",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Now",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (media.type == MediaType.TvSeries || media.type == MediaType.Anime) {
                                        state.currentSeasonEpisodes.firstOrNull()?.let { ep ->
                                            onDownloadClick(media, ep)
                                        } ?: onDownloadClick(media, null)
                                    } else {
                                        onDownloadClick(media, null)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x3325252E)),
                                border = BorderStroke(1.dp, VortexColors.GoldenAmber),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(
                                    imageVector = VortexIcons.Download,
                                    contentDescription = "Download Movie",
                                    tint = VortexColors.GoldenAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download",
                                    color = VortexColors.GoldenAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Section Header: "Watch TV Shows" on left, "See All" on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Watch TV Shows",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See All",
                            color = Color(0xFF8E8E9A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Cards Carousel (Screen 3: Badlands, Turok, etc.)
                    val sampleShows = listOf(
                        Pair("BADLANDS", "https://image.tmdb.org/t/p/w500/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg"),
                        Pair("TUROK ORIGINS (2024)", "https://image.tmdb.org/t/p/w500/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg"),
                        Pair("PREDATOR HUNT", "https://image.tmdb.org/t/p/w500/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg")
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sampleShows) { (title, posterUrl) ->
                            Column(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onPlayMovie(media) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 10f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E1E24))
                                ) {
                                    AsyncImage(
                                        model = posterUrl,
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Episodes Section if Series or Anime
                    if (media.type == MediaType.TvSeries || media.type == MediaType.Anime) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Episodes (${state.currentSeasonEpisodes.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        state.currentSeasonEpisodes.forEach { ep ->
                            EpisodeItemRow(
                                episode = ep,
                                onPlay = { onPlayEpisode(media, ep) },
                                onDownload = { onDownloadClick(media, ep) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeItemRow(
    episode: MediaEpisode,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A22))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x33F5A524)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = VortexIcons.Play,
                contentDescription = "Play Episode",
                tint = VortexColors.GoldenAmber,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title ?: "Episode ${episode.episodeNumber}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Episode ${episode.episodeNumber}",
                color = Color(0xFF8E8E9A),
                fontSize = 11.sp
            )
        }

        IconButton(onClick = onDownload) {
            Icon(
                imageVector = VortexIcons.Download,
                contentDescription = "Download Episode",
                tint = VortexColors.GoldenAmber,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
