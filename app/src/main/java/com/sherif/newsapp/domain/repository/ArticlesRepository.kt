package com.sherif.newsapp.domain.repository

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface ArticlesRepository {

    fun observeTopHeadlines(): Flow<List<Article>>

    suspend fun refreshTopHeadlines(): DataResult<List<Article>>

    suspend fun search(query: String): SearchResult

    suspend fun getArticle(url: String): Article?
}
