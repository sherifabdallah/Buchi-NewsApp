package com.sherif.newsapp.presentation.articles

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.domain.usecase.ObserveTopHeadlinesUseCase
import com.sherif.newsapp.domain.usecase.RefreshTopHeadlinesUseCase
import com.sherif.newsapp.domain.usecase.SearchArticlesUseCase
import com.sherif.newsapp.testing.FakeArticlesRepository
import com.sherif.newsapp.testing.FakeNetworkMonitor
import com.sherif.newsapp.testing.MainDispatcherRule
import com.sherif.newsapp.testing.article
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeArticlesRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = ArticlesViewModel(
        savedStateHandle = savedStateHandle,
        observeTopHeadlines = ObserveTopHeadlinesUseCase(repository),
        refreshTopHeadlines = RefreshTopHeadlinesUseCase(repository),
        searchArticles = SearchArticlesUseCase(repository),
        networkMonitor = networkMonitor,
    )

    @Test
    fun `shows cached headlines while the refresh is still running`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1), article(2))
        repository.refreshGate = CompletableDeferred()

        val viewModel = createViewModel()
        runCurrent()

        val headlines = viewModel.state.value.headlines
        assertThat(headlines.articles).containsExactly(article(1), article(2)).inOrder()
        assertThat(headlines.isLoading).isTrue()
    }

    @Test
    fun `replaces the cache with fresh headlines once the refresh succeeds`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1))
        repository.refreshResult = DataResult.Success(listOf(article(2), article(3)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val headlines = viewModel.state.value.headlines
        assertThat(headlines.articles).containsExactly(article(2), article(3)).inOrder()
        assertThat(headlines.isLoading).isFalse()
        assertThat(headlines.error).isNull()
    }

    @Test
    fun `shows an error when the refresh fails and nothing is cached`() = runTest {
        repository.refreshResult = DataResult.Failure(DataError.NoConnection)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val headlines = viewModel.state.value.headlines
        assertThat(headlines.articles).isEmpty()
        assertThat(headlines.isLoading).isFalse()
        assertThat(headlines.error).isEqualTo(DataError.NoConnection)
    }

    @Test
    fun `keeps cached headlines and reports the failure in a snackbar`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1))
        repository.refreshResult = DataResult.Failure(DataError.Server)

        val viewModel = createViewModel()

        viewModel.effects.test {
            assertThat(awaitItem()).isEqualTo(ArticlesEffect.ShowRefreshError(DataError.Server))
        }
        assertThat(viewModel.state.value.headlines.articles).containsExactly(article(1))
    }

    @Test
    fun `still reports a failed refresh when the request fails before the cache is read`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1))
        repository.cacheReadDelayMillis = 1_000
        repository.refreshResult = DataResult.Failure(DataError.Unauthorized)

        val viewModel = createViewModel()

        viewModel.effects.test {
            assertThat(awaitItem()).isEqualTo(ArticlesEffect.ShowRefreshError(DataError.Unauthorized))
        }
        // And the cached articles are what's on screen, not the full screen error.
        assertThat(viewModel.state.value.headlines.articles).containsExactly(article(1))
    }

    @Test
    fun `does not show a snackbar while offline because the banner already explains it`() = runTest {
        networkMonitor.isOnline.value = false
        repository.cachedHeadlines.value = listOf(article(1))
        repository.refreshResult = DataResult.Failure(DataError.NoConnection)

        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()
            expectNoEvents()
        }
        assertThat(viewModel.state.value.isOffline).isTrue()
    }

    @Test
    fun `only searches once the user stops typing`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(ArticlesIntent.QueryChanged("a"))
        advanceTimeBy(200)
        viewModel.onIntent(ArticlesIntent.QueryChanged("ap"))
        advanceTimeBy(200)
        viewModel.onIntent(ArticlesIntent.QueryChanged("apple "))
        advanceUntilIdle()

        assertThat(repository.searchQueries).containsExactly("apple")
    }

    @Test
    fun `shows search results instead of the headlines`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1))
        repository.refreshResult = DataResult.Success(listOf(article(1)))
        repository.searchResult = SearchResult.Remote(listOf(article(7)))
        val viewModel = createViewModel()

        viewModel.onIntent(ArticlesIntent.QueryChanged("apple"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.visibleList.articles).containsExactly(article(7))
        assertThat(state.visibleList.isFromCache).isFalse()
    }

    @Test
    fun `falls back to saved articles when the search request fails`() = runTest {
        repository.searchResult = SearchResult.Cached(listOf(article(3)), DataError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(ArticlesIntent.QueryChanged("apple"))
        advanceUntilIdle()

        val results = viewModel.state.value.searchResults
        assertThat(results?.articles).containsExactly(article(3))
        assertThat(results?.isFromCache).isTrue()
        assertThat(results?.error).isEqualTo(DataError.NoConnection)
    }

    @Test
    fun `clearing the query goes straight back to the headlines`() = runTest {
        repository.searchResult = SearchResult.Remote(listOf(article(3)))
        val viewModel = createViewModel()
        viewModel.onIntent(ArticlesIntent.QueryChanged("apple"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.searchResults).isNotNull()

        viewModel.onIntent(ArticlesIntent.ClearQuery)

        // No advancing time here: the headlines have to come back without waiting for the debounce.
        assertThat(viewModel.state.value.query).isEmpty()
        assertThat(viewModel.state.value.searchResults).isNull()
    }

    @Test
    fun `pull to refresh during a search runs the search again`() = runTest {
        repository.searchResult = SearchResult.Remote(listOf(article(3)))
        val viewModel = createViewModel()
        viewModel.onIntent(ArticlesIntent.QueryChanged("apple"))
        advanceUntilIdle()

        viewModel.onIntent(ArticlesIntent.Refresh)
        assertThat(viewModel.state.value.isRefreshing).isTrue()
        advanceUntilIdle()

        assertThat(repository.searchQueries).containsExactly("apple", "apple")
        assertThat(repository.refreshCount).isEqualTo(1)
        assertThat(viewModel.state.value.isRefreshing).isFalse()
    }

    @Test
    fun `pull to refresh reloads the headlines and shows the indicator meanwhile`() = runTest {
        repository.refreshResult = DataResult.Success(listOf(article(1)))
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.refreshResult = DataResult.Success(listOf(article(1), article(2)))
        viewModel.onIntent(ArticlesIntent.Refresh)
        assertThat(viewModel.state.value.isRefreshing).isTrue()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(repository.refreshCount).isEqualTo(2)
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.headlines.articles).containsExactly(article(1), article(2)).inOrder()
    }

    @Test
    fun `retrying from the error screen shows the skeleton instead of the pull indicator`() = runTest {
        repository.refreshResult = DataResult.Failure(DataError.NoConnection)
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.refreshGate = CompletableDeferred()
        viewModel.onIntent(ArticlesIntent.Refresh)
        runCurrent()

        val state = viewModel.state.value
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.headlines.isLoading).isTrue()
        assertThat(state.headlines.error).isNull()
    }

    @Test
    fun `ignores a refresh while another one is still running`() = runTest {
        repository.refreshGate = CompletableDeferred()
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onIntent(ArticlesIntent.Refresh)
        viewModel.onIntent(ArticlesIntent.Refresh)
        runCurrent()

        assertThat(repository.refreshCount).isEqualTo(1)
    }

    @Test
    fun `runs an offline search again once the connection is back`() = runTest {
        networkMonitor.isOnline.value = false
        repository.searchResult = SearchResult.Cached(listOf(article(3)), DataError.NoConnection)
        val viewModel = createViewModel()
        viewModel.onIntent(ArticlesIntent.QueryChanged("apple"))
        advanceUntilIdle()

        repository.searchResult = SearchResult.Remote(listOf(article(3), article(4)))
        networkMonitor.isOnline.value = true
        advanceUntilIdle()

        val results = viewModel.state.value.searchResults
        assertThat(repository.searchQueries).containsExactly("apple", "apple")
        assertThat(results?.isFromCache).isFalse()
        assertThat(results?.articles).containsExactly(article(3), article(4)).inOrder()
    }

    @Test
    fun `refreshes again when the connection comes back after a failure`() = runTest {
        networkMonitor.isOnline.value = false
        repository.refreshResult = DataResult.Failure(DataError.NoConnection)
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.refreshResult = DataResult.Success(listOf(article(1)))
        networkMonitor.isOnline.value = true
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(repository.refreshCount).isEqualTo(2)
        assertThat(state.isOffline).isFalse()
        assertThat(state.headlines.articles).containsExactly(article(1))
        assertThat(state.headlines.error).isNull()
    }

    @Test
    fun `restores the search after process death`() = runTest {
        repository.searchResult = SearchResult.Remote(listOf(article(3)))

        val viewModel = createViewModel(SavedStateHandle(mapOf("query" to "apple")))
        advanceUntilIdle()

        assertThat(viewModel.state.value.query).isEqualTo("apple")
        assertThat(viewModel.state.value.searchResults?.articles).containsExactly(article(3))
    }
}
