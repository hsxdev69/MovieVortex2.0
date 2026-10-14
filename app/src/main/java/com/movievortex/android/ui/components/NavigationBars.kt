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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Stylized AV / MV Monogram Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AV",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-1.5).sp
            )
        }

        // Center: "Watch Offline Anywhere" text (Screenshot_20261014_180728.jpg)
        Text(
            text = "Watch Offline Anywhere",
            color = Color(0xFFDDDDDD),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Right: Search Icon
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = VortexIcons.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
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
            .background(Color(0xFF000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                title = "Movie/Tv",
                icon = VortexIcons.Movie,
                isSelected = currentDestination == MainDestination.MoviesTv,
                onClick = { onDestinationSelected(MainDestination.MoviesTv) }
            )

            BottomNavItemWithCharacter(
                title = "Anime",
                character = "ह",
                isSelected = currentDestination == MainDestination.Anime,
                onClick = { onDestinationSelected(MainDestination.Anime) }
            )

            BottomNavItem(
                title = "Downloads",
                icon = VortexIcons.Download,
                isSelected = currentDestination == MainDestination.Downloads,
                badgeCount = activeDownloadCount,
                onClick = { onDestinationSelected(MainDestination.Downloads) }
            )

            BottomNavItem(
                title = "Settings",
                icon = VortexIcons.Settings,
                isSelected = currentDestination == MainDestination.Settings,
                onClick = { onDestinationSelected(MainDestination.Settings) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) Color.White else Color(0xFF888888)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
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
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Selected active white underline bar (Screenshot_20261014_180728.jpg)
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) Color.White else Color.Transparent)
        )
    }
}

@Composable
private fun BottomNavItemWithCharacter(
    title: String,
    character: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) Color.White else Color(0xFF888888)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = character,
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Selected active white underline bar
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) Color.White else Color.Transparent)
        )
    }
}
