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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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
        viewModelScope.launch(Dispatchers.IO) {
            val downloadItems = mutableListOf<DownloadUiItem>()

            // 1. Scan offline directory for completed files
            try {
                val dir = File("/storage/emulated/0/Download/MovieVortex")
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { file ->
                        file.isFile && (file.extension.equals("mp4", true) ||
                                file.extension.equals("mkv", true) ||
                                file.extension.equals("webm", true) ||
                                file.extension.equals("avi", true))
                    }?.forEach { file ->
                        val displayTitle = file.nameWithoutExtension.replace("_", " ")
                        downloadItems.add(
                            DownloadUiItem(
                                id = file.absolutePath.hashCode().toString(),
                                title = displayTitle,
                                poster = null,
                                quality = "1080p",
                                fileSizeBytes = file.length(),
                                downloadedBytes = file.length(),
                                progress = 1.0f,
                                state = MediaDownloadState.Completed,
                                localFilePath = file.absolutePath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadController", "Error scanning local downloads", e)
            }

            // 2. Active downloads from CloudStream queue
            try {
                val queue = DownloadQueueManager.queue.value
                queue.forEach { queueWrapper ->
                    val item = queueWrapper.downloadItem
                    val resumeEp = queueWrapper.resumePackage?.item?.ep
                    val id = queueWrapper.id
                    val title = item?.resultName ?: resumeEp?.mainName ?: resumeEp?.name ?: "Media Download"
                    val poster = item?.resultPoster ?: resumeEp?.poster
                    val cleanName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")

                    if (downloadItems.none { it.title == title }) {
                        downloadItems.add(
                            DownloadUiItem(
                                id = id.toString(),
                                title = title,
                                poster = poster,
                                quality = "1080p",
                                fileSizeBytes = 0L,
                                downloadedBytes = 0L,
                                progress = 0.35f,
                                state = MediaDownloadState.Downloading,
                                localFilePath = "/storage/emulated/0/Download/MovieVortex/$cleanName.mp4"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadController", "Error loading queue", e)
            }

            _uiState.value = DownloadsUiState(
                items = downloadItems,
                activeDownloadsCount = downloadItems.count { it.state == MediaDownloadState.Downloading }
            )
        }
    }

    fun pauseDownload(id: String) {
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
        val itemToDelete = _uiState.value.items.find { it.id == id }
        if (itemToDelete?.localFilePath != null) {
            try {
                val f = File(itemToDelete.localFilePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
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
        val localPath = "/storage/emulated/0/Download/MovieVortex/$cleanName.mp4"

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
            localFilePath = localPath
        )

        _uiState.value = _uiState.value.copy(
            items = listOf(newItem) + _uiState.value.items.filter { it.title != title },
            activeDownloadsCount = _uiState.value.activeDownloadsCount + 1
        )

        Toast.makeText(context, "Starting download: $title", Toast.LENGTH_SHORT).show()

        // 1. Queue into CloudStream's native DownloadQueueManager
        try {
            val resultEp = com.lagradost.cloudstream3.ui.result.ResultEpisode(
                name = episode?.title ?: media.title,
                season = episode?.seasonNumber,
                episode = episode?.episodeNumber,
                data = episode?.dataUrl ?: media.rawUrl,
                posterUrl = episode?.poster ?: media.poster,
                id = (episode?.dataUrl ?: media.rawUrl).hashCode()
            )
            val queueItem = com.lagradost.cloudstream3.utils.downloader.DownloadObjects.DownloadQueueItem(
                episode = resultEp,
                isMovie = media.type == com.movievortex.android.model.MediaType.Movie,
                name = media.title,
                type = if (media.type == com.movievortex.android.model.MediaType.Movie)
                    com.lagradost.cloudstream3.TvType.Movie else com.lagradost.cloudstream3.TvType.TvSeries,
                posterUrl = media.poster,
                apiName = media.providerApiName,
                id = (media.rawUrl + (episode?.dataUrl ?: "")).hashCode(),
                url = media.rawUrl
            )
            DownloadQueueManager.addToQueue(queueItem.toWrapper())
        } catch (e: Exception) {
            android.util.Log.e("DownloadController", "Error adding to CloudStream queue", e)
        }

        // 2. Background resolution and direct download manager
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var directVideoUrl: String? = streamUrl
                if (directVideoUrl == null) {
                    val api = com.lagradost.cloudstream3.APIHolder.getApiFromNameNull(media.providerApiName)
                        ?: com.lagradost.cloudstream3.APIHolder.apis.firstOrNull { media.rawUrl.startsWith(it.mainUrl) }
                        ?: com.lagradost.cloudstream3.APIHolder.apis.firstOrNull()

                    if (api != null) {
                        val targetData = episode?.dataUrl ?: media.rawUrl
                        com.lagradost.cloudstream3.ui.APIRepository(api).loadLinks(
                            data = targetData,
                            isCasting = false,
                            subtitleCallback = {},
                            callback = { link ->
                                if (directVideoUrl == null && link.url.isNotBlank()) {
                                    directVideoUrl = link.url
                                }
                            }
                        )
                    }
                }

                if (directVideoUrl != null && (directVideoUrl!!.startsWith("http://") || directVideoUrl!!.startsWith("https://"))) {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
                    if (dm != null) {
                        val request = android.app.DownloadManager.Request(Uri.parse(directVideoUrl))
                            .setTitle(title)
                            .setDescription("MovieVortex Offline Download")
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MovieVortex/$cleanName.mp4")
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                        dm.enqueue(request)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadController", "Background link resolve/download failed", e)
            }
        }
    }
}
