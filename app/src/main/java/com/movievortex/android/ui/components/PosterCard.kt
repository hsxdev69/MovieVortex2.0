package com.movievortex.android.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.movievortex.android.model.MediaType
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun PosterCard(
    media: MovieVortexMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 135.dp,
    showDetails: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(width)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(VortexColors.SkeletonDark)
        ) {
            SubcomposeAsyncImage(
                model = media.poster ?: media.backdrop,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    PosterSkeleton(cornerRadius = 12.dp)
                },
                error = {
                    PosterSkeleton(cornerRadius = 12.dp)
                }
            )

            // Bottom gradient overlay on poster for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x20000000),
                                Color(0xCC000000)
                            ),
                            startY = 180f
                        )
                    )
            )

            // Top Badges Row (Type badge & Rating)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Type Tag
                val typeLabel = when (media.type) {
                    MediaType.Movie -> "MOVIE"
                    MediaType.TvSeries -> "SERIES"
                    MediaType.Anime -> "ANIME"
                    else -> "HD"
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = Color(0xFFDDDDDD),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Rating Pill (if available)
                media.rating?.let { rating ->
                    if (rating > 0f) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xE616161C),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ %.1f".format(rating),
                                color = VortexColors.WarmAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Progress bar (if watching)
            if (media.watchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x66FFFFFF))
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(media.watchProgress.coerceIn(0.05f, 1f))
                            .height(3.dp)
                            .background(VortexColors.CinematicRed)
                    )
                }
            }
        }

        if (showDetails) {
            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = media.title,
                color = VortexColors.PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle: Year & Genres
            val subText = listOfNotNull(
                media.year?.toString(),
                media.genres.firstOrNull()
            ).joinToString(" • ")

            if (subText.isNotBlank()) {
                Text(
                    text = subText,
                    color = VortexColors.MutedText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
