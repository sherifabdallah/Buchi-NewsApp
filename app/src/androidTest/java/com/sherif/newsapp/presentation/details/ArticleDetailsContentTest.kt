package com.sherif.newsapp.presentation.details

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.presentation.theme.NewsTheme
import org.junit.Rule
import org.junit.Test

class ArticleDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitleAuthorAndDescription() {
        setContent(ArticleDetailsState.Loaded(article(author = "Jane Cooper", description = "What happened today.")))

        composeRule.onNodeWithText("Council approves new bike lanes").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.byline, "Jane Cooper")).assertIsDisplayed()
        composeRule.onNodeWithText("What happened today.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.read_full_article)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fallsBackWhenAuthorAndDescriptionAreMissing() {
        setContent(ArticleDetailsState.Loaded(article(author = null, description = null)))

        composeRule.onNodeWithText(string(R.string.unknown_author)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.no_description)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun usesTheTeaserWhenThereIsNoDescription() {
        setContent(ArticleDetailsState.Loaded(article(description = null, content = "The council voted on Monday…")))

        composeRule.onNodeWithText("The council voted on Monday…").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun explainsWhenTheArticleIsNoLongerSaved() {
        setContent(ArticleDetailsState.NotFound)

        composeRule.onNodeWithText(string(R.string.article_not_found_title)).assertIsDisplayed()
    }

    @Test
    fun backButtonGoesBack() {
        var wentBack = false
        setContent(ArticleDetailsState.Loaded(article()), onBack = { wentBack = true })

        composeRule.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertThat(wentBack).isTrue()
    }

    private fun setContent(state: ArticleDetailsState, onBack: () -> Unit = {}) {
        composeRule.setContent {
            NewsTheme {
                ArticleDetailsContent(state = state, onBack = onBack)
            }
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun article(
        author: String? = "Jane Cooper",
        description: String? = "What happened today.",
        content: String? = null,
    ) = Article(
        url = "https://example.com/news/bike-lanes",
        title = "Council approves new bike lanes",
        description = description,
        content = content,
        author = author,
        imageUrl = null,
        sourceName = "Example News",
        publishedAt = null,
    )
}
