package com.sherif.newsapp.presentation.common

import com.sherif.newsapp.domain.model.Article
import java.time.Instant
import java.time.temporal.ChronoUnit

val previewArticles = listOf(
    Article(
        url = "https://example.com/markets",
        title = "Stocks edge higher as investors wait on a busy week of tech earnings",
        description = "Futures were steady after a volatile week, with oil pulling back from a six-month high.",
        content = null,
        author = "Jane Cooper",
        imageUrl = null,
        sourceName = "Bloomberg",
        publishedAt = Instant.now().minus(2, ChronoUnit.HOURS),
    ),
    Article(
        url = "https://example.com/science",
        title = "New simulation brings the early universe to life on a record-breaking scale",
        description = null,
        content = null,
        author = null,
        imageUrl = null,
        sourceName = "Phys.org",
        publishedAt = Instant.now().minus(5, ChronoUnit.HOURS),
    ),
    Article(
        url = "https://example.com/sports",
        title = "Seahawks hand the Chiefs their first loss of the season",
        description = "A stifling defense kept Kansas City out of the end zone for three quarters.",
        content = null,
        author = "Nate Taylor",
        imageUrl = null,
        sourceName = "ESPN",
        publishedAt = Instant.now().minus(1, ChronoUnit.DAYS),
    ),
)
