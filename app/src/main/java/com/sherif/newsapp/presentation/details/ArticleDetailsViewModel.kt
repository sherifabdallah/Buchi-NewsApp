package com.sherif.newsapp.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.newsapp.domain.usecase.GetArticleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// Only the url travels through navigation; the article itself is read back from the database.
@HiltViewModel(assistedFactory = ArticleDetailsViewModel.Factory::class)
class ArticleDetailsViewModel @AssistedInject constructor(
    @Assisted articleUrl: String,
    getArticle: GetArticleUseCase,
) : ViewModel() {

    val state: StateFlow<ArticleDetailsState> = flow {
        val article = getArticle(articleUrl)
        emit(if (article != null) ArticleDetailsState.Loaded(article) else ArticleDetailsState.NotFound)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArticleDetailsState.Loading,
    )

    @AssistedFactory
    interface Factory {
        fun create(articleUrl: String): ArticleDetailsViewModel
    }
}
