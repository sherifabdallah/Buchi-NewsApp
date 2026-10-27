package com.sherif.newsapp.testing

import com.sherif.newsapp.data.remote.NewsApi
import com.sherif.newsapp.data.remote.dto.ArticleDto
import com.sherif.newsapp.data.remote.dto.ArticlesResponse
import com.sherif.newsapp.data.remote.dto.SourceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeNewsApi @Inject constructor() : NewsApi {

    var headlines: List<ArticleDto> = emptyList()
    var failure: Exception? = null

    override suspend fun getTopHeadlines(country: String, page: Int): ArticlesResponse {
        failure?.let { throw it }
        return ArticlesResponse(status = "ok", articles = if (page == 1) headlines else emptyList())
    }

    override suspend fun searchTopHeadlines(country: String, query: String): ArticlesResponse {
        failure?.let { throw it }
        return ArticlesResponse(status = "ok", articles = headlines.filter { it.title.orEmpty().contains(query, ignoreCase = true) })
    }
}

fun articleDto(id: String, title: String, description: String? = null) = ArticleDto(
    source = SourceDto(name = "Example News"),
    author = "Jane Cooper",
    title = title,
    description = description,
    url = "https://example.com/news/$id",
    publishedAt = "2026-10-27T08:00:00Z",
)
