package com.movievortex.android.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import com.movievortex.android.core.CoreAdapter
import com.movievortex.android.model.MovieVortexMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SearchStatus {
    Idle,
    Searching,
    Results,
    NoResults,
    Error
}

data class SearchUiState(
    val query: String = "",
    val status: SearchStatus = SearchStatus.Idle,
    val results: List<MovieVortexMedia> = emptyList(),
    val recentQueries: List<String> = listOf("Stranger Things", "Cyberpunk", "Avengers", "Arcane", "Oppenheimer", "Attack on Titan"),
    val errorMessage: String? = null
)

class SearchController : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)

        if (newQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.value = _uiState.value.copy(
                status = SearchStatus.Idle,
                results = emptyList()
            )
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce search typing
            performSearch(newQuery)
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = "",
            status = SearchStatus.Idle,
            results = emptyList(),
            errorMessage = null
        )
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                query = query,
                status = SearchStatus.Searching,
                errorMessage = null
            )

            try {
                val apis = APIHolder.apis
                if (apis.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        status = SearchStatus.NoResults,
                        results = emptyList()
                    )
                    return@launch
                }

                // Query providers in parallel
                val searchResults: List<MovieVortexMedia> = withContext(Dispatchers.IO) {
                    apis.map { api ->
                        async<List<MovieVortexMedia>> {
                            try {
                                val res = api.search(query)
                                if (res != null) {
                                    res.map { CoreAdapter.fromSearchResponse(it) }
                                } else emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                // Deduplicate by title & url
                val distinctResults = searchResults.distinctBy { it.title.trim().lowercase() }

                if (distinctResults.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        status = SearchStatus.NoResults,
                        results = emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        status = SearchStatus.Results,
                        results = distinctResults
                    )
                }
            } catch (e: Exception) {
                // Rule 33: Never expose technical errors to normal users
                _uiState.value = _uiState.value.copy(
                    status = SearchStatus.Error,
                    results = emptyList(),
                    errorMessage = "This content is currently unavailable. Please try again."
                )
            }
        }
    }
}
