package com.sherif.newsapp.domain.usecase

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.repository.ArticlesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTopHeadlinesUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    operator fun invoke(): Flow<List<Article>> = repository.observeTopHeadlines()
}
