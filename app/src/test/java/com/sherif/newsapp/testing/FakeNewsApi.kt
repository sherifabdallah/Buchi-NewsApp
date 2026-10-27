package com.sherif.newsapp.testing

import com.sherif.newsapp.data.remote.NewsApi
import com.sherif.newsapp.data.remote.dto.ArticleDto
import com.sherif.newsapp.data.remote.dto.ArticlesResponse

class FakeNewsApi : NewsApi {

    val pages = mutableMapOf<Int, List<ArticleDto>>()
    var searchResults: List<ArticleDto> = emptyList()
    var failure: Exception? = null

    val requestedPages = mutableListOf<Int>()

    override suspend fun getTopHeadlines(country: String, page: Int): ArticlesResponse {
        requestedPages += page
        failure?.let { throw it }
        return response(pages[page].orEmpty())
    }

    override suspend fun searchTopHeadlines(country: String, query: String): ArticlesResponse {
        failure?.let { throw it }
        return response(searchResults)
    }

    private fun response(articles: List<ArticleDto>) =
        ArticlesResponse(status = "ok", totalResults = articles.size, articles = articles)
}
