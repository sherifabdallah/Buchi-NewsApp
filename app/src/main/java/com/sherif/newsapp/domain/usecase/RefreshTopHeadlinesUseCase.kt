package com.sherif.newsapp.domain.usecase

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataResult
import com.sherif.newsapp.domain.repository.ArticlesRepository
import javax.inject.Inject

class RefreshTopHeadlinesUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    suspend operator fun invoke(): DataResult<List<Article>> = repository.refreshTopHeadlines()
}
