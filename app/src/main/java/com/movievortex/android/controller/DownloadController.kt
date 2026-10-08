package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.utils.downloader.DownloadManager
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import com.movievortex.android.model.MediaDownloadState
import com.movievortex.android.model.MediaType
import com.movievortex.android.model.MovieVortexMedia
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
            // Load current downloads from DownloadManager
            val queue = DownloadQueueManager.currentQueue
            val downloadItems = mutableListOf<DownloadUiItem>()

            // Map queue to DownloadUiItems
            queue.forEach { queueItem ->
                val progress = if (queueItem.totalBytes > 0) {
                    (queueItem.downloadedBytes.toFloat() / queueItem.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f

                downloadItems.add(
                    DownloadUiItem(
                        id = queueItem.key.toString(),
                        title = queueItem.item.name,
                        poster = queueItem.item.poster,
                        quality = "1080p",
                        fileSizeBytes = queueItem.totalBytes,
                        downloadedBytes = queueItem.downloadedBytes,
                        progress = progress,
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
}
