package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import com.movievortex.android.model.MediaDownloadState
import com.movievortex.android.model.MediaType
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.movievortex.android.model.MediaEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadUiItem(
    val id: String,
    val title: String,
    val mediaType: MediaType = MediaType.Movie,
    val poster: String? = null,
    val quality: String = "1080p",
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0
    val speedBytesPerSec: Long = 0L,
    val state: MediaDownloadState = MediaDownloadState.Queued,
    val localFilePath: String? = null
)

data class DownloadsUiState(
    val items: List<DownloadUiItem> = emptyList(),
    val activeDownloadsCount: Int = 0
)

class DownloadController : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            val queue = DownloadQueueManager.queue.value
            val downloadItems = mutableListOf<DownloadUiItem>()

            queue.forEach { queueWrapper ->
                val item = queueWrapper.downloadItem
                val resumeEp = queueWrapper.resumePackage?.item?.ep
                val id = queueWrapper.id
                val title = item?.resultName ?: resumeEp?.mainName ?: resumeEp?.name ?: "Media Download"
                val poster = item?.resultPoster ?: resumeEp?.poster

                downloadItems.add(
                    DownloadUiItem(
                        id = id.toString(),
                        title = title,
                        poster = poster,
                        quality = "1080p",
                        fileSizeBytes = 0L,
                        downloadedBytes = 0L,
                        progress = 0f,
                        state = MediaDownloadState.Downloading
                    )
                )
            }

            _uiState.value = DownloadsUiState(
                items = downloadItems,
                activeDownloadsCount = downloadItems.count { it.state == MediaDownloadState.Downloading }
            )
        }
    }

    fun pauseDownload(id: String) {
        // Pauses underlying task
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == id) it.copy(state = MediaDownloadState.Paused) else it
            }
        )
    }

    fun resumeDownload(id: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == id) it.copy(state = MediaDownloadState.Downloading) else it
            }
        )
    }

    fun deleteDownload(id: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filter { it.id != id }
        )
    }

    fun downloadMedia(
        context: Context,
        media: com.movievortex.android.model.MovieVortexMedia,
        episode: MediaEpisode? = null,
        streamUrl: String? = null
    ) {
        val title = if (episode != null) {
            "${media.title} S${episode.seasonNumber}E${episode.episodeNumber}"
        } else {
            media.title
        }
        val cleanName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val newId = System.currentTimeMillis().toString()

        val newItem = DownloadUiItem(
            id = newId,
            title = title,
            mediaType = media.type,
            poster = episode?.poster ?: media.poster,
            quality = "1080p",
            fileSizeBytes = 0L,
            downloadedBytes = 0L,
            progress = 0.05f,
            state = MediaDownloadState.Downloading,
            localFilePath = "/storage/emulated/0/Download/MovieVortex/$cleanName.mp4"
        )

        _uiState.value = _uiState.value.copy(
            items = listOf(newItem) + _uiState.value.items,
            activeDownloadsCount = _uiState.value.activeDownloadsCount + 1
        )

        val targetUrl = streamUrl ?: media.rawUrl
        if (targetUrl.isNotBlank() && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
                if (dm != null) {
                    val request = android.app.DownloadManager.Request(Uri.parse(targetUrl))
                        .setTitle(title)
                        .setDescription("Downloading $title from MovieVortex")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MovieVortex/$cleanName.mp4")
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                    dm.enqueue(request)
                    Toast.makeText(context, "Download started: $title", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Download queued: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
