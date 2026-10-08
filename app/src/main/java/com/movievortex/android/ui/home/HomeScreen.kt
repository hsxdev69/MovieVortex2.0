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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
            // Type & Quality tags
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
                        text = "FEATURED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                activeItem.rating?.let { rating ->
                    Text(
                        text = "★ %.1f".format(rating),
                        color = VortexColors.WarmAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                activeItem.year?.let { year ->
                    Text(
                        text = year.toString(),
                        color = VortexColors.SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = activeItem.title,
                color = VortexColors.PrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            activeItem.synopsis?.let { synopsis ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = synopsis,
                    color = VortexColors.SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPlayClick(activeItem) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VortexColors.CinematicRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = { onMediaClick(activeItem) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x33000000)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "My List",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "My List",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
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
            color = VortexColors.PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { media ->
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                            .size(28.dp)
                            .padding(4.dp)
                            .background(Color(0x99000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
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
    Column {
        Text(
            text = "Popular Streaming Networks",
            color = VortexColors.PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(platforms) { platform ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPlatformClick(platform) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(VortexColors.CardSurface)
                            .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = platform.name.take(3).uppercase(),
                            color = Color(platform.primaryColor),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = platform.name,
                        color = VortexColors.SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
                color = VortexColors.PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "More →",
                color = VortexColors.CinematicRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
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
