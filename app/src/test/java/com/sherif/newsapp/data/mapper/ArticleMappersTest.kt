package com.sherif.newsapp.data.mapper

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.data.remote.dto.ArticleDto
import com.sherif.newsapp.data.remote.dto.SourceDto
import org.junit.Test
import java.time.Instant

class ArticleMappersTest {

    @Test
    fun `drops the publisher suffix when it matches the source`() {
        val titles = listOf(
            dto(title = "Stocks climb as oil dips - Bloomberg.com", source = "Bloomberg"),
            dto(title = "Jordan arrests three suspects - Al Jazeera", source = "Al Jazeera English"),
            dto(title = "Israel braces for a pivotal election - BBC", source = "BBC News"),
        ).map { it.toEntityOrNull(savedAt = 0)?.title }

        assertThat(titles).containsExactly(
            "Stocks climb as oil dips",
            "Jordan arrests three suspects",
            "Israel braces for a pivotal election",
        ).inOrder()
    }

    @Test
    fun `keeps the suffix when it isn't the source name`() {
        val entity = dto(title = "Thunder lose Williams to injury - NBA.com", source = "Heat.com").toEntityOrNull(0)

        assertThat(entity?.title).isEqualTo("Thunder lose Williams to injury - NBA.com")
    }

    @Test
    fun `strips the truncation marker from the content`() {
        val entity = dto(content = "Security forces arrested three suspects… [+2306 chars]").toEntityOrNull(0)

        assertThat(entity?.content).isEqualTo("Security forces arrested three suspects…")
    }

    @Test
    fun `skips removed articles and articles without a url or title`() {
        assertThat(dto(title = "[Removed]").toEntityOrNull(0)).isNull()
        assertThat(dto(title = "   ").toEntityOrNull(0)).isNull()
        assertThat(dto(url = null).toEntityOrNull(0)).isNull()
    }

    @Test
    fun `treats a link in the author field as an unknown author`() {
        assertThat(dto(author = "https://www.facebook.com/InStyle").toEntityOrNull(0)?.author).isNull()
        assertThat(dto(author = " Jenny Lee ").toEntityOrNull(0)?.author).isEqualTo("Jenny Lee")
    }

    @Test
    fun `parses the publish date and ignores malformed ones`() {
        assertThat(dto(publishedAt = "2026-10-26T07:05:58Z").toEntityOrNull(0)?.publishedAt)
            .isEqualTo(Instant.parse("2026-10-26T07:05:58Z").toEpochMilli())
        assertThat(dto(publishedAt = "yesterday").toEntityOrNull(0)?.publishedAt).isNull()
    }

    @Test
    fun `keeps the first copy of an article that appears twice`() {
        val entities = listOf(
            dto(url = "https://example.com/a", title = "First"),
            dto(url = "https://example.com/b", title = "Other"),
            dto(url = "https://example.com/a", title = "Second"),
        ).toEntities(savedAt = 0)

        assertThat(entities.map { it.title }).containsExactly("First", "Other").inOrder()
    }

    private fun dto(
        url: String? = "https://example.com/a",
        title: String? = "Title",
        source: String? = "Example News",
        author: String? = null,
        content: String? = null,
        publishedAt: String? = null,
    ) = ArticleDto(
        source = SourceDto(name = source),
        author = author,
        title = title,
        url = url,
        content = content,
        publishedAt = publishedAt,
    )
}
