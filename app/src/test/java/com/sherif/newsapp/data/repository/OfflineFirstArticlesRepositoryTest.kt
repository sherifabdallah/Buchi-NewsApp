package com.sherif.newsapp.data.repository

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.testing.FakeArticleDao
import com.sherif.newsapp.testing.FakeNewsApi
import com.sherif.newsapp.testing.articleDto
import com.sherif.newsapp.testing.urlFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OfflineFirstArticlesRepositoryTest {

    private val api = FakeNewsApi()
    private val dao = FakeArticleDao()
    private val clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC)
    private val repository = OfflineFirstArticlesRepository(api, dao, clock)

    @Test
    fun `refresh merges both pages in order without duplicates`() = runTest {
        api.pages[1] = listOf(articleDto("a"), articleDto("b"))
        api.pages[2] = listOf(articleDto("c"), articleDto("a"))

        val result = repository.refreshTopHeadlines()

        assertThat(api.requestedPages).containsExactly(1, 2)
        assertThat((result as DataResult.Success).data.map { it.url })
            .containsExactly(urlFor("a"), urlFor("b"), urlFor("c")).inOrder()
        assertThat(repository.observeTopHeadlines().first().map { it.url })
            .containsExactly(urlFor("a"), urlFor("b"), urlFor("c")).inOrder()
    }

    @Test
    fun `a failed refresh leaves the cached headlines alone`() = runTest {
        api.pages[1] = listOf(articleDto("a"))
        repository.refreshTopHeadlines()

        api.failure = IOException("no network")
        val result = repository.refreshTopHeadlines()

        assertThat(result).isEqualTo(DataResult.Failure(DataError.NoConnection))
        assertThat(repository.observeTopHeadlines().first().map { it.url }).containsExactly(urlFor("a"))
    }

    @Test
    fun `search results are saved so the details screen can open them`() = runTest {
        api.searchResults = listOf(articleDto("x"))

        val result = repository.search("x")

        assertThat(result).isInstanceOf(SearchResult.Remote::class.java)
        assertThat(repository.getArticle(urlFor("x"))?.title).isEqualTo("Headline x")
        // ...but they don't leak into the headlines feed.
        assertThat(repository.observeTopHeadlines().first()).isEmpty()
    }

    @Test
    fun `search falls back to matching saved articles when the request fails`() = runTest {
        api.pages[1] = listOf(
            articleDto("a", title = "Apple unveils a touchscreen MacBook"),
            articleDto("b", title = "Heavy rain expected this weekend", description = "Forecasters say apple orchards could flood"),
            articleDto("c", title = "Seahawks win again"),
        )
        repository.refreshTopHeadlines()

        api.failure = IOException("no network")
        val result = repository.search("apple")

        assertThat(result).isInstanceOf(SearchResult.Cached::class.java)
        assertThat((result as SearchResult.Cached).error).isEqualTo(DataError.NoConnection)
        assertThat(result.articles.map { it.url }).containsExactly(urlFor("a"), urlFor("b"))
    }
}
