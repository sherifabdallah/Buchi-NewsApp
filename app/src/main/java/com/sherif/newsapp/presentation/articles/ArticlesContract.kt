package com.sherif.newsapp.presentation.articles

import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataError

data class ArticlesState(
    val query: String = "",
    val headlines: ArticleListState = ArticleListState(isLoading = true),
    // Null while there's no query, or the first search for it hasn't started yet.
    val searchResults: ArticleListState? = null,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val visibleList: ArticleListState get() = searchResults ?: headlines
}

data class ArticleListState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val isFromCache: Boolean = false,
)

sealed interface ArticlesIntent {
    data class QueryChanged(val query: String) : ArticlesIntent
    data object ClearQuery : ArticlesIntent
    data object Refresh : ArticlesIntent
}

sealed interface ArticlesEffect {
    data class ShowRefreshError(val error: DataError) : ArticlesEffect
}
