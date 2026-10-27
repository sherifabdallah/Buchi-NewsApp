package com.sherif.newsapp.testing

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FakeArticlesRepository : ArticlesRepository {

    val cachedHeadlines = MutableStateFlow<List<Article>>(emptyList())

    // Simulates a cold start where reading the database is slower than the network request.
    var cacheReadDelayMillis = 0L

    var refreshResult: DataResult<List<Article>> = DataResult.Success(emptyList())

    // When set, refreshes suspend until it completes, to look at the state while loading.
    var refreshGate: CompletableDeferred<Unit>? = null

    var searchResult: SearchResult = SearchResult.Remote(emptyList())

    var refreshCount = 0
        private set

    val searchQueries = mutableListOf<String>()

    override fun observeTopHeadlines(): Flow<List<Article>> = flow {
        delay(cacheReadDelayMillis)
        emitAll(cachedHeadlines)
    }

    override suspend fun refreshTopHeadlines(): DataResult<List<Article>> {
        refreshCount++
        refreshGate?.await()
        return refreshResult.also { result ->
            if (result is DataResult.Success) cachedHeadlines.value = result.data
        }
    }

    override suspend fun search(query: String): SearchResult {
        searchQueries += query
        return searchResult
    }

    override suspend fun getArticle(url: String): Article? =
        cachedHeadlines.value.find { it.url == url }
}
