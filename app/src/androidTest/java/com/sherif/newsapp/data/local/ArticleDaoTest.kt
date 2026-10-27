package com.sherif.newsapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {

    private lateinit var database: NewsDatabase
    private lateinit var dao: ArticleDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NewsDatabase::class.java,
        ).build()
        dao = database.articleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replacingHeadlinesKeepsTheOrderTheyCameIn() = runTest {
        dao.replaceTopHeadlines(listOf(entity("b"), entity("a"), entity("c")), deleteUnusedBefore = 0)

        assertThat(dao.observeTopHeadlines().first().map { it.url }).containsExactly("b", "a", "c").inOrder()
    }

    @Test
    fun replacingHeadlinesDropsTheOldFeed() = runTest {
        dao.replaceTopHeadlines(listOf(entity("a"), entity("b")), deleteUnusedBefore = 0)

        dao.replaceTopHeadlines(listOf(entity("c")), deleteUnusedBefore = 0)

        assertThat(dao.observeTopHeadlines().first().map { it.url }).containsExactly("c")
    }

    @Test
    fun oldArticlesOutsideTheFeedAreDeletedButRecentOnesStay() = runTest {
        dao.upsertArticles(listOf(entity("old-search", savedAt = 10), entity("recent-search", savedAt = 100)))

        dao.replaceTopHeadlines(listOf(entity("headline", savedAt = 100)), deleteUnusedBefore = 50)

        assertThat(dao.getArticle("old-search")).isNull()
        assertThat(dao.getArticle("recent-search")).isNotNull()
        assertThat(dao.getArticle("headline")).isNotNull()
    }

    @Test
    fun updatingAnArticleKeepsItInTheFeed() = runTest {
        dao.replaceTopHeadlines(listOf(entity("a", title = "Old title")), deleteUnusedBefore = 0)

        dao.upsertArticles(listOf(entity("a", title = "New title")))

        assertThat(dao.observeTopHeadlines().first().map { it.title }).containsExactly("New title")
    }

    @Test
    fun searchMatchesTitleOrDescriptionIgnoringCase() = runTest {
        dao.upsertArticles(
            listOf(
                entity("a", title = "Apple unveils a touchscreen MacBook"),
                entity("b", title = "Heavy rain this weekend", description = "Apple orchards could flood"),
                entity("c", title = "Seahawks win again"),
            ),
        )

        assertThat(dao.search("apple".escapeForLike()).map { it.url }).containsExactly("a", "b")
    }

    @Test
    fun searchTreatsWildcardCharactersLiterally() = runTest {
        dao.upsertArticles(listOf(entity("a", title = "Prices are up 100% this year"), entity("b", title = "Rain expected")))

        assertThat(dao.search("%".escapeForLike()).map { it.url }).containsExactly("a")
        assertThat(dao.search("_".escapeForLike())).isEmpty()
    }

    private fun entity(
        url: String,
        title: String = "Title $url",
        description: String? = null,
        savedAt: Long = 0,
    ) = ArticleEntity(
        url = url,
        title = title,
        description = description,
        content = null,
        author = null,
        imageUrl = null,
        sourceName = null,
        publishedAt = null,
        savedAt = savedAt,
    )
}
