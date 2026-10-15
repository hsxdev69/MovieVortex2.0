package com.movievortex.android.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movievortex.android.controller.AnimeController
import com.movievortex.android.controller.DetailController
import com.movievortex.android.controller.DownloadController
import com.movievortex.android.controller.HomeController
import com.movievortex.android.controller.LibraryController
import com.movievortex.android.controller.PlayerController
import com.movievortex.android.controller.SearchController
import com.movievortex.android.controller.SettingsController
import com.movievortex.android.model.MediaEpisode
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.model.StreamingPlatform
import com.movievortex.android.ui.anime.AnimeScreen
import com.movievortex.android.ui.catalog.PlatformCatalogScreen
import com.movievortex.android.ui.components.MainDestination
import com.movievortex.android.ui.components.MovieVortexBottomBar
import com.movievortex.android.ui.components.MovieVortexTopBar
import com.movievortex.android.ui.detail.DetailScreen
import com.movievortex.android.ui.dns.DnsIntroScreen
import com.movievortex.android.ui.downloads.DownloadsScreen
import com.movievortex.android.ui.home.HomeScreen
import com.movievortex.android.ui.player.MovieVortexPlayerScreen
import com.movievortex.android.ui.search.SearchScreen
import com.movievortex.android.ui.settings.SettingsScreen
import com.movievortex.android.ui.splash.MovieVortexSplashScreen
import com.movievortex.android.ui.theme.VortexColors

sealed class AppScreen {
    object Splash : AppScreen()
    object DnsIntro : AppScreen()
    object Main : AppScreen()
    object Search : AppScreen()
    data class PlatformCatalog(val platform: StreamingPlatform) : AppScreen()
    data class Detail(val media: MovieVortexMedia) : AppScreen()
    data class Player(val media: MovieVortexMedia, val episode: MediaEpisode? = null, val offlinePath: String? = null) : AppScreen()
}

fun launchCloudStreamPlayer(
    context: Context,
    media: MovieVortexMedia,
    episode: MediaEpisode? = null
) {
    try {
        val intent = Intent(context, com.lagradost.cloudstream3.ui.player.DownloadedPlayerActivity::class.java).apply {
            putExtra(com.lagradost.cloudstream3.MainActivity.API_NAME_EXTRA_KEY, media.providerApiName)
            putExtra("CONTENT_URL", media.rawUrl)
            putExtra("TITLE", media.title)
            if (episode != null) {
                putExtra("EPISODE", episode.episodeNumber)
                putExtra("SEASON", episode.seasonNumber)
                putExtra("EPISODE_DATA", episode.dataUrl)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("MovieVortex", "Error launching player", e)
    }
}

fun launchOfflinePlayer(context: Context, localFilePath: String?) {
    if (localFilePath.isNullOrBlank()) return
    try {
        val file = File(localFilePath)
        val uri = Uri.fromFile(file)
        val intent = Intent(context, com.lagradost.cloudstream3.ui.player.DownloadedPlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("MovieVortex", "Error launching offline player", e)
    }
}

@Composable
fun MovieVortexNavGraph(
    homeController: HomeController = viewModel(),
    animeController: AnimeController = viewModel(),
    searchController: SearchController = viewModel(),
    detailController: DetailController = viewModel(),
    playerController: PlayerController = viewModel(),
    downloadController: DownloadController = viewModel(),
    libraryController: LibraryController = viewModel(),
    settingsController: SettingsController,
    isFirstLaunch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
    var currentDestination by remember { mutableStateOf(MainDestination.MoviesTv) }

    val downloadState by downloadController.uiState.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is AppScreen.Splash -> {
                MovieVortexSplashScreen(
                    onSplashFinished = {
                        currentScreen = if (isFirstLaunch) AppScreen.DnsIntro else AppScreen.Main
                    }
                )
            }

            is AppScreen.DnsIntro -> {
                DnsIntroScreen(
                    settingsController = settingsController,
                    onContinue = { currentScreen = AppScreen.Main }
                )
            }

            is AppScreen.Main -> {
                Scaffold(
                    topBar = {
                        MovieVortexTopBar(
                            onSearchClick = { currentScreen = AppScreen.Search },
                            isTransparent = currentDestination == MainDestination.MoviesTv || currentDestination == MainDestination.Anime
                        )
                    },
                    bottomBar = {
                        MovieVortexBottomBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { currentDestination = it },
                            activeDownloadCount = downloadState.activeDownloadsCount
                        )
                    },
                    containerColor = VortexColors.PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentDestination, label = "tab_crossfade") { dest ->
                            when (dest) {
                                MainDestination.MoviesTv -> {
                                    HomeScreen(
                                        homeController = homeController,
                                        onMediaClick = { media ->
                                            detailController.loadMediaDetail(media.rawUrl, media.providerApiName, media)
                                            currentScreen = AppScreen.Detail(media)
                                        },
                                        onPlayClick = { media ->
                                            launchCloudStreamPlayer(context, media, null)
                                        },
                                        onPlatformClick = { platform ->
                                            currentScreen = AppScreen.PlatformCatalog(platform)
                                        }
                                    )
                                }

                                MainDestination.Anime -> {
                                    AnimeScreen(
                                        animeController = animeController,
                                        onMediaClick = { media ->
                                            detailController.loadMediaDetail(media.rawUrl, media.providerApiName, media)
                                            currentScreen = AppScreen.Detail(media)
                                        },
                                        onPlayClick = { media ->
                                            launchCloudStreamPlayer(context, media, null)
                                        }
                                    )
                                }

                                MainDestination.Downloads -> {
                                    DownloadsScreen(
                                        downloadController = downloadController,
                                        onBrowseClick = { currentDestination = MainDestination.MoviesTv },
                                        onPlayOffline = { item ->
                                            launchOfflinePlayer(context, item.localFilePath)
                                        }
                                    )
                                }

                                MainDestination.Settings -> {
                                    SettingsScreen(
                                        settingsController = settingsController,
                                        onOpenDnsSetup = { currentScreen = AppScreen.DnsIntro }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is AppScreen.Search -> {
                SearchScreen(
                    searchController = searchController,
                    onMediaClick = { media ->
                        detailController.loadMediaDetail(media.rawUrl, media.providerApiName, media)
                        currentScreen = AppScreen.Detail(media)
                    },
                    onBackClick = { currentScreen = AppScreen.Main }
                )
            }

            is AppScreen.PlatformCatalog -> {
                val homeState by homeController.uiState.collectAsState()
                PlatformCatalogScreen(
                    platform = screen.platform,
                    catalogItems = homeState.exploreGrid,
                    onMediaClick = { media ->
                        detailController.loadMediaDetail(media.rawUrl, media.providerApiName, media)
                        currentScreen = AppScreen.Detail(media)
                    },
                    onBackClick = { currentScreen = AppScreen.Main }
                )
            }

            is AppScreen.Detail -> {
                DetailScreen(
                    detailController = detailController,
                    onPlayMovie = { media ->
                        launchCloudStreamPlayer(context, media, null)
                    },
                    onPlayEpisode = { media, ep ->
                        launchCloudStreamPlayer(context, media, ep)
                    },
                    onDownloadClick = { media, ep ->
                        downloadController.downloadMedia(context, media, ep, null)
                        currentDestination = MainDestination.Downloads
                        currentScreen = AppScreen.Main
                    },
                    onBackClick = { currentScreen = AppScreen.Main }
                )
            }

            is AppScreen.Player -> {
                launchCloudStreamPlayer(context, screen.media, screen.episode)
                currentScreen = AppScreen.Main
            }
        }
    }
}
