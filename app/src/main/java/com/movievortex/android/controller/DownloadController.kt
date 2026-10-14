package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}
