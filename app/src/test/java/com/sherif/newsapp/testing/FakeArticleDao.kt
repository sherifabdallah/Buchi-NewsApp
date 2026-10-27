package com.sherif.newsapp.testing

import com.sherif.newsapp.data.local.ArticleDao
import com.sherif.newsapp.data.local.ArticleEntity
import com.sherif.newsapp.data.local.TopHeadlineEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

// In-memory stand-in for the Room DAO. replaceTopHeadlines() isn't overridden, so the real
// default implementation runs on top of these primitives. The SQL itself is covered by ArticleDaoTest.
class FakeArticleDao : ArticleDao {

    private val articles = MutableStateFlow<Map<String, ArticleEntity>>(emptyMap())
    private val headlines = MutableStateFlow<List<TopHeadlineEntity>>(emptyList())

    override fun observeTopHeadlines(): Flow<List<ArticleEntity>> =
        combine(articles, headlines) { articles, headlines ->
            headlines.sortedBy { it.position }.mapNotNull { articles[it.articleUrl] }
        }

    override suspend fun getArticle(url: String): ArticleEntity? = articles.value[url]

    override suspend fun search(query: String): List<ArticleEntity> {
        val term = query.replace("\\%", "%").replace("\\_", "_").replace("\\\\", "\\")
        return articles.value.values
            .filter { it.title.contains(term, ignoreCase = true) || it.description?.contains(term, ignoreCase = true) == true }
            .sortedByDescending { it.publishedAt }
    }

    override suspend fun upsertArticles(articles: List<ArticleEntity>) {
        this.articles.value += articles.associateBy { it.url }
    }

    override suspend fun insertTopHeadlines(headlines: List<TopHeadlineEntity>) {
        this.headlines.value += headlines
    }

    override suspend fun clearTopHeadlines() {
        headlines.value = emptyList()
    }

    override suspend fun deleteUnusedArticles(savedBefore: Long) {
        val inFeed = headlines.value.map { it.articleUrl }.toSet()
        articles.value = articles.value.filterValues { it.savedAt >= savedBefore || it.url in inFeed }
    }
}
