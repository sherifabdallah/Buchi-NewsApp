package com.sherif.newsapp.presentation.articles

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.presentation.theme.NewsTheme
import org.junit.Rule
import org.junit.Test

class ArticlesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ArticlesIntent>()
    private val clicked = mutableListOf<Article>()

    @Test
    fun showsTheArticlesAndOpensTheOneThatIsTapped() {
        setContent(ArticlesState(headlines = ArticleListState(articles = listOf(article(1), article(2)))))

        composeRule.onNodeWithText("Headline 1").assertIsDisplayed()
        composeRule.onNodeWithText("Headline 2").performClick()

        assertThat(clicked).containsExactly(article(2))
    }

    @Test
    fun errorStateOffersARetry() {
        setContent(ArticlesState(headlines = ArticleListState(error = DataError.NoConnection)))

        composeRule.onNodeWithText(string(R.string.error_no_connection_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()

        assertThat(intents).containsExactly(ArticlesIntent.Refresh)
    }

    @Test
    fun typingInTheSearchFieldSendsTheQuery() {
        setContent(ArticlesState(headlines = ArticleListState(articles = listOf(article(1)))))

        composeRule.onNode(hasSetTextAction()).performTextInput("apple")

        assertThat(intents).containsExactly(ArticlesIntent.QueryChanged("apple"))
    }

    @Test
    fun showsASkeletonWhileTheFirstLoadIsRunning() {
        setContent(ArticlesState(headlines = ArticleListState(isLoading = true)))

        composeRule.onNodeWithContentDescription(string(R.string.loading_articles)).assertIsDisplayed()
    }

    @Test
    fun showsTheOfflineBannerAboveSavedArticles() {
        setContent(ArticlesState(headlines = ArticleListState(articles = listOf(article(1))), isOffline = true))

        composeRule.onNodeWithText(string(R.string.offline_banner)).assertIsDisplayed()
        composeRule.onNodeWithText("Headline 1").assertIsDisplayed()
    }

    @Test
    fun saysWhenSearchResultsComeFromSavedArticles() {
        setContent(
            ArticlesState(
                query = "apple",
                searchResults = ArticleListState(
                    articles = listOf(article(1)),
                    error = DataError.Server,
                    isFromCache = true,
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.search_cached_notice)).assertIsDisplayed()
    }

    @Test
    fun explainsWhenASearchFindsNothing() {
        setContent(ArticlesState(query = "zzz", searchResults = ArticleListState()))

        composeRule.onNodeWithText(string(R.string.no_results_title, "zzz")).assertIsDisplayed()
    }

    private fun setContent(state: ArticlesState) {
        composeRule.setContent {
            NewsTheme {
                ArticlesContent(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onIntent = { intents += it },
                    onArticleClick = { clicked += it },
                )
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun article(id: Int) = Article(
        url = "https://example.com/news/$id",
        title = "Headline $id",
        description = null,
        content = null,
        author = null,
        imageUrl = null,
        sourceName = "Example News",
        publishedAt = null,
    )
}
