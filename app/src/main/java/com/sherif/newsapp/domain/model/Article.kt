package com.sherif.newsapp.domain.model

import java.time.Instant

data class Article(
    // NewsAPI doesn't give articles an id, the url is the only stable identifier.
    val url: String,
    val title: String,
    val description: String?,
    val content: String?,
    val author: String?,
    val imageUrl: String?,
    val sourceName: String?,
    val publishedAt: Instant?,
)
