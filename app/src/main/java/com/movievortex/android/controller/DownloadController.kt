package com.movievortex.android.controller

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import com.movievortex.android.model.MediaDownloadState
import com.movievortex.android.model.MediaEpisode
import com.movievortex.android.model.MediaType
import com.movievortex.android.model.MovieVortexMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DownloadUiItem(
    val id: String,
    val title: String,
    val mediaType: MediaType = MediaType.Movie,
    val poster: String? = null,
    val quality: String = "1080p",
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0 strictly calculated from actual transferred bytes
    val speedBytesPerSec: Long = 0L,
    val state: MediaDownloadState = MediaDownloadState.Queued,
    val localFilePath: String? = null,
    val errorMessage: String? = null,
    val originalMedia: MovieVortexMedia? = null,
    val originalEpisode: MediaEpisode? = null
)

data class DownloadsUiState(
    val items: List<DownloadUiItem> = emptyList(),
    val activeDownloadsCount: Int = 0
)

class DownloadController : ViewModel() {

    companion object {
        private const val TAG = "DownloadController"
        const val DOWNLOAD_DIR_NAME = "MovieVortex"
    }

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val downloadProgressListener: (Triple<Int, Long, Long>) -> Unit = { (id, downloaded, total) ->
        updateProgressForQueueItem(id.toString(), downloaded, total)
    }

    private val downloadStatusListener: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit = { (id, status) ->
        handleQueueStatusChanged(id.toString(), status)
    }

    private val downloadDeleteListener: (Int) -> Unit = { id ->
        handleQueueItemDeleted(id.toString())
    }

    init {
        // Attach listeners to MovieVortex's internal downloader events
        VideoDownloadManager.downloadProgressEvent += downloadProgressListener
        VideoDownloadManager.downloadStatusEvent += downloadStatusListener
        VideoDownloadManager.downloadDeleteEvent += downloadDeleteListener
        loadDownloads()
    }

    override fun onCleared() {
        VideoDownloadManager.downloadProgressEvent -= downloadProgressListener
        VideoDownloadManager.downloadStatusEvent -= downloadStatusListener
        VideoDownloadManager.downloadDeleteEvent -= downloadDeleteListener
        super.onCleared()
    }

    /**
     * Verifies that a file is not an HTML error response or placeholder.
     */
    fun isHtmlOrInvalidMediaFile(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 1024L) return true
            val buffer = ByteArray(512)
            val bytesRead = file.inputStream().use { it.read(buffer) }
            if (bytesRead <= 0) return true
            val headerStr = String(buffer, 0, bytesRead, Charsets.UTF_8).lowercase()
            headerStr.contains("<!doctype html") ||
                    headerStr.contains("<html") ||
                    headerStr.contains("<head") ||
                    headerStr.contains("<body") ||
                    headerStr.contains("{\"error\"") ||
                    headerStr.contains("404 not found") ||
                    headerStr.contains("403 forbidden")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Refreshes downloads list by scanning local files and reading internal download queue state.
     */
    fun loadDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadItems = mutableListOf<DownloadUiItem>()

            // 1. Scan external storage MovieVortex downloads directory
            try {
                val dir = File("/storage/emulated/0/Download/$DOWNLOAD_DIR_NAME")
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { file ->
                        file.isFile && (file.extension.equals("mp4", true) ||
                                file.extension.equals("mkv", true) ||
                                file.extension.equals("webm", true) ||
                                file.extension.equals("avi", true))
                    }?.forEach { file ->
                        if (!isHtmlOrInvalidMediaFile(file)) {
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning local downloads", e)
            }

            // 2. Scan internal Download cache
            try {
                val ctx = CloudStreamApp.context
                if (ctx != null) {
                    ctx.getKeys(DOWNLOAD_EPISODE_CACHE)?.forEach { key ->
                        val cachedEp = ctx.getKey<DownloadObjects.DownloadEpisodeCached>(key)
                        if (cachedEp != null) {
                            val info = VideoDownloadManager.getDownloadFileInfo(ctx, cachedEp.id)
                            if (info != null && info.fileLength > 1024L) {
                                val title = cachedEp.name ?: "Episode ${cachedEp.episode}"
                                val header = ctx.getKey<DownloadObjects.DownloadHeaderCached>(
                                    DOWNLOAD_HEADER_CACHE,
                                    cachedEp.parentId.toString()
                                )
                                val fullTitle = if (header != null && !title.startsWith(header.name)) "${header.name} - $title" else title
                                if (downloadItems.none { it.id == cachedEp.id.toString() || it.title == fullTitle }) {
                                    val isDone = info.totalBytes > 0 && info.fileLength >= info.totalBytes
                                    val prog = if (info.totalBytes > 0) (info.fileLength.toFloat() / info.totalBytes.toFloat()).coerceIn(0f, 1f) else 1.0f
                                    downloadItems.add(
                                        DownloadUiItem(
                                            id = cachedEp.id.toString(),
                                            title = fullTitle,
                                            poster = cachedEp.poster ?: header?.poster,
                                            quality = "1080p",
                                            fileSizeBytes = info.totalBytes,
                                            downloadedBytes = info.fileLength,
                                            progress = prog,
                                            state = if (isDone) MediaDownloadState.Completed else MediaDownloadState.Downloading,
                                            localFilePath = info.path.toString()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading download cache", e)
            }

            // 3. Active downloads from internal persistent queue
            try {
                val queue = DownloadQueueManager.queue.value
                queue.forEach { queueWrapper ->
                    val item = queueWrapper.downloadItem
                    val resumeEp = queueWrapper.resumePackage?.item?.ep
                    val id = queueWrapper.id
                    val title = item?.resultName ?: resumeEp?.mainName ?: resumeEp?.name ?: "Media Download"
                    val poster = item?.resultPoster ?: resumeEp?.poster
                    val cleanName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")

                    if (downloadItems.none { it.title == title || it.id == id.toString() }) {
                        downloadItems.add(
                            0,
                            DownloadUiItem(
                                id = id.toString(),
                                title = title,
                                poster = poster,
                                quality = "1080p",
                                fileSizeBytes = 0L,
                                downloadedBytes = 0L,
                                progress = 0f,
                                state = MediaDownloadState.Downloading,
                                localFilePath = "/storage/emulated/0/Download/$DOWNLOAD_DIR_NAME/$cleanName.mp4"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading internal queue", e)
            }

            // Preserve active in-memory states (downloading, failed, paused)
            val activeInMemory = _uiState.value.items.filter {
                it.state == MediaDownloadState.Downloading || it.state == MediaDownloadState.Failed || it.state == MediaDownloadState.Paused
            }
            activeInMemory.forEach { activeItem ->
                if (downloadItems.none { it.id == activeItem.id || it.title == activeItem.title }) {
                    downloadItems.add(0, activeItem)
                }
            }

            _uiState.value = DownloadsUiState(
                items = downloadItems,
                activeDownloadsCount = downloadItems.count { it.state == MediaDownloadState.Downloading }
            )
        }
    }

    private fun updateProgressForQueueItem(id: String, downloaded: Long, total: Long) {
        val prog = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                if (item.id == id || item.id == (item.title.hashCode()).toString()) {
                    item.copy(
                        downloadedBytes = downloaded,
                        fileSizeBytes = if (total > 0L) total else item.fileSizeBytes,
                        progress = prog,
                        state = if (prog >= 1.0f) MediaDownloadState.Completed else MediaDownloadState.Downloading
                    )
                } else item
            },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    private fun handleQueueStatusChanged(id: String, status: VideoDownloadManager.DownloadType) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { item ->
                if (item.id == id) {
                    when (status) {
                        VideoDownloadManager.DownloadType.IsDone -> {
                            item.copy(
                                state = MediaDownloadState.Completed,
                                progress = 1.0f,
                                downloadedBytes = if (item.fileSizeBytes > 0) item.fileSizeBytes else item.downloadedBytes,
                                errorMessage = null
                            )
                        }
                        VideoDownloadManager.DownloadType.IsPaused -> {
                            item.copy(state = MediaDownloadState.Paused)
                        }
                        VideoDownloadManager.DownloadType.IsFailed -> {
                            item.copy(
                                state = MediaDownloadState.Failed,
                                errorMessage = "Stream download failed from provider."
                            )
                        }
                        VideoDownloadManager.DownloadType.IsStopped -> {
                            item.copy(state = MediaDownloadState.Cancelled)
                        }
                        VideoDownloadManager.DownloadType.IsDownloading -> {
                            item.copy(state = MediaDownloadState.Downloading)
                        }
                        else -> item
                    }
                } else item
            },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    private fun handleQueueItemDeleted(id: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filter { it.id != id },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    fun pauseDownload(id: String) {
        val intId = id.toIntOrNull()
        if (intId != null) {
            VideoDownloadManager.downloadEvent.invoke(intId to VideoDownloadManager.DownloadActionType.Pause)
        }
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == id) it.copy(state = MediaDownloadState.Paused) else it
            },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    fun resumeDownload(id: String) {
        val intId = id.toIntOrNull()
        if (intId != null) {
            VideoDownloadManager.downloadEvent.invoke(intId to VideoDownloadManager.DownloadActionType.Resume)
        }
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == id) it.copy(state = MediaDownloadState.Downloading) else it
            },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    fun retryDownload(context: Context, item: DownloadUiItem) {
        val media = item.originalMedia
        if (media != null) {
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filter { it.id != item.id }
            )
            downloadMedia(context, media, item.originalEpisode)
        } else {
            Toast.makeText(context, "Cannot retry: original source details missing.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownload(id: String) {
        val itemToDelete = _uiState.value.items.find { it.id == id }

        val intId = id.toIntOrNull()
        if (intId != null) {
            try {
                DownloadQueueManager.cancelDownload(intId)
                CloudStreamApp.context?.let { ctx ->
                    VideoDownloadManager.deleteFileAndUpdateSettings(ctx, intId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling download for id $id: ${e.message}")
            }
        }

        // Delete physical file if present
        if (itemToDelete?.localFilePath != null) {
            try {
                if (itemToDelete.localFilePath.startsWith("content://")) {
                    CloudStreamApp.context?.contentResolver?.delete(Uri.parse(itemToDelete.localFilePath), null, null)
                } else {
                    val f = File(itemToDelete.localFilePath)
                    if (f.exists()) f.delete()
                }
            } catch (_: Exception) {}
        }

        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filter { it.id != id },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }

    /**
     * Executes real in-app download pipeline:
     * Media Selection -> Provider Resolution -> Stream Validation -> In-App Queue -> Verification.
     * Keeps all downloads and notifications strictly inside MovieVortex without invoking external apps.
     */
    fun downloadMedia(
        context: Context,
        media: MovieVortexMedia,
        episode: MediaEpisode? = null,
        streamUrl: String? = null
    ) {
        val title = if (episode != null) {
            "${media.title} S${episode.seasonNumber}E${episode.episodeNumber}"
        } else {
            media.title
        }
        val cleanName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val localPath = "/storage/emulated/0/Download/$DOWNLOAD_DIR_NAME/$cleanName.mp4"

        // 1. Duplicate check: Already downloaded and verified
        val targetFile = File(localPath)
        if (targetFile.exists() && !isHtmlOrInvalidMediaFile(targetFile)) {
            Toast.makeText(context, "Already downloaded and available offline!", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Duplicate check: Already actively downloading
        if (_uiState.value.items.any { (it.title == title || it.localFilePath == localPath) && it.state == MediaDownloadState.Downloading }) {
            Toast.makeText(context, "Download already in progress for: $title", Toast.LENGTH_SHORT).show()
            return
        }

        val newId = System.currentTimeMillis().toString()
        val newItem = DownloadUiItem(
            id = newId,
            title = title,
            mediaType = media.type,
            poster = episode?.poster ?: media.poster,
            quality = "1080p",
            fileSizeBytes = 0L,
            downloadedBytes = 0L,
            progress = 0f,
            state = MediaDownloadState.Queued,
            localFilePath = localPath,
            originalMedia = media,
            originalEpisode = episode
        )

        _uiState.value = _uiState.value.copy(
            items = listOf(newItem) + _uiState.value.items.filter { it.title != title },
            activeDownloadsCount = _uiState.value.activeDownloadsCount + 1
        )

        Toast.makeText(context, "Resolving video links for: $title...", Toast.LENGTH_SHORT).show()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = APIHolder.getApiFromNameNull(media.providerApiName)
                    ?: APIHolder.apis.firstOrNull { media.rawUrl.startsWith(it.mainUrl) }
                    ?: APIHolder.apis.firstOrNull()

                if (api == null) {
                    val errMsg = "Provider '${media.providerApiName}' is not loaded or active."
                    Log.e(TAG, errMsg)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        updateItemState(newId, MediaDownloadState.Failed, errMsg)
                    }
                    return@launch
                }

                val repo = APIRepository(api)

                // Resolve real media dataUrl
                var targetDataUrl = episode?.dataUrl
                if (targetDataUrl.isNullOrBlank() && media.rawUrl.isNotBlank()) {
                    val loadRes = repo.load(media.rawUrl)
                    if (loadRes is Resource.Success) {
                        targetDataUrl = when (val resVal = loadRes.value) {
                            is MovieLoadResponse -> resVal.dataUrl.ifBlank { media.rawUrl }
                            is TvSeriesLoadResponse -> resVal.episodes.firstOrNull()?.data ?: media.rawUrl
                            is AnimeLoadResponse -> resVal.episodes.values.flatten().firstOrNull()?.data ?: media.rawUrl
                            else -> media.rawUrl
                        }
                    }
                }
                if (targetDataUrl.isNullOrBlank()) {
                    targetDataUrl = media.rawUrl
                }

                val extractedLinks = mutableListOf<ExtractorLink>()
                val extractedSubs = mutableListOf<SubtitleData>()

                if (targetDataUrl.isNotBlank()) {
                    repo.loadLinks(
                        data = targetDataUrl,
                        isCasting = false,
                        subtitleCallback = { sub ->
                            extractedSubs.add(PlayerSubtitleHelper.getSubtitleData(sub))
                        },
                        callback = { link ->
                            val urlLower = link.url.lowercase()
                            if (link.url.isNotBlank() &&
                                (urlLower.startsWith("http://") || urlLower.startsWith("https://")) &&
                                !urlLower.contains(".html") && !urlLower.contains("/embed/")
                            ) {
                                extractedLinks.add(link)
                            }
                        }
                    )
                }

                if (extractedLinks.isEmpty() && streamUrl.isNullOrBlank()) {
                    val errMsg = "No downloadable media stream found for $title."
                    Log.w(TAG, errMsg)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        updateItemState(newId, MediaDownloadState.Failed, errMsg)
                    }
                    return@launch
                }

                val epNumber = episode?.episodeNumber ?: 1
                val seasonNumber = episode?.seasonNumber ?: 1
                val tvType = if (media.type == MediaType.Movie) TvType.Movie else TvType.TvSeries

                val resultEp = ResultEpisode(
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
                    videoWatchState = VideoWatchState.None
                )

                // Queue into MovieVortex's internal in-app downloader
                val queueItem = DownloadObjects.DownloadQueueItem(
                    episode = resultEp,
                    isMovie = media.type == MediaType.Movie,
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
                updateItemState(newId, MediaDownloadState.Downloading, null)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "MovieVortex download started: $title", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errMsg = "Download failed: ${e.message ?: "Unknown error"}"
                Log.e(TAG, "Download resolution error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                    updateItemState(newId, MediaDownloadState.Failed, errMsg)
                }
            }
        }
    }

    private fun updateItemState(id: String, state: MediaDownloadState, error: String?) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == id) it.copy(state = state, errorMessage = error) else it
            },
            activeDownloadsCount = _uiState.value.items.count { it.state == MediaDownloadState.Downloading }
        )
    }
}
