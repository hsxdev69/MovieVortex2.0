package com.movievortex.android.ui.home

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.movievortex.android.controller.ContentRow
import com.movievortex.android.controller.HomeController
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.model.StreamingPlatform
import com.movievortex.android.ui.components.PosterCard
import com.movievortex.android.ui.components.PosterSkeleton
import com.movievortex.android.ui.theme.VortexColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    homeController: HomeController,
    onMediaClick: (MovieVortexMedia) -> Unit,
    onPlayClick: (MovieVortexMedia) -> Unit,
    onPlatformClick: (StreamingPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by homeController.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VortexColors.PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = VortexColors.CinematicRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading MovieVortex...",
                    color = VortexColors.MutedText,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Featured Cinematic Hero
        if (state.heroItems.isNotEmpty()) {
            item {
                FeaturedHero(
                    items = state.heroItems,
                    onMediaClick = onMediaClick,
                    onPlayClick = onPlayClick
                )
            }
        }

        // 2. Continue Watching (Home only)
        if (state.continueWatching.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                ContinueWatchingSection(
                    items = state.continueWatching,
                    onMediaClick = onMediaClick,
                    onRemove = { homeController.removeContinueWatching(it) }
                )
            }
        }

        // 3. Streaming Platform Shortcuts (Home only)
        if (state.platforms.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                PlatformShortcutsSection(
                    platforms = state.platforms,
                    onPlatformClick = onPlatformClick
                )
            }
        }

        // 4. Curated Content Rows
        items(state.rows) { row ->
            Spacer(modifier = Modifier.height(24.dp))
            ContentRowSection(
                row = row,
                onMediaClick = onMediaClick
            )
        }

        // 5. Explore More Grid Header
        if (state.exploreGrid.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Explore More",
                        color = VortexColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Infinite catalog curated from global streaming networks",
                        color = VortexColors.MutedText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Explore Grid Items (3 columns chunked)
            val chunked = state.exploreGrid.chunked(3)
            items(chunked) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { media ->
                        PosterCard(
                            media = media,
                            onClick = { onMediaClick(media) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining empty columns
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Auto-load trigger near bottom
            item {
                LaunchedEffect(Unit) {
                    homeController.loadMoreExplore()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isExploreLoading) {
                        CircularProgressIndicator(
                            color = VortexColors.CinematicRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedHero(
    items: List<MovieVortexMedia>,
    onMediaClick: (MovieVortexMedia) -> Unit,
    onPlayClick: (MovieVortexMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-rotation every 7 seconds
    LaunchedEffect(items) {
        while (true) {
            delay(7000)
            if (items.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }

    val activeItem = items.getOrNull(currentIndex) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .clickable { onMediaClick(activeItem) }
    ) {
        Crossfade(targetState = activeItem, label = "hero_crossfade") { item ->
            AsyncImage(
                model = item.backdrop ?: item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient Masking for absolute readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VortexColors.HeroGradient)
        )

            // Hero Details Content at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // Faint MovieVortex brand text
            Text(
                text = "MovieVortex",
                color = Color(0x66FFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Type & Multi tags (Screenshot_20261014_180728.jpg)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MOVIE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MULTI",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = activeItem.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Year and Rating: 2026 • ★ 6.3
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${activeItem.year ?: 2026}",
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "  •  ",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
                Text(
                    text = "★ %.1f".format(activeItem.rating ?: 6.3f),
                    color = Color(0xFFFFC107),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Actions (White Play pill button, My List, Page indicator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White pill Play button (Screenshot_20261014_180728.jpg)
                    Button(
                        onClick = { onPlayClick(activeItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Play,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // My List Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMediaClick(activeItem) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = VortexIcons.BookmarkBorder,
                            contentDescription = "My List",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "My List",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Page indicator (01 — 19)
                val displayCurrent = (currentIndex + 1).coerceAtLeast(1)
                val displayTotal = items.size.coerceAtLeast(1)
                Text(
                    text = "%02d ── %02d".format(displayCurrent, displayTotal),
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingSection(
    items: List<MovieVortexMedia>,
    onMediaClick: (MovieVortexMedia) -> Unit,
    onRemove: (MovieVortexMedia) -> Unit
) {
    Column {
        Text(
            text = "Continue Watching",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items) { media ->
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMediaClick(media) }
                ) {
                    PosterCard(
                        media = media,
                        onClick = { onMediaClick(media) },
                        showDetails = false
                    )

                    // Remove button
                    IconButton(
                        onClick = { onRemove(media) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(26.dp)
                            .padding(4.dp)
                            .background(Color(0x99000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = VortexIcons.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformShortcutsSection(
    platforms: List<StreamingPlatform>,
    onPlatformClick: (StreamingPlatform) -> Unit
) {
    val ottPlatforms = listOf(
        Pair("Prime Video", Color(0xFF00A8E1)),
        Pair("Netflix", Color(0xFFE50914)),
        Pair("Sony LIV", Color(0xFF6B21A8)),
        Pair("Crunchyroll", Color(0xFFFF6400)),
        Pair("JioHotstar", Color(0xFF1B4DFF)),
        Pair("Zee5", Color(0xFF8250DF))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(ottPlatforms) { (name, brandColor) ->
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(brandColor)
                    .clickable {
                        val matched = platforms.firstOrNull { it.name.contains(name, ignoreCase = true) }
                            ?: StreamingPlatform(name.lowercase(), name, "", 0xFF000000L, "")
                        onPlatformClick(matched)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (name) {
                        "Prime Video" -> "prime\nvideo"
                        "Netflix" -> "N"
                        "Sony LIV" -> "SONY\nliv"
                        "Crunchyroll" -> "CR"
                        "JioHotstar" -> "Jio\nHotstar"
                        else -> "Z5"
                    },
                    color = Color.White,
                    fontSize = if (name == "Netflix") 24.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ContentRowSection(
    row: ContentRow,
    onMediaClick: (MovieVortexMedia) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { /* More */ }
            ) {
                Text(
                    text = "More",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = VortexIcons.ChevronRight,
                    contentDescription = "More",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(row.items) { media ->
                PosterCard(
                    media = media,
                    onClick = { onMediaClick(media) }
                )
            }
        }
    }
}
