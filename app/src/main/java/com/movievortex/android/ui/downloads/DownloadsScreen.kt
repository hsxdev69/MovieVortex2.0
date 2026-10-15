package com.movievortex.android.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movievortex.android.ui.components.VortexIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.movievortex.android.controller.DownloadController
import com.movievortex.android.controller.DownloadUiItem
import com.movievortex.android.model.MediaDownloadState
import com.movievortex.android.ui.theme.VortexColors

@Composable
fun DownloadsScreen(
    downloadController: DownloadController,
    onBrowseClick: () -> Unit,
    onPlayOffline: (DownloadUiItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by downloadController.uiState.collectAsState()

    LaunchedEffect(Unit) {
        downloadController.loadDownloads()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VortexColors.PureBlack)
            .padding(top = 16.dp)
    ) {
        // Heading
        Text(
            text = "Downloads",
            color = VortexColors.PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.items.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = VortexIcons.DownloadOff,
                        contentDescription = null,
                        tint = VortexColors.MutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        color = VortexColors.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Download movies and episodes to watch offline anywhere",
                        color = VortexColors.MutedText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBrowseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = VortexColors.CinematicRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Browse Content", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Downloads Vertical List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { downloadController.pauseDownload(item.id) },
                        onResume = { downloadController.resumeDownload(item.id) },
                        onDelete = { downloadController.deleteDownload(item.id) },
                        onPlay = { onPlayOffline(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadUiItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VortexColors.CardSurface)
            .border(1.dp, VortexColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(68.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(VortexColors.DarkGraphite)
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info & Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = VortexColors.PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // State & Quality
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.state == MediaDownloadState.Completed) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x3310B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = VortexColors.EmeraldGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${item.quality} • ${(item.progress * 100).toInt()}%",
                        color = VortexColors.SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar if in progress
            if (item.state != MediaDownloadState.Completed) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = VortexColors.CinematicRed,
                    trackColor = VortexColors.SkeletonDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Button
        when (item.state) {
            MediaDownloadState.Completed -> {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = VortexIcons.Play,
                        contentDescription = "Play offline",
                        tint = VortexColors.EmeraldGreen
                    )
                }
            }
            MediaDownloadState.Downloading -> {
                IconButton(onClick = onPause) {
                    Icon(
                        imageVector = VortexIcons.Pause,
                        contentDescription = "Pause",
                        tint = VortexColors.SecondaryText
                    )
                }
            }
            MediaDownloadState.Paused -> {
                IconButton(onClick = onResume) {
                    Icon(
                        imageVector = VortexIcons.Play,
                        contentDescription = "Resume",
                        tint = VortexColors.PrimaryText
                    )
                }
            }
            else -> {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = VortexIcons.Delete,
                        contentDescription = "Delete",
                        tint = VortexColors.MutedText
                    )
                }
            }
        }
    }
}
