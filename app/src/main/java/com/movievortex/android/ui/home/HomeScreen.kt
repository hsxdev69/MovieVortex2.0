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
import com.movievortex.android.ui.components.OttLogoBadge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
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
    var selectedCategory by remember { mutableStateOf("Movies") }

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
        // 0. Category Pills Row (Figma Screenshot_20261015_194137.jpg)
        item {
            CategoryPillsRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

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
fun CategoryPillsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Movies", "Sports", "TV Shows", "News", "Anime")
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val bgColor = if (isSelected) Color(0xFFFFB800) else Color(0x33252528)
            val textColor = if (isSelected) Color.Black else Color(0xFFE2E2E6)
            val borderModifier = if (isSelected) {
                Modifier
            } else {
                Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(20.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(borderModifier)
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCategorySelected(category) }
                    )
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
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
            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .height(430.dp)
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0xAA000000),
                            Color(0xF00A0A0C)
                        )
                    )
                )
        )

        // Hero Details Content at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Category Tag / Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFB800))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "VORTEX ORIGINAL",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "4K ULTRA HD",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = activeItem.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Year and Rating: 2026 • ★ 8.2 • Trending
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
                    text = "★ %.1f".format(activeItem.rating ?: 8.2f),
                    color = Color(0xFFFFB800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  •  Trending",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Actions (Golden Amber Watch Now button + My List + Indicator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Golden Amber Watch Now button (Figma Screenshot_20261015_194137.jpg)
                    Button(
                        onClick = { onPlayClick(activeItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
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
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // My List Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x33252528))
                            .border(1.dp, Color(0x30FFFFFF), CircleShape)
                            .clickable { onMediaClick(activeItem) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VortexIcons.BookmarkBorder,
                            contentDescription = "My List",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.take(5).forEachIndexed { index, _ ->
                        val isIndicatorActive = index == (currentIndex % 5)
                        Box(
                            modifier = Modifier
                                .width(if (isIndicatorActive) 16.dp else 6.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isIndicatorActive) Color(0xFFFFB800) else Color(0x66FFFFFF))
                        )
                    }
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
        "Prime Video",
        "Netflix",
        "Sony LIV",
        "Crunchyroll",
        "JioHotstar",
        "Zee5"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(ottPlatforms) { name ->
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .clickable {
                        val matched = platforms.firstOrNull { it.name.contains(name, ignoreCase = true) }
                            ?: StreamingPlatform(name.lowercase(), name, "", 0xFF000000L, "")
                        onPlatformClick(matched)
                    }
            ) {
                OttLogoBadge(name = name)
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
                    text = "See All",
                    color = Color(0xFFFFB800),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = VortexIcons.ChevronRight,
                    contentDescription = "See All",
                    tint = Color(0xFFFFB800),
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
