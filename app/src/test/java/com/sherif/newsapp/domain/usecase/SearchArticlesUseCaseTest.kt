package com.sherif.newsapp.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.testing.FakeArticlesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchArticlesUseCaseTest {

    private val repository = FakeArticlesRepository()
    private val searchArticles = SearchArticlesUseCase(repository)

    @Test
    fun `trims the keyword before searching`() = runTest {
        searchArticles("  apple ")

        assertThat(repository.searchQueries).containsExactly("apple")
    }

    @Test
    fun `a blank keyword never reaches the repository`() = runTest {
        val result = searchArticles("   ")

        assertThat(result).isEqualTo(SearchResult.Remote(emptyList()))
        assertThat(repository.searchQueries).isEmpty()
    }
}
