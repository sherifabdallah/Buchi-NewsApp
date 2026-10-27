package com.sherif.newsapp.domain.usecase

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.repository.ArticlesRepository
import javax.inject.Inject

class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository,
) {
    suspend operator fun invoke(url: String): Article? = repository.getArticle(url)
}
