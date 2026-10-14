package com.movievortex.android.ui.detail

import androidx.compose.animation.AnimatedVisibility
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
import com.movievortex.android.ui.components.VortexIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    if (state.isLoading && media == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VortexColors.PureBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = VortexColors.CinematicRed,
                strokeWidth = 3.dp
            )
        }
        return
    }

    if (media == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VortexColors.PureBlack),
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
                    colors = ButtonDefaults.buttonColors(containerColor = VortexColors.CinematicRed)
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
    ) {
        // 1. Cinematic Backdrop Top Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = media.backdrop ?: media.poster,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VortexColors.HeroGradient)
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 12.dp)
                        .background(Color(0x88000000), CircleShape)
                ) {
                    Icon(
                        imageVector = VortexIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. Title, Metadata & Action Buttons
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Media Type Pill
                val typeLabel = when (media.type) {
                    MediaType.Movie -> "MOVIE"
                    MediaType.TvSeries -> "TV SERIES"
                    MediaType.Anime -> "ANIME"
                    else -> "STREAM"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(VortexColors.CinematicRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    media.rating?.let { rating ->
                        Text(
                            text = "★ %.1f".format(rating),
                            color = VortexColors.WarmAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    media.year?.let { year ->
                        Text(
                            text = year.toString(),
                            color = VortexColors.SecondaryText,
                            fontSize = 13.sp
                        )
                    }

                    media.runtime?.let { runtime ->
                        Text(
                            text = "$runtime min",
                            color = VortexColors.MutedText,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = media.title,
                    color = VortexColors.PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row: Play, My List, Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VortexColors.CinematicRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Play,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // My List Button
                    OutlinedButton(
                        onClick = { detailController.toggleMyList() },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VortexColors.BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VortexColors.CardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isInMyList) VortexIcons.Check else VortexIcons.BookmarkBorder,
                            contentDescription = "My List",
                            tint = if (state.isInMyList) VortexColors.EmeraldGreen else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isInMyList) "Saved" else "My List",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Download Button
                    OutlinedButton(
                        onClick = {
                            onDownloadClick(media, state.currentSeasonEpisodes.firstOrNull())
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VortexColors.BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = VortexColors.CardSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Download,
                            contentDescription = "Download",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Synopsis
                media.synopsis?.let { synopsis ->
                    Text(
                        text = synopsis,
                        color = VortexColors.SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Genres Pills
                if (media.genres.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(media.genres) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VortexColors.CardSurface)
                                    .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = genre,
                                    color = VortexColors.SecondaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 3. Seasons & Episodes Section (for TV Series & Anime)
        if (media.seasons.isNotEmpty() || media.episodes.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Episodes",
                        color = VortexColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Season Selector Pills (if multiple seasons)
                    if (media.seasons.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(media.seasons) { season ->
                                val isSelected = season.seasonNumber == state.selectedSeason
                                val bg = if (isSelected) VortexColors.CinematicRed else VortexColors.CardSurface
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { detailController.selectSeason(season.seasonNumber) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = season.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Episode List Items
            items(state.currentSeasonEpisodes) { episode ->
                EpisodeListItem(
                    episode = episode,
                    onClick = { onPlayEpisode(media, episode) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 4. Cast Section (if available)
        if (media.cast.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Top Cast",
                        color = VortexColors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(media.cast.take(12)) { castMember ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(72.dp)
                        ) {
                            AsyncImage(
                                model = castMember.avatarUrl,
                                contentDescription = castMember.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(VortexColors.DarkGraphite)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = castMember.name,
                                color = VortexColors.PrimaryText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun EpisodeListItem(
    episode: MediaEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VortexColors.CardSurface)
            .border(0.5.dp, VortexColors.BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode Thumbnail / Number Box
        Box(
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(VortexColors.DarkGraphite)
        ) {
            if (episode.poster != null) {
                AsyncImage(
                    model = episode.poster,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EP ${episode.episodeNumber}",
                        color = VortexColors.SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = VortexIcons.Play,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Watch progress bar
            if (episode.watchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x66FFFFFF))
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(episode.watchProgress)
                            .height(3.dp)
                            .background(VortexColors.CinematicRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Episode Metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    color = VortexColors.PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (episode.isWatched) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0x3310B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WATCHED",
                            color = VortexColors.EmeraldGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            episode.synopsis?.let { synopsis ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = synopsis,
                    color = VortexColors.MutedText,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
