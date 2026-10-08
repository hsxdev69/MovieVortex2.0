package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.movievortex.android.core.CoreAdapter
import com.movievortex.android.model.MediaType
import com.movievortex.android.model.MovieVortexMedia
import com.movievortex.android.model.StreamingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContentRow(
    val title: String,
    val items: List<MovieVortexMedia>
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroItems: List<MovieVortexMedia> = emptyList(),
    val continueWatching: List<MovieVortexMedia> = emptyList(),
    val platforms: List<StreamingPlatform> = emptyList(),
    val rows: List<ContentRow> = emptyList(),
    val exploreGrid: List<MovieVortexMedia> = emptyList(),
    val isExploreLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeController : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var explorePage = 1

    val defaultPlatforms = listOf(
        StreamingPlatform("netflix", "Netflix", "ic_netflix", 0xFFE50914, "Streaming giant"),
        StreamingPlatform("disney", "Disney+", "ic_disney", 0xFF113CCF, "Disney, Marvel, Star Wars"),
        StreamingPlatform("hbomax", "Max", "ic_max", 0xFF002BE7, "HBO, Warner Bros, DC"),
        StreamingPlatform("appletv", "Apple TV+", "ic_appletv", 0xFF1A1A1A, "Apple Original Stories"),
        StreamingPlatform("prime", "Prime Video", "ic_prime", 0xFF00A8E1, "Amazon Originals & Exclusives"),
        StreamingPlatform("paramount", "Paramount+", "ic_paramount", 0xFF0064FF, "Paramount, CBS, Showtime"),
        StreamingPlatform("hulu", "Hulu", "ic_hulu", 0xFF1CE783, "Hit movies and TV series")
    )

    init {
        loadHomeContent()
    }

    fun loadHomeContent(forceReload: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // 1. Continue Watching from DataStoreHelper
                val continueWatchingItems = loadContinueWatching()

                // 2. Load Content from Core Providers
                val apis = APIHolder.apis
                val mainApi = apis.firstOrNull { it.hasMainPage } ?: apis.firstOrNull()

                val rows = mutableListOf<ContentRow>()
                val heroList = mutableListOf<MovieVortexMedia>()
                val exploreList = mutableListOf<MovieVortexMedia>()

                if (mainApi != null) {
                    val repo = APIRepository(mainApi)
                    val result = withContext(Dispatchers.IO) {
                        repo.getMainPage(1)
                    }

                    when (result) {
                        is Resource.Success -> {
                            result.value.forEach { homePageResponse ->
                                homePageResponse?.items?.forEach { pageList ->
                                    val rowItems = pageList.list.map { CoreAdapter.fromSearchResponse(it) }
                                    if (rowItems.isNotEmpty()) {
                                        rows.add(ContentRow(title = pageList.name, items = rowItems))
                                    }
                                }
                            }
                        }
                        is Resource.Failure -> {
                            // Friendly fallback handled gracefully
                        }
                        else -> {}
                    }
                }

                // Populate Hero from first rows if available
                rows.firstOrNull()?.items?.take(5)?.let {
                    heroList.addAll(it)
                }

                // Seed explore grid
                rows.getOrNull(1)?.items?.let {
                    exploreList.addAll(it)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    heroItems = heroList,
                    continueWatching = continueWatchingItems,
                    platforms = defaultPlatforms,
                    rows = rows,
                    exploreGrid = exploreList
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    continueWatching = loadContinueWatching(),
                    platforms = defaultPlatforms,
                    errorMessage = "Unable to load content catalog right now."
                )
            }
        }
    }

    private fun loadContinueWatching(): List<MovieVortexMedia> {
        val resumeMap = DataStoreHelper.getAllResumeStateIds() ?: return emptyList()
        val list = mutableListOf<MovieVortexMedia>()

        resumeMap.take(10).forEach { resumeItem ->
            val pos = DataStoreHelper.getViewPos(resumeItem)
            if (pos != null && pos.duration > 0 && pos.position < pos.duration * 0.95) {
                val progress = (pos.position.toFloat() / pos.duration.toFloat()).coerceIn(0f, 1f)
                val bookmarked = DataStoreHelper.getBookmarkedData(resumeItem)
                val title = bookmarked?.name ?: "Watched Item"
                val poster = bookmarked?.posterUrl

                list.add(
                    MovieVortexMedia(
                        id = resumeItem,
                        title = title,
                        poster = poster,
                        backdrop = poster,
                        watchProgress = progress,
                        rawUrl = resumeItem
                    )
                )
            }
        }

        return list
    }

    fun removeContinueWatching(media: MovieVortexMedia) {
        DataStoreHelper.setViewPos(media.rawUrl, 0L, 0L)
        _uiState.value = _uiState.value.copy(
            continueWatching = _uiState.value.continueWatching.filter { it.id != media.id }
        )
    }

    fun loadMoreExplore() {
        if (_uiState.value.isExploreLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExploreLoading = true)
            explorePage++

            try {
                val apis = APIHolder.apis
                val mainApi = apis.firstOrNull { it.hasMainPage } ?: apis.firstOrNull()
                if (mainApi != null) {
                    val repo = APIRepository(mainApi)
                    val result = withContext(Dispatchers.IO) {
                        repo.getMainPage(explorePage)
                    }

                    if (result is Resource.Success) {
                        val newItems = result.value.flatMap { hp ->
                            hp?.items?.flatMap { it.list } ?: emptyList()
                        }.map { CoreAdapter.fromSearchResponse(it) }

                        _uiState.value = _uiState.value.copy(
                            exploreGrid = _uiState.value.exploreGrid + newItems,
                            isExploreLoading = false
                        )
                        return@launch
                    }
                }
            } catch (_: Exception) {}

            _uiState.value = _uiState.value.copy(isExploreLoading = false)
        }
    }
}
