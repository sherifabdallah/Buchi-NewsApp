package com.sherif.newsapp.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertWithMessage
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.presentation.articles.ArticleListState
import com.sherif.newsapp.presentation.articles.ArticlesContent
import com.sherif.newsapp.presentation.articles.ArticlesState
import com.sherif.newsapp.presentation.common.previewArticles
import com.sherif.newsapp.presentation.details.ArticleDetailsContent
import com.sherif.newsapp.presentation.details.ArticleDetailsState
import com.sherif.newsapp.presentation.theme.NewsTheme
import org.junit.Rule
import org.junit.Test

// Every tappable element has to give TalkBack something to announce. Touch target size isn't checked
// here because Compose enforces the 48dp minimum itself. Google's Accessibility Test Framework would
// check more, but it needs View-based Material Components, which clash with the app's dependencies.
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headlines() = checkScreen {
        ArticlesContent(
            state = ArticlesState(headlines = ArticleListState(articles = previewArticles), isOffline = true),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onArticleClick = {},
        )
    }

    @Test
    fun searchResults() = checkScreen {
        ArticlesContent(
            state = ArticlesState(
                query = "stocks",
                searchResults = ArticleListState(articles = previewArticles, error = DataError.Server, isFromCache = true),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onArticleClick = {},
        )
    }

    @Test
    fun errorState() = checkScreen {
        ArticlesContent(
            state = ArticlesState(headlines = ArticleListState(error = DataError.RateLimited)),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onArticleClick = {},
        )
    }

    @Test
    fun articleDetails() = checkScreen {
        ArticleDetailsContent(state = ArticleDetailsState.Loaded(previewArticles[0]), onBack = {})
    }

    private fun checkScreen(content: @Composable () -> Unit) {
        composeRule.setContent { NewsTheme { content() } }

        val clickable = composeRule.onAllNodes(hasClickAction())
        val count = clickable.fetchSemanticsNodes().size
        assertWithMessage("no clickable elements found").that(count).isGreaterThan(0)

        repeat(count) { index ->
            val node = clickable[index]
            val config = node.fetchSemanticsNode().config
            val label = listOfNotNull(
                config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
                config.getOrNull(SemanticsProperties.Text)?.joinToString(),
                config.getOrNull(SemanticsProperties.EditableText)?.text,
            ).joinToString().trim()
            assertWithMessage("clickable element without a label: $config").that(label).isNotEmpty()
        }
    }
}
