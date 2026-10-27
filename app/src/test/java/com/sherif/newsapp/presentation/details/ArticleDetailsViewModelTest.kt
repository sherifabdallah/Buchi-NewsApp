package com.sherif.newsapp.presentation.details

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.usecase.GetArticleUseCase
import com.sherif.newsapp.testing.FakeArticlesRepository
import com.sherif.newsapp.testing.MainDispatcherRule
import com.sherif.newsapp.testing.article
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ArticleDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeArticlesRepository()

    @Test
    fun `loads the article for the given url`() = runTest {
        repository.cachedHeadlines.value = listOf(article(1), article(2))

        val viewModel = ArticleDetailsViewModel(article(2).url, GetArticleUseCase(repository))

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(ArticleDetailsState.Loading)
            assertThat(awaitItem()).isEqualTo(ArticleDetailsState.Loaded(article(2)))
        }
    }

    @Test
    fun `reports an article that is no longer saved`() = runTest {
        val viewModel = ArticleDetailsViewModel("https://example.com/gone", GetArticleUseCase(repository))

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(ArticleDetailsState.Loading)
            assertThat(awaitItem()).isEqualTo(ArticleDetailsState.NotFound)
        }
    }
}
