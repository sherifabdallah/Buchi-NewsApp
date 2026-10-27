package com.sherif.newsapp.data.repository

import com.sherif.newsapp.data.local.ArticleDao
import com.sherif.newsapp.data.local.ArticleEntity
import com.sherif.newsapp.data.local.escapeForLike
import com.sherif.newsapp.data.mapper.toDomain
import com.sherif.newsapp.data.mapper.toEntities
import com.sherif.newsapp.data.remote.NewsApi
import com.sherif.newsapp.data.safeCall
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class OfflineFirstArticlesRepository @Inject constructor(
    private val api: NewsApi,
    private val dao: ArticleDao,
    private val clock: Clock,
) : ArticlesRepository {

    override fun observeTopHeadlines(): Flow<List<Article>> =
        dao.observeTopHeadlines().map { articles -> articles.map(ArticleEntity::toDomain) }

    // Both pages are requested in parallel and merged in page order. If either one fails the
    // cache is left untouched, so the feed never ends up half old and half new.
    override suspend fun refreshTopHeadlines(): DataResult<List<Article>> = safeCall {
        val now = clock.millis()
        val articles = coroutineScope {
            HEADLINE_PAGES
                .map { page -> async { api.getTopHeadlines(COUNTRY, page).articles } }
                .awaitAll()
                .flatten()
                .toEntities(savedAt = now)
        }
        dao.replaceTopHeadlines(articles, deleteUnusedBefore = now - SEARCH_CACHE_RETENTION.inWholeMilliseconds)
        articles.map(ArticleEntity::toDomain)
    }

    override suspend fun search(query: String): SearchResult {
        val remote = safeCall {
            api.searchTopHeadlines(COUNTRY, query).articles
                .toEntities(savedAt = clock.millis())
                .also { dao.upsertArticles(it) }
        }
        return when (remote) {
            is DataResult.Success -> SearchResult.Remote(remote.data.map(ArticleEntity::toDomain))
            is DataResult.Failure -> SearchResult.Cached(
                articles = dao.search(query.escapeForLike()).map(ArticleEntity::toDomain),
                error = remote.error,
            )
        }
    }

    override suspend fun getArticle(url: String): Article? = dao.getArticle(url)?.toDomain()

    private companion object {
        const val COUNTRY = "us"
        val HEADLINE_PAGES = 1..2

        // Search results are saved so the details screen can load them by url and so they show up in
        // offline searches. Once they're no longer in the headlines they're kept for a few days.
        val SEARCH_CACHE_RETENTION = 3.days
    }
}
