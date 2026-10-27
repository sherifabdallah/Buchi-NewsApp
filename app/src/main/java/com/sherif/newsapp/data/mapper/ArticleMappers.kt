package com.sherif.newsapp.data.mapper

import com.sherif.newsapp.data.local.ArticleEntity
import com.sherif.newsapp.data.remote.dto.ArticleDto
import com.sherif.newsapp.domain.model.Article
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val REMOVED_PLACEHOLDER = "[Removed]"
private val TRUNCATION_MARKER = Regex("""\s*\[\+\d+ chars]$""")

fun List<ArticleDto>.toEntities(savedAt: Long): List<ArticleEntity> =
    mapNotNull { it.toEntityOrNull(savedAt) }.distinctBy { it.url }

fun ArticleDto.toEntityOrNull(savedAt: Long): ArticleEntity? {
    val url = url?.takeIf { it.isNotBlank() } ?: return null
    val title = title.cleaned() ?: return null
    val sourceName = source?.name.cleaned()
    return ArticleEntity(
        url = url,
        title = title.withoutPublisherSuffix(sourceName),
        description = description.cleaned(),
        content = content.cleaned()?.replace(TRUNCATION_MARKER, ""),
        // Some sources put a profile link in the author field instead of a name.
        author = author.cleaned()?.takeUnless { it.startsWith("http") },
        imageUrl = urlToImage?.takeIf { it.isNotBlank() },
        sourceName = sourceName,
        publishedAt = publishedAt?.toEpochMillisOrNull(),
        savedAt = savedAt,
    )
}

fun ArticleEntity.toDomain() = Article(
    url = url,
    title = title,
    description = description,
    content = content,
    author = author,
    imageUrl = imageUrl,
    sourceName = sourceName,
    publishedAt = publishedAt?.let(Instant::ofEpochMilli),
)

// Trimmed text, or null for blanks and the "[Removed]" placeholder NewsAPI uses for taken-down articles.
private fun String?.cleaned(): String? =
    this?.trim()?.takeUnless { it.isEmpty() || it == REMOVED_PLACEHOLDER }

// Titles usually end with " - Publisher", which repeats the source we already show next to them.
// The suffix doesn't always match the source name exactly ("Bloomberg.com" vs "Bloomberg",
// "Al Jazeera" vs "Al Jazeera English"), so compare loosely and leave the title alone otherwise.
private fun String.withoutPublisherSuffix(sourceName: String?): String {
    val separator = lastIndexOf(" - ")
    if (sourceName == null || separator <= 0) return this

    val suffix = substring(separator + 3).normalized()
    val source = sourceName.normalized()
    val isPublisher = suffix.isNotEmpty() && source.isNotEmpty() &&
        (suffix.startsWith(source) || source.startsWith(suffix))
    return if (isPublisher) substring(0, separator).trimEnd() else this
}

private fun String.normalized() = lowercase().filter(Char::isLetterOrDigit)

private fun String.toEpochMillisOrNull(): Long? =
    try {
        OffsetDateTime.parse(this).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
