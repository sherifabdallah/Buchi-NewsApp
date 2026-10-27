package com.sherif.newsapp

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.sherif.newsapp.testing.FakeNewsApi
import com.sherif.newsapp.testing.articleDto
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

// Runs the real app (activity, navigation, ViewModels, repository, Room) with a fake API.
@HiltAndroidTest
class NewsAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var api: FakeNewsApi

    @Before
    fun setUp() {
        hiltRule.inject()
        api.headlines = listOf(
            articleDto("1", "Council approves new bike lanes - Example News", description = "The plan adds 40 km of lanes."),
            articleDto("2", "Local team wins the championship"),
        )
    }

    @Test
    fun opensAnArticleFromTheHeadlinesAndComesBack() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // The " - Example News" suffix is dropped because it repeats the source.
            waitFor("Council approves new bike lanes")
            composeRule.onNodeWithText("Council approves new bike lanes").performClick()

            composeRule.onNodeWithContentDescription(string(R.string.share)).assertIsDisplayed()
            composeRule.onNodeWithText(string(R.string.byline, "Jane Cooper")).assertIsDisplayed()

            composeRule.onNodeWithContentDescription(string(R.string.back)).performClick()
            composeRule.onNodeWithText(string(R.string.top_headlines)).assertIsDisplayed()
        }
    }

    @Test
    fun searchingNarrowsTheListDown() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("Council approves new bike lanes")

            composeRule.onNode(hasSetTextAction()).performTextInput("team")

            composeRule.waitUntil(TIMEOUT_MILLIS) {
                composeRule.onAllNodes(hasText("Council approves new bike lanes")).fetchSemanticsNodes().isEmpty()
            }
            composeRule.onNodeWithText("Local team wins the championship").assertIsDisplayed()
        }
    }

    @Test
    fun keepsTheArticlesAndOffersARetryWhenARefreshFails() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("Local team wins the championship")
            api.failure = HttpException(Response.error<Unit>(503, "".toResponseBody()))

            // Pull on the whole list; a swipe across a single row is too short to trigger the refresh.
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }

            waitFor(string(R.string.error_server_title))
            composeRule.onNodeWithText(string(R.string.action_retry)).assertIsDisplayed()
            composeRule.onNodeWithText("Local team wins the championship").assertIsDisplayed()
        }
    }

    private fun waitFor(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
