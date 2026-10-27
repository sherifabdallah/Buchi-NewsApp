package com.sherif.newsapp.data.remote.dto

import kotlinx.serialization.Serializable

// Everything is optional on purpose: NewsAPI regularly sends nulls for author, urlToImage, content...
@Serializable
data class ArticlesResponse(
    val status: String? = null,
    val totalResults: Int = 0,
    val articles: List<ArticleDto> = emptyList(),
)

@Serializable
data class ArticleDto(
    val source: SourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null,
)

@Serializable
data class SourceDto(
    val id: String? = null,
    val name: String? = null,
)
