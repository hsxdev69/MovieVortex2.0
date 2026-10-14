package com.movievortex.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movievortex.android.controller.SearchController
import com.movievortex.android.controller.SearchStatus
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.ui.components.PosterCard
import com.movievortex.android.ui.components.PosterSkeleton
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun SearchScreen(
    searchController: SearchController,
    onMediaClick: (MovieVortexMedia) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by searchController.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .padding(top = 12.dp)
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = VortexIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = VortexColors.PrimaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dark Search Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VortexColors.CardSurface)
                    .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = VortexIcons.Search,
                        contentDescription = "Search",
                        tint = VortexColors.MutedText,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (state.query.isEmpty()) {
                            Text(
                                text = "Search movies, series, anime...",
                                color = VortexColors.MutedText,
                                fontSize = 14.sp
                            )
                        }

                        BasicTextField(
                            value = state.query,
                            onValueChange = { searchController.onQueryChange(it) },
                            singleLine = true,
                            cursorBrush = SolidColor(VortexColors.CinematicRed),
                            textStyle = TextStyle(
                                color = VortexColors.PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    searchController.performSearch(state.query)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { searchController.clearQuery() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = VortexIcons.Close,
                                contentDescription = "Clear",
                                tint = VortexColors.SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Content States
        when (state.status) {
            SearchStatus.Idle -> {
                // Suggestions & Recent Searches
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Trending Searches",
                        color = VortexColors.PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    state.recentQueries.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    searchController.performSearch(suggestion)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = VortexIcons.Search,
                                contentDescription = null,
                                tint = VortexColors.MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                color = VortexColors.SecondaryText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            SearchStatus.Searching -> {
                // Branded Skeletons Grid during search
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(listOf(1, 2, 3, 4)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(3) {
                                PosterSkeleton(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 12.dp
                                )
                            }
                        }
                    }
                }
            }

            SearchStatus.Results -> {
                // Responsive Poster Grid (3 columns chunked)
                val chunked = state.results.chunked(3)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { media ->
                                PosterCard(
                                    media = media,
                                    onClick = { onMediaClick(media) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            SearchStatus.NoResults -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = VortexIcons.SearchOff,
                            contentDescription = "No Results",
                            tint = VortexColors.MutedText,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results found for \"${state.query}\"",
                            color = VortexColors.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching for a different title or keyword",
                            color = VortexColors.MutedText,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            SearchStatus.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = state.errorMessage ?: "This content is currently unavailable.",
                            color = VortexColors.PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please check your network and try again.",
                            color = VortexColors.MutedText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { searchController.performSearch(state.query) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VortexColors.CinematicRed
                            )
                        ) {
                            Text(text = "Retry", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
