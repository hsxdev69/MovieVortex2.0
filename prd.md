# MovieVortex Backend Architecture & Integration PRD
**Target Audience**: AI Coding Assistants, Core Systems Developers, and Frontend Integration Engineers  
**Application**: MovieVortex Android (Engineered on CloudStream 3 Modular Framework)  
**Document Version**: 2.0.0-STABLE  
**Format**: Comprehensive Technical Specification & Implementation Guide

---

## 1. Executive Summary & Architectural Overview

MovieVortex is a high-performance Android streaming application. Its architecture separates a **modern Jetpack Compose presentation layer** from a **production-hardened modular backend core** adapted from the open-source CloudStream 3 ecosystem.

```
+-----------------------------------------------------------------------------------+
|                        MovieVortex UI Layer (Jetpack Compose)                      |
|  - HomeScreen         - DetailScreen     - AnimeScreen        - SearchScreen      |
|  - DownloadsScreen    - MyListScreen     - SettingsScreen     - Splash/DNS        |
+-----------------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------------+
|               Controller & Normalization Layer (com.movievortex.android)          |
|  - CoreAdapter: Normalizes raw responses into unified MovieVortexMedia models     |
|  - Controllers: HomeController, DetailController, DownloadController, etc.       |
|  - VortexPluginBootstrap: Auto-installs & activates extension repository plugins  |
+-----------------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------------+
|               CloudStream 3 Core Engine (com.lagradost.cloudstream3)              |
|  - Provider Registry (APIHolder, MainAPI, APIRepository)                          |
|  - Stream Extractor Pipeline (ExtractorLink, ExtractorApi, SubtitleFile)         |
|  - Native In-App Player (DownloadedPlayerActivity, GeneratorPlayer, CS3IPlayer)   |
|  - Persistent Downloader (VideoDownloadManager, DownloadQueueManager)             |
|  - Local Storage & Cache (DataStoreHelper, DOWNLOAD_EPISODE_CACHE)                |
+-----------------------------------------------------------------------------------+
```

### Core Architecture Principles for Any AI or Developer
1. **Zero Mock Implementations**: All metadata, poster URLs, and stream links are extracted live from real providers.
2. **Never Re-architect the Native Player**: The video player (`DownloadedPlayerActivity`, `GeneratorPlayer`, `FullScreenPlayer`, `CS3IPlayer`) is heavily optimized with ExoPlayer/mpv backends, hardware decoding, subtitle synchronization, and custom renderers. Do NOT replace this player with a generic Compose `AndroidView(VideoView)`.
3. **Encapsulated In-App Downloader**: MovieVortex uses its own in-app `VideoDownloadManager` and stores physical files into `/storage/emulated/0/Download/MovieVortex/`. It does not rely on external download managers.
4. **Deterministic Normalization**: UI components interact only with normalized models (`MovieVortexMedia`, `MediaEpisode`, `StreamSource`), never directly with raw reflection objects.

---

## 2. Core Infrastructure & Lifecycle

### 2.1 Application Entrypoint (`MovieVortexActivity.kt`)
The primary UI host is `com.movievortex.android.MovieVortexActivity`.

```kotlin
package com.movievortex.android

class MovieVortexActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 1. Register activity instance for CloudStream core utilities
        CommonActivity.setActivityInstance(this)

        // 2. Handle intent navigation (e.g. download notification taps)
        handleNavigationIntent(intent)

        // 3. Auto-bootstrap required media providers in background
        ioSafe {
            VortexPluginBootstrap.bootstrap(this@MovieVortexActivity)
        }

        // 4. Set Jetpack Compose Content
        setContent {
            MovieVortexTheme {
                MovieVortexNavGraph(...)
            }
        }
    }
}
```

### 2.2 Deep Linking & Intent Navigation
When a download completes, the background notification launches `MovieVortexActivity` with `DOWNLOAD_NAVIGATE_TO` extra:
- Extra key: `DOWNLOAD_NAVIGATE_TO` or `NAVIGATE_TO = "downloads"`
- The activity reads this in `handleNavigationIntent(intent)` and immediately switches `targetDestination` to `MainDestination.Downloads`.

---

## 3. Plugin & Provider Subsystem (`VortexPluginBootstrap.kt`)

MovieVortex loads streaming providers dynamically as `.cs3` plugins from the **Phisher Repo**.

### 3.1 Repository Details
- **Repo Name**: `Phisher Repo`
- **Repo URL**: `https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json`

### 3.2 Target Pre-installed Plugins
| Plugin Name | Internal Name | Primary Role | Fallback Binary URL |
| :--- | :--- | :--- | :--- |
| **AniVortex** | `AniVortex` | Anime movies, series, sub/dub episodes | `.../builds/AniVortex.cs3` |
| **IStreamFlare** | `IStreamFlare` | Western movies, Hollywood, English TV series | `.../builds/IStreamFlare.cs3` |
| **MovieBoxProvider** | `MovieBoxProvider`| Global blockbuster movies & multi-quality streams | `.../builds/MovieBoxProvider.cs3` |

### 3.3 Bootstrap Execution Flow
1. Add `RepositoryData` via `RepositoryManager.addRepository(repoData)`.
2. Load any pre-cached local plugins using `PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins`.
3. Fetch repo metadata online via `RepositoryManager.getRepoPlugins`.
4. Check if target `.cs3` files exist in internal app storage (`PluginManager.getPluginPath`).
5. If missing, download and load dynamically with `PluginManager.downloadPlugin(..., loadPlugin = true)`.
6. Broadcast lifecycle events to notify active ViewModels:
   ```kotlin
   MainActivity.mainPluginsLoadedEvent.invoke(true)
   MainActivity.afterPluginsLoadedEvent.invoke(true)
   ```

---

## 4. Provider & Stream Extractor Pipeline

All content sources in MovieVortex implement `MainAPI`.

### 4.1 Accessing Active Providers
```kotlin
import com.lagradost.cloudstream3.APIHolder

// List all registered providers
val allApis: List<MainAPI> = APIHolder.apis

// Find a specific provider by its unique identifier
val provider: MainAPI? = APIHolder.getApiFromNameNull("IStreamFlare")
    ?: APIHolder.apis.firstOrNull { it.hasMainPage }
```

### 4.2 Querying Provider Content (`APIRepository`)
Always wrap `MainAPI` instances inside `APIRepository` to safely execute suspended coroutine queries:
```kotlin
val repo = APIRepository(provider)

// 1. Fetch Home Page Rows
val homeResult: Resource<List<HomePageResponse?>> = repo.getMainPage(page = 1)

// 2. Search
val searchResult: Resource<List<SearchResponse>> = repo.search(query = "Interstellar")

// 3. Load Metadata & Episode Structure
val detailResult: Resource<LoadResponse> = repo.load(url = contentUrl)

// 4. Resolve Stream Links and Subtitles
repo.loadLinks(
    data = episodeDataUrl,
    isCasting = false,
    subtitleCallback = { subtitleFile ->
        // SubtitleFile(lang, url)
    },
    callback = { extractorLink ->
        // ExtractorLink(source, name, url, referer, quality, isM3u8, isDash, headers)
    }
)
```

---

## 5. Unified Data Normalization (`CoreAdapter.kt`)

Raw CloudStream responses are polymorphic and must be converted to `MovieVortexMedia` before passing to UI screens.

### 5.1 Unified Data Models (`MovieVortexMedia.kt`)
```kotlin
data class MovieVortexMedia(
    val id: String,                         // Format: "${providerApiName}_${rawUrl}"
    val title: String,
    val originalTitle: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val type: MediaType,                    // Movie, TvSeries, Anime, LiveStream
    val year: Int? = null,
    val rating: Float = 0f,
    val runtime: Int? = null,               // In minutes
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<MediaCast> = emptyList(),
    val trailerUrl: String? = null,
    val seasons: List<MediaSeason> = emptyList(),
    val episodes: List<MediaEpisode> = emptyList(),
    val watchProgress: Float = 0f,          // Coerced 0.0f - 1.0f
    val isInMyList: Boolean = false,
    val rawUrl: String,                     // Provider's unique content URI
    val providerApiName: String             // Name of originating MainAPI
)

data class MediaEpisode(
    val id: String,                         // Extractor data identifier
    val title: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val poster: String?,
    val synopsis: String?,
    val dataUrl: String,                    // Passed to repo.loadLinks()
    val watchProgress: Float = 0f,
    val isWatched: Boolean = false
)

data class StreamSource(
    val url: String,
    val quality: String,                    // "4K UHD", "1080p FHD", "720p HD", "Auto"
    val headers: Map<String, String> = emptyMap(),
    val isM3u8: Boolean = false,
    val isDASH: Boolean = false,
    val extractorName: String = "Direct Stream"
)
```

### 5.2 Key Conversion Methods in `CoreAdapter`
- `CoreAdapter.fromSearchResponse(SearchResponse): MovieVortexMedia`
- `CoreAdapter.fromLoadResponse(LoadResponse, providerApi: String): MovieVortexMedia`
- `CoreAdapter.fromEpisode(Episode, parentUrl: String): MediaEpisode`
- `CoreAdapter.fromExtractorLink(ExtractorLink): StreamSource`
- `CoreAdapter.fromSubtitleFile(SubtitleFile): SubtitleTrack`

### 5.3 Watch Progress & Bookmarking Persistence
State is stored in the app's internal key-value database using `DataStoreHelper`:
- **Read Resume Progress**:
  ```kotlin
  val resume = DataStoreHelper.getViewPos(contentUrl.hashCode())
  val progress = if (resume != null && resume.duration > 0) {
      (resume.position.toFloat() / resume.duration.toFloat()).coerceIn(0f, 1f)
  } else 0f
  ```
- **Read Bookmark State**:
  ```kotlin
  val isBookmarked = DataStoreHelper.getBookmarkedData(contentUrl.hashCode()) != null
  ```
- **Toggle Bookmark**:
  ```kotlin
  DataStoreHelper.setBookmarkedData(contentUrl.hashCode(), bookmarkedObject)
  ```

---

## 6. Video Player Architecture (`DownloadedPlayerActivity.kt`)

MovieVortex relies on the native CloudStream player activity: `com.lagradost.cloudstream3.ui.player.DownloadedPlayerActivity`.

### 6.1 Launching Online Streaming Playback
To stream movies or series episodes, invoke `launchCloudStreamPlayer`:
```kotlin
fun launchCloudStreamPlayer(
    context: Context,
    media: MovieVortexMedia,
    episode: MediaEpisode? = null
) {
    val intent = Intent(context, DownloadedPlayerActivity::class.java).apply {
        putExtra(MainActivity.API_NAME_EXTRA_KEY, media.providerApiName)
        putExtra("CONTENT_URL", media.rawUrl)
        putExtra("TITLE", media.title)
        if (episode != null) {
            putExtra("EPISODE", episode.episodeNumber)
            putExtra("SEASON", episode.seasonNumber)
            putExtra("EPISODE_DATA", episode.dataUrl)
        }
    }
    context.startActivity(intent)
}
```

### 6.2 Launching Offline Downloaded Playback
When playing a locally saved video file from the downloads folder:
```kotlin
fun launchOfflinePlayer(context: Context, localFilePath: String, title: String? = null) {
    val file = File(localFilePath)
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } else {
        Uri.fromFile(file)
    }

    val intent = Intent(context, DownloadedPlayerActivity::class.java).apply {
        putExtra("OFFLINE_PATH", localFilePath)
        if (!title.isNullOrBlank()) {
            putExtra("TITLE", title)
        }
        action = Intent.ACTION_VIEW
        setDataAndType(uri, "video/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
```

### 6.3 Player Engine Initialization Inside `DownloadedPlayerActivity`
`DownloadedPlayerActivity` dispatches playback using `GeneratorPlayer`:
- For **Online Streams**: Uses `RepoFileGenerator` which queries `APIRepository.loadLinks()` asynchronously and updates audio/subtitle tracks.
- For **Offline Files**:
  ```kotlin
  val extractorUri = ExtractorUri(uri = uri, name = displayName, id = path.hashCode())
  val args = GeneratorPlayer.newInstance(DownloadFileGenerator(listOf(extractorUri)), 0)
  navigateToPlayer(args)
  ```
- **Critical Fix**: `navigateToPlayer(args)` uses `window.decorView.post { ... }` to ensure the fragment view hierarchy is attached before navigation is dispatched, preventing black screens or crashes.

---

## 7. In-App Download Manager Pipeline (`DownloadController.kt`)

Downloads are processed entirely within MovieVortex using `VideoDownloadManager` and `DownloadQueueManager`.

### 7.1 Pipeline Stages
```
[1. User Taps Download] 
        │
        ▼
[2. Duplicate Check] ──> Check if local file exists & verified OR currently in queue
        │
        ▼
[3. Resolve Stream Links] ──> Invoke repo.loadLinks(targetDataUrl) on background IO
        │
        ▼
[4. Filter Real Media URLs] ──> Exclude HTML pages, embeds, or bad status codes
        │
        ▼
[5. Enqueue Download] ──> DownloadQueueManager.addToQueue(queueItem.toWrapper())
        │
        ▼
[6. Byte-Accurate Transfer] ──> Listen to VideoDownloadManager.downloadProgressEvent
        │
        ▼
[7. Completion & Validation] ──> Verify header: not HTML, size > 1KB, valid container
```

### 7.2 Storage Path & Naming Conventions
- **Base Directory**: `/storage/emulated/0/Download/MovieVortex/`
- **Filename Sanitization**:
  ```kotlin
  val cleanName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
  val localPath = "/storage/emulated/0/Download/MovieVortex/$cleanName.mp4"
  ```

### 7.3 Integrity Verification (`isHtmlOrInvalidMediaFile`)
To prevent invalid downloads (such as server error pages or Captcha blocks being saved as movies):
```kotlin
fun isHtmlOrInvalidMediaFile(file: File): Boolean {
    if (!file.exists() || file.length() < 1024L) return true
    val buffer = ByteArray(512)
    val bytesRead = file.inputStream().use { it.read(buffer) }
    if (bytesRead <= 0) return true
    val headerStr = String(buffer, 0, bytesRead, Charsets.UTF_8).lowercase()
    return headerStr.contains("<!doctype html") ||
           headerStr.contains("<html") ||
           headerStr.contains("<body") ||
           headerStr.contains("{\"error\"") ||
           headerStr.contains("404 not found") ||
           headerStr.contains("403 forbidden")
}
```

### 7.4 Downloader Event Bus
`DownloadController` registers three event listeners to track real background progress:
1. `VideoDownloadManager.downloadProgressEvent += { (id, downloaded, total) -> ... }`
2. `VideoDownloadManager.downloadStatusEvent += { (id, status) -> ... }`
3. `VideoDownloadManager.downloadDeleteEvent += { id -> ... }`

---

## 8. ViewModels & Controllers Reference

| Controller | Package | Primary Responsibilities |
| :--- | :--- | :--- |
| `HomeController` | `com.movievortex.android.controller` | Loads `repo.getMainPage(1)`, populates Hero carousel, handles OTT brand filters (Netflix, Disney+, Max, etc.), and reads Continue Watching. |
| `DetailController` | `com.movievortex.android.controller` | Calls `repo.load(rawUrl)`, decomposes seasons/episodes, manages active season selection, and reads synopsis/cast. |
| `AnimeController` | `com.movievortex.android.controller` | Specifically routes queries to `AniVortex` provider for specialized Anime feeds and Japanese audio / subtitle tags. |
| `SearchController` | `com.movievortex.android.controller` | Runs multi-threaded search across all active providers via `repo.search(query)` and aggregates normalized results. |
| `DownloadController`| `com.movievortex.android.controller` | Manages download queue, tracks byte transfer, scans local storage for completed files, handles pause/resume/delete, and triggers offline playback. |
| `PlayerController` | `com.movievortex.android.controller` | State holder for playback lifecycle, quality switching, subtitle selection, and playback time tracking. |
| `SettingsController`| `com.movievortex.android.controller` | Manages DoH (DNS over HTTPS) provider configuration (Cloudflare, Google, AdGuard) and app-level preferences. |

---

## 9. Concrete Integration Code Examples

### Example 1: Loading Details & Playing an Episode
```kotlin
// In your Composable or Activity:
val detailController: DetailController = viewModel()
val media: MovieVortexMedia = ...

// 1. Fetch details
detailController.loadMediaDetail(media.rawUrl, media.providerApiName, media)

// 2. Play episode
val episodeToPlay = detailController.uiState.value.currentSeasonEpisodes.first()
launchCloudStreamPlayer(context, media, episodeToPlay)
```

### Example 2: Initiating a Movie Download
```kotlin
val downloadController: DownloadController = viewModel()
val movieMedia: MovieVortexMedia = ...

// Starts resolution and queues file into /storage/emulated/0/Download/MovieVortex/
downloadController.downloadMedia(context = context, media = movieMedia)
```

### Example 3: Playing an Offline Downloaded File
```kotlin
val downloadItem: DownloadUiItem = ...
if (downloadItem.state == MediaDownloadState.Completed && downloadItem.localFilePath != null) {
    launchOfflinePlayer(
        context = context,
        localFilePath = downloadItem.localFilePath,
        title = downloadItem.title
    )
}
```

---

## 10. Developer Rules & Implementation Constraints

When working on or enhancing this codebase, any developer or AI MUST strictly observe the following rules:

1. **NEVER modify or rewrite the Video Player core**:
   - Files `DownloadedPlayerActivity.kt`, `FullScreenPlayer.kt`, `player_custom_layout.xml`, and `CS3IPlayer.kt` MUST remain untouched.
   - Do NOT replace `DownloadedPlayerActivity` with a custom video player view.
2. **NEVER use mock data or fake URLs**:
   - Always resolve streams through `APIRepository.loadLinks()` or read physical files from `/storage/emulated/0/Download/MovieVortex/`.
3. **NEVER change download directory locations**:
   - Completed videos must always reside in `/storage/emulated/0/Download/MovieVortex/` to allow seamless local scanning and offline playback.
4. **Preserve `CommonActivity.setActivityInstance`**:
   - CloudStream core methods require an active Activity context reference. If creating a new Activity, call `CommonActivity.setActivityInstance(this)` in `onCreate()` and `onResume()`.
5. **Always Verify Files**:
   - Never mark a download as completed without running `isHtmlOrInvalidMediaFile(file)` to verify that real media bytes were written.
