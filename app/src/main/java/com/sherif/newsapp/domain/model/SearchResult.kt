package com.sherif.newsapp.domain.model

sealed interface SearchResult {
    val articles: List<Article>

    data class Remote(override val articles: List<Article>) : SearchResult

    // The request failed, so these are matches from the articles already saved on the device.
    data class Cached(override val articles: List<Article>, val error: DataError) : SearchResult
}
