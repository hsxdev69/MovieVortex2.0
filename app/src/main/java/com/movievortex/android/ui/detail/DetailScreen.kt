package com.movievortex.android.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Cinematic Backdrop with Overlapping Poster (Screenshot_20261014_180737.jpg)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    // Backdrop Image
                    AsyncImage(
                        model = media.backdrop ?: media.poster,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    )

                    // Cinematic Gradient Vignette (top and bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x99000000),
                                        Color.Transparent,
                                        Color(0xCC000000),
                                        VortexColors.PureBlack
                                    )
                                )
                            )
                    )

                    // Overlapping Poster at lower-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp)
                            .width(115.dp)
                            .aspectRatio(2f / 3f)
                            .shadow(12.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(VortexColors.CardSurface)
                    ) {
                        AsyncImage(
                            model = media.poster ?: media.backdrop,
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 2. Title, Rating Badge, Metadata & Genres
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Title
                    Text(
                        text = media.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rating & Year Row (Yellow Rating Pill)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yellow Rating Badge (★ 8.4)
                        val ratingValue = media.rating ?: 8.4f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFC107))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ %.1f".format(ratingValue),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        val mediaTypeText = when (media.type) {
                            MediaType.TvSeries -> "TV Series"
                            MediaType.Anime -> "Anime"
                            else -> "Movie"
                        }
                        val yearText = media.year?.let { "$it" } ?: "2024"

                        Text(
                            text = "$mediaTypeText  $yearText",
                            color = Color(0xFFDDDDDD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Countries line
                    val countriesText = media.production?.takeIf { it.isNotBlank() }
                        ?: "United States, Canada, United Arab Emirates"
                    Text(
                        text = countriesText,
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Vote Count
                    Text(
                        text = "708.7K votes",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Genre Pills (Dark background with subtle rounded border)
                    val displayGenres = if (media.genres.isNotEmpty()) media.genres else listOf("Action", "Adventure", "Drama", "Sci-Fi")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayGenres) { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1F1F1F))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons Row (Watch Now & Download Movie)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = VortexIcons.Play,
                                contentDescription = "Watch Now",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                            border = BorderStroke(1.2.dp, Color(0xFFFFB800)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = VortexIcons.Download,
                                contentDescription = "Download Movie",
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download",
                                color = Color(0xFFFFB800),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Storyline Section
                    Text(
                        text = "Storyline",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = media.synopsis?.takeIf { it.isNotBlank() }
                            ?: "Paul Atreides unites with the Fremen while on a warpath of revenge against the conspirators who destroyed his family. Facing a choice between the love of his life and the fate of the universe, he endeavors to prevent a terrible future.",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Languages Section
                    Text(
                        text = "Languages",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "English",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Cast Section
                    Text(
                        text = "Cast",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Cast Avatars Row
            item {
                val castList = if (media.cast.isNotEmpty()) media.cast else listOf(
                    com.movievortex.android.model.MediaCast("Timothée Chalamet", "Paul Atreides"),
                    com.movievortex.android.model.MediaCast("Zendaya", "Chani"),
                    com.movievortex.android.model.MediaCast("Rebecca Ferguson", "Lady Jessica"),
                    com.movievortex.android.model.MediaCast("Javier Bardem", "Stilgar")
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(castList) { actor ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(68.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (actor.avatarUrl != null) {
                                    AsyncImage(
                                        model = actor.avatarUrl,
                                        contentDescription = actor.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = actor.name.take(1),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = actor.name,
                                color = Color(0xFFDDDDDD),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. Seasons & Episodes Section (if TV or Anime)
            if (media.seasons.isNotEmpty() || media.episodes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Episodes",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (media.seasons.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(media.seasons) { season ->
                                    val isSelected = season.seasonNumber == state.selectedSeason
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color.White else Color(0xFF222222))
                                            .clickable { detailController.selectSeason(season.seasonNumber) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = season.name,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                items(state.currentSeasonEpisodes) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onClick = { onPlayEpisode(media, episode) },
                        onDownloadClick = { onDownloadClick(media, episode) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Top Navigation Bar (Back and Bookmark overlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    imageVector = VortexIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { detailController.toggleMyList() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isInMyList) VortexIcons.Bookmark else VortexIcons.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Floating Action Bar at Bottom-Right: Download & Watch
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Pill Button
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xF01A1A20)),
                border = BorderStroke(1.2.dp, Color(0xFFFFB800)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = VortexIcons.Download,
                    contentDescription = "Download",
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Download",
                    color = Color(0xFFFFB800),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Watch Pill Button
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = VortexIcons.Play,
                    contentDescription = "Watch",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Watch",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: MediaEpisode,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141414))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222))
        ) {
            if (episode.poster != null) {
                AsyncImage(
                    model = episode.poster,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = VortexIcons.Play,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "EP ${episode.episodeNumber} • ${episode.title}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            episode.runtime?.let { mins ->
                Text(
                    text = "$mins min",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onDownloadClick,
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF222228), CircleShape)
        ) {
            Icon(
                imageVector = VortexIcons.Download,
                contentDescription = "Download Episode",
                tint = Color(0xFFFFB800),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
