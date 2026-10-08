package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.movievortex.android.core.CoreAdapter
import com.movievortex.android.model.MediaEpisode
import com.movievortex.android.model.MediaSeason
import com.movievortex.android.model.MovieVortexMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val isLoading: Boolean = true,
    val media: MovieVortexMedia? = null,
    val selectedSeason: Int = 1,
    val currentSeasonEpisodes: List<MediaEpisode> = emptyList(),
    val isTrailerPlaying: Boolean = false,
    val isInMyList: Boolean = false,
    val errorMessage: String? = null
)

class DetailController : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadMediaDetail(rawUrl: String, apiName: String, initialMedia: MovieVortexMedia? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                media = initialMedia,
                errorMessage = null
            )

            try {
                val api = APIHolder.getApiFromNameNull(apiName)
                    ?: APIHolder.apis.firstOrNull { rawUrl.startsWith(it.mainUrl) }
                    ?: APIHolder.apis.firstOrNull()

                if (api == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Content provider is currently unavailable."
                    )
                    return@launch
                }

                val repo = APIRepository(api)
                val response = withContext(Dispatchers.IO) {
                    repo.load(rawUrl)
                }

                when (response) {
                    is Resource.Success -> {
                        val fullMedia = CoreAdapter.fromLoadResponse(response.value, api.name)
                        val initialSeason = fullMedia.seasons.firstOrNull()?.seasonNumber ?: 1
                        val seasonEps = fullMedia.episodes.filter { it.seasonNumber == initialSeason }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            media = fullMedia,
                            selectedSeason = initialSeason,
                            currentSeasonEpisodes = seasonEps,
                            isInMyList = fullMedia.isInMyList
                        )
                    }
                    is Resource.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "This content is currently unavailable. Please try again."
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "This content is currently unavailable. Please try again."
                )
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val currentMedia = _uiState.value.media ?: return
        val filtered = currentMedia.episodes.filter { it.seasonNumber == seasonNumber }
        _uiState.value = _uiState.value.copy(
            selectedSeason = seasonNumber,
            currentSeasonEpisodes = filtered
        )
    }

    fun toggleMyList() {
        val current = _uiState.value.media ?: return
        val newState = !_uiState.value.isInMyList

        // Persist bookmark via DataStoreHelper
        if (newState) {
            val bookmark = SyncAPI.SyncResult(
                id = current.id.hashCode(),
                name = current.title,
                url = current.rawUrl,
                apiName = current.providerApiName,
                type = CoreAdapter.toMediaType(null).name,
                posterUrl = current.poster
            )
            DataStoreHelper.setBookmarkedData(current.rawUrl, bookmark)
        } else {
            DataStoreHelper.removeBookmarkedData(current.rawUrl)
        }

        _uiState.value = _uiState.value.copy(
            isInMyList = newState,
            media = current.copy(isInMyList = newState)
        )
    }

    fun toggleTrailer() {
        _uiState.value = _uiState.value.copy(
            isTrailerPlaying = !_uiState.value.isTrailerPlaying
        )
    }
}
