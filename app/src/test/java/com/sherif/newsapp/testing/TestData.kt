package com.sherif.newsapp.testing

import com.sherif.newsapp.data.remote.dto.ArticleDto
import com.sherif.newsapp.data.remote.dto.SourceDto
import com.sherif.newsapp.domain.model.Article
import java.time.Instant

fun urlFor(id: String) = "https://example.com/news/$id"

fun article(id: Int) = Article(
    url = urlFor(id.toString()),
    title = "Headline $id",
    description = "Description $id",
    content = null,
    author = "Reporter $id",
    imageUrl = null,
    sourceName = "Example News",
    publishedAt = Instant.parse("2026-10-27T10:00:00Z"),
)

fun articleDto(
    id: String,
    title: String = "Headline $id",
    description: String? = null,
) = ArticleDto(
    source = SourceDto(name = "Example News"),
    author = "Reporter",
    title = title,
    description = description,
    url = urlFor(id),
    publishedAt = "2026-10-27T10:00:00Z",
)
