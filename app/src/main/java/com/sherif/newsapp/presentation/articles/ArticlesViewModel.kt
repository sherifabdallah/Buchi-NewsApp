package com.sherif.newsapp.presentation.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.newsapp.domain.connectivity.NetworkMonitor
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.domain.usecase.ObserveTopHeadlinesUseCase
import com.sherif.newsapp.domain.usecase.RefreshTopHeadlinesUseCase
import com.sherif.newsapp.domain.usecase.SearchArticlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeTopHeadlines: ObserveTopHeadlinesUseCase,
    private val refreshTopHeadlines: RefreshTopHeadlinesUseCase,
    private val searchArticles: SearchArticlesUseCase,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    // Kept in SavedStateHandle so the search survives process death.
    private val searchQuery = savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val _state = MutableStateFlow(ArticlesState(query = searchQuery.value))
    val state: StateFlow<ArticlesState> = _state.asStateFlow()

    private val _effects = Channel<ArticlesEffect>(Channel.BUFFERED)
    val effects: Flow<ArticlesEffect> = _effects.receiveAsFlow()

    private val searchRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var refreshJob: Job? = null

    init {
        observeTopHeadlines()
            .onEach { articles -> _state.update { it.copy(headlines = it.headlines.copy(articles = articles)) } }
            .launchIn(viewModelScope)

        networkMonitor.isOnline
            .onEach(::onConnectivityChanged)
            .launchIn(viewModelScope)

        observeSearchQuery()
        refreshHeadlines()
    }

    fun onIntent(intent: ArticlesIntent) {
        when (intent) {
            is ArticlesIntent.QueryChanged -> onQueryChanged(intent.query)
            ArticlesIntent.ClearQuery -> onQueryChanged("")
            ArticlesIntent.Refresh -> refresh()
        }
    }

    private fun onQueryChanged(query: String) {
        _state.update {
            // Clearing the field should bring the headlines back right away, not after the debounce.
            if (query.isBlank()) it.copy(query = query, searchResults = null) else it.copy(query = query)
        }
        savedStateHandle[KEY_QUERY] = query
    }

    private fun refresh() {
        val current = _state.value
        val showIndicator = current.visibleList.articles.isNotEmpty()
        if (current.hasQuery) {
            if (showIndicator) _state.update { it.copy(isRefreshing = true) }
            searchRetries.tryEmit(Unit)
        } else {
            refreshHeadlines(showIndicator)
        }
    }

    private fun refreshHeadlines(showIndicator: Boolean = false) {
        if (showIndicator) _state.update { it.copy(isRefreshing = true) }
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _state.update { it.copy(headlines = it.headlines.copy(isLoading = true, error = null)) }
            val headlines = when (val result = refreshTopHeadlines()) {
                is DataResult.Success -> ArticleListState(articles = result.data)
                // Ask the database rather than the current state: on a cold start the request can
                // fail before Room has delivered the cached list.
                is DataResult.Failure -> ArticleListState(articles = observeTopHeadlines().first(), error = result.error)
            }
            _state.update { it.copy(headlines = headlines, isRefreshing = false) }

            // With nothing cached the error screen already explains what happened,
            // and when offline the banner does, so the snackbar is only for the remaining case.
            val error = headlines.error
            if (error != null && headlines.articles.isNotEmpty() && !_state.value.isOffline) {
                _effects.send(ArticlesEffect.ShowRefreshError(error))
            }
        }
    }

    private fun observeSearchQuery() {
        val typedQueries = searchQuery
            .map { it.trim() }
            .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
            .distinctUntilChanged()
        val retries = searchRetries.map { searchQuery.value.trim() }

        viewModelScope.launch {
            // collectLatest cancels a request that's still running when a newer query arrives.
            merge(typedQueries, retries).collectLatest(::search)
        }
    }

    private suspend fun search(query: String) {
        if (query.isEmpty()) {
            _state.update { it.copy(searchResults = null, isRefreshing = false) }
            return
        }

        // The previous results stay on screen while the new ones load.
        _state.update {
            it.copy(searchResults = ArticleListState(articles = it.searchResults?.articles.orEmpty(), isLoading = true))
        }
        val result = searchArticles(query)
        _state.update {
            it.copy(
                searchResults = ArticleListState(
                    articles = result.articles,
                    error = (result as? SearchResult.Cached)?.error,
                    isFromCache = result is SearchResult.Cached,
                ),
                isRefreshing = false,
            )
        }
    }

    private fun onConnectivityChanged(isOnline: Boolean) {
        val reconnected = isOnline && _state.value.isOffline
        _state.update { it.copy(isOffline = !isOnline) }
        if (!reconnected) return

        val current = _state.value
        if (current.headlines.error != null) refreshHeadlines()
        if (current.searchResults?.isFromCache == true) searchRetries.tryEmit(Unit)
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val SEARCH_DEBOUNCE_MILLIS = 500L
    }
}
