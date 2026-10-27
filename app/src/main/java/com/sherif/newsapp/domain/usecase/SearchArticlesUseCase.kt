package com.sherif.newsapp.domain.usecase

import com.sherif.newsapp.domain.model.SearchResult
import com.sherif.newsapp.domain.repository.ArticlesRepository
import javax.inject.Inject

class SearchArticlesUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    suspend operator fun invoke(query: String): SearchResult {
        val keyword = query.trim()
        if (keyword.isEmpty()) return SearchResult.Remote(emptyList())
        return repository.search(keyword)
    }
}
