package com.sherif.newsapp.presentation.details

import com.sherif.newsapp.domain.model.Article

sealed interface ArticleDetailsState {
    data object Loading : ArticleDetailsState
    data class Loaded(val article: Article) : ArticleDetailsState
    data object NotFound : ArticleDetailsState
}
