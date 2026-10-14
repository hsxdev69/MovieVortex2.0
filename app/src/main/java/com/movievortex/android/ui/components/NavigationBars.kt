package com.movievortex.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movievortex.android.ui.theme.VortexColors

enum class MainDestination {
    MoviesTv,
    Anime,
    Downloads,
    Settings
}

@Composable
fun MovieVortexTopBar(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    streamingQualityText: String = "4K ULTRA HD",
    isTransparent: Boolean = true
) {
    val bgModifier = if (isTransparent) {
        Modifier.background(VortexColors.TopBarGradient)
    } else {
        Modifier.background(VortexColors.PureBlack)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(bgModifier)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo
        MovieVortexHeaderLogo()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Streaming Quality Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FFFFFF))
                    .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = streamingQualityText,
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Quick Search Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = VortexIcons.Search,
                    contentDescription = "Search",
                    tint = VortexColors.PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MovieVortexBottomBar(
    currentDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeDownloadCount: Int = 0
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFA0B0B0E))
    ) {
        // Thin top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(VortexColors.Divider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                title = "Movies & TV",
                iconSelected = VortexIcons.Movie,
                iconUnselected = VortexIcons.Movie,
                isSelected = currentDestination == MainDestination.MoviesTv,
                onClick = { onDestinationSelected(MainDestination.MoviesTv) }
            )

            BottomNavItem(
                title = "Anime",
                iconSelected = VortexIcons.Tv,
                iconUnselected = VortexIcons.Tv,
                isSelected = currentDestination == MainDestination.Anime,
                onClick = { onDestinationSelected(MainDestination.Anime) }
            )

            BottomNavItem(
                title = "Downloads",
                iconSelected = VortexIcons.Download,
                iconUnselected = VortexIcons.Download,
                isSelected = currentDestination == MainDestination.Downloads,
                badgeCount = activeDownloadCount,
                onClick = { onDestinationSelected(MainDestination.Downloads) }
            )

            BottomNavItem(
                title = "Settings",
                iconSelected = VortexIcons.Settings,
                iconUnselected = VortexIcons.Settings,
                isSelected = currentDestination == MainDestination.Settings,
                onClick = { onDestinationSelected(MainDestination.Settings) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = tween(200),
        label = "iconScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) VortexColors.PrimaryText else VortexColors.MutedText,
        animationSpec = tween(200),
        label = "contentColor"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with optional badge
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(VortexColors.CinematicRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Selected active glowing dot indicator
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    if (isSelected) VortexColors.CinematicRed else Color.Transparent
                )
        )
    }
}
