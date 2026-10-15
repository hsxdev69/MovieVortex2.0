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

        Toast.makeText(context, "Resolving video links for: $title...", Toast.LENGTH_SHORT).show()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = com.lagradost.cloudstream3.APIHolder.getApiFromNameNull(media.providerApiName)
                    ?: com.lagradost.cloudstream3.APIHolder.apis.firstOrNull { media.rawUrl.startsWith(it.mainUrl) }
                    ?: com.lagradost.cloudstream3.APIHolder.apis.firstOrNull()

                val repo = if (api != null) com.lagradost.cloudstream3.ui.APIRepository(api) else null

                // Resolve real episode dataUrl
                var targetDataUrl = episode?.dataUrl
                if (targetDataUrl.isNullOrBlank() && repo != null && media.rawUrl.isNotBlank()) {
                    val loadRes = repo.load(media.rawUrl)
                    if (loadRes is com.lagradost.cloudstream3.mvvm.Resource.Success) {
                        targetDataUrl = when (val resVal = loadRes.value) {
                            is com.lagradost.cloudstream3.MovieLoadResponse -> resVal.dataUrl.ifBlank { media.rawUrl }
                            is com.lagradost.cloudstream3.TvSeriesLoadResponse -> resVal.episodes.firstOrNull()?.data ?: media.rawUrl
                            is com.lagradost.cloudstream3.AnimeLoadResponse -> resVal.episodes.values.flatten().firstOrNull()?.data ?: media.rawUrl
                            else -> media.rawUrl
                        }
                    }
                }
                if (targetDataUrl.isNullOrBlank()) {
                    targetDataUrl = media.rawUrl
                }

                val extractedLinks = mutableListOf<com.lagradost.cloudstream3.utils.ExtractorLink>()
                val extractedSubs = mutableListOf<com.lagradost.cloudstream3.ui.player.SubtitleData>()

                if (repo != null && targetDataUrl.isNotBlank()) {
                    repo.loadLinks(
                        data = targetDataUrl,
                        isCasting = false,
                        subtitleCallback = { sub ->
                            extractedSubs.add(com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.getSubtitleData(sub))
                        },
                        callback = { link ->
                            if (link.url.isNotBlank()) {
                                extractedLinks.add(link)
                            }
                        }
                    )
                }

                val epNumber = episode?.episodeNumber ?: 1
                val seasonNumber = episode?.seasonNumber ?: 1
                val tvType = if (media.type == com.movievortex.android.model.MediaType.Movie)
                    com.lagradost.cloudstream3.TvType.Movie else com.lagradost.cloudstream3.TvType.TvSeries

                val resultEp = com.lagradost.cloudstream3.ui.result.ResultEpisode(
                    headerName = media.title,
                    name = episode?.title ?: media.title,
                    poster = episode?.poster ?: media.poster,
                    episode = epNumber,
                    seasonIndex = seasonNumber,
                    season = seasonNumber,
                    data = targetDataUrl,
                    apiName = media.providerApiName,
                    id = (media.rawUrl + targetDataUrl).hashCode(),
                    index = epNumber - 1,
                    position = 0L,
                    duration = 0L,
                    score = null,
                    description = media.synopsis,
                    isFiller = false,
                    tvType = tvType,
                    parentId = media.rawUrl.hashCode(),
                    videoWatchState = com.lagradost.cloudstream3.ui.result.VideoWatchState.None
                )

                // 1. Queue into CloudStream's native DownloadQueueManager with real links
                val queueItem = com.lagradost.cloudstream3.utils.downloader.DownloadObjects.DownloadQueueItem(
                    episode = resultEp,
                    isMovie = media.type == com.movievortex.android.model.MediaType.Movie,
                    resultName = media.title,
                    resultType = tvType,
                    resultPoster = media.poster,
                    apiName = media.providerApiName,
                    resultId = media.rawUrl.hashCode(),
                    resultUrl = media.rawUrl,
                    links = if (extractedLinks.isNotEmpty()) extractedLinks else null,
                    subs = if (extractedSubs.isNotEmpty()) extractedSubs else null
                )
                DownloadQueueManager.addToQueue(queueItem.toWrapper())

                // 2. Select best direct download link (e.g. mp4/mkv)
                val bestDirectLink = extractedLinks.firstOrNull { !it.isM3u8 && (it.url.contains(".mp4") || it.url.contains(".mkv")) }
                    ?: extractedLinks.firstOrNull { !it.isM3u8 }
                    ?: extractedLinks.firstOrNull()

                val directVideoUrl = streamUrl ?: bestDirectLink?.url
                if (directVideoUrl != null && (directVideoUrl.startsWith("http://") || directVideoUrl.startsWith("https://"))) {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
                    if (dm != null) {
                        val request = android.app.DownloadManager.Request(Uri.parse(directVideoUrl))
                            .setTitle(title)
                            .setDescription("MovieVortex Offline Download")
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MovieVortex/$cleanName.mp4")
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        if (bestDirectLink != null) {
                            if (bestDirectLink.referer.isNotBlank()) {
                                request.addRequestHeader("Referer", bestDirectLink.referer)
                            }
                            bestDirectLink.headers.forEach { (k, v) ->
                                request.addRequestHeader(k, v)
                            }
                        }
                        dm.enqueue(request)
                    }

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Real download started: $title", Toast.LENGTH_SHORT).show()
                    }
                } else if (extractedLinks.isNotEmpty()) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "MovieVortex Download Queue active for: $title", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Download queued: Waiting for stream links", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadController", "Background link resolve/download failed", e)
            }
        }
    }
}
