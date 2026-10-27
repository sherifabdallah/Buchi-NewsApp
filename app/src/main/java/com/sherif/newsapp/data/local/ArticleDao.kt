package com.sherif.newsapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query(
        """
        SELECT articles.* FROM articles
        INNER JOIN top_headlines ON top_headlines.article_url = articles.url
        ORDER BY top_headlines.position
        """,
    )
    fun observeTopHeadlines(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE url = :url")
    suspend fun getArticle(url: String): ArticleEntity?

    // The query has to be escaped with escapeForLike() first.
    @Query(
        """
        SELECT * FROM articles
        WHERE title LIKE '%' || :query || '%' ESCAPE '\'
           OR description LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY published_at DESC
        """,
    )
    suspend fun search(query: String): List<ArticleEntity>

    // Upsert rather than REPLACE: a replace deletes the row first, which would cascade to top_headlines.
    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Insert
    suspend fun insertTopHeadlines(headlines: List<TopHeadlineEntity>)

    @Query("DELETE FROM top_headlines")
    suspend fun clearTopHeadlines()

    @Query("DELETE FROM articles WHERE saved_at < :savedBefore AND url NOT IN (SELECT article_url FROM top_headlines)")
    suspend fun deleteUnusedArticles(savedBefore: Long)

    @Transaction
    suspend fun replaceTopHeadlines(articles: List<ArticleEntity>, deleteUnusedBefore: Long) {
        clearTopHeadlines()
        upsertArticles(articles)
        insertTopHeadlines(articles.mapIndexed { position, article -> TopHeadlineEntity(position, article.url) })
        deleteUnusedArticles(deleteUnusedBefore)
    }
}

fun String.escapeForLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
