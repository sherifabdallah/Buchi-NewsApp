package com.sherif.newsapp.presentation.articles

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.domain.model.DataError
import com.sherif.newsapp.presentation.articles.components.ArticleListSkeleton
import com.sherif.newsapp.presentation.articles.components.ArticleRow
import com.sherif.newsapp.presentation.articles.components.ArticleSearchBar
import com.sherif.newsapp.presentation.articles.components.FeaturedArticleCard
import com.sherif.newsapp.presentation.common.ErrorMessage
import com.sherif.newsapp.presentation.common.MessageView
import com.sherif.newsapp.presentation.common.ObserveAsEvents
import com.sherif.newsapp.presentation.common.previewArticles
import com.sherif.newsapp.presentation.common.titleRes
import com.sherif.newsapp.presentation.theme.MaxContentWidth
import com.sherif.newsapp.presentation.theme.NewsTheme

@Composable
fun ArticlesScreen(
    onArticleClick: (Article) -> Unit,
    viewModel: ArticlesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is ArticlesEffect.ShowRefreshError -> {
                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(effect.error.titleRes),
                    actionLabel = resources.getString(R.string.action_retry),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onIntent(ArticlesIntent.Refresh)
                }
            }
        }
    }

    ArticlesContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onArticleClick = onArticleClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesContent(
    state: ArticlesState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ArticlesIntent) -> Unit,
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val headlinesListState = rememberLazyListState()
    // A new set of results should start from the top, not inherit the previous scroll position.
    val searchListState = remember(state.searchResults?.articles) { LazyListState() }
    val list = state.visibleList

    Scaffold(
        // Keeps the whole screen clear of a side camera cutout in landscape, so the title, search field
        // and list share the same edges instead of only the app bar moving out of the way.
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ArticlesTopBar(
                query = state.query,
                onQueryChange = { onIntent(ArticlesIntent.QueryChanged(it)) },
                onClearQuery = { onIntent(ArticlesIntent.ClearQuery) },
                // With nothing on screen the full screen error already says we're offline.
                showOfflineBanner = state.isOffline && list.articles.isNotEmpty(),
                showProgress = list.isLoading && list.articles.isNotEmpty() && !state.isRefreshing,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(ArticlesIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when {
                list.articles.isNotEmpty() -> ArticleList(
                    articles = list.articles,
                    featureFirst = state.searchResults == null,
                    showCachedNotice = list.isFromCache && !state.isOffline,
                    listState = if (state.searchResults == null) headlinesListState else searchListState,
                    bottomPadding = padding.calculateBottomPadding(),
                    onArticleClick = onArticleClick,
                )

                list.isLoading -> ArticleListSkeleton()

                list.error != null -> ErrorMessage(
                    error = list.error,
                    onRetry = { onIntent(ArticlesIntent.Refresh) },
                )

                state.searchResults != null -> MessageView(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.no_results_title, state.query.trim()),
                    message = stringResource(R.string.no_results_message),
                )

                else -> MessageView(
                    icon = Icons.Outlined.Newspaper,
                    title = stringResource(R.string.no_headlines_title),
                    message = stringResource(R.string.no_headlines_message),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticlesTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    showOfflineBanner: Boolean,
    showProgress: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The title collapses on scroll, the search field stays pinned underneath it.
            TopAppBar(
                title = { HeadlinesTitle() },
                // Same width as the list so the title lines up with it on tablets.
                modifier = Modifier.widthIn(max = MaxContentWidth),
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            ArticleSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
            AnimatedVisibility(visible = showOfflineBanner) {
                OfflineBanner()
            }
            // Always takes up the same space, so the list doesn't shift when the bar appears.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            ) {
                if (showProgress) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun HeadlinesTitle() {
    val context = LocalContext.current
    val today = remember {
        DateUtils.formatDateTime(
            context,
            System.currentTimeMillis(),
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR,
        )
    }
    Column {
        Text(
            text = today,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.top_headlines),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

@Composable
private fun ArticleList(
    articles: List<Article>,
    featureFirst: Boolean,
    showCachedNotice: Boolean,
    listState: LazyListState,
    bottomPadding: Dp,
    onArticleClick: (Article) -> Unit,
) {
    // Hide the keyboard once the user starts scrolling through results. Only real drags count,
    // programmatic scrolls (like jumping to the top for new results) shouldn't close it mid-typing.
    val focusManager = LocalFocusManager.current
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        if (isDragged) focusManager.clearFocus()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidePadding = ((maxWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding, bottom = bottomPadding + 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showCachedNotice) {
                item(key = "cached-notice", contentType = "notice") {
                    CachedResultsNotice(Modifier.animateItem())
                }
            }
            itemsIndexed(
                items = articles,
                key = { _, article -> article.url },
                contentType = { index, _ -> if (featureFirst && index == 0) "featured" else "row" },
            ) { index, article ->
                if (featureFirst && index == 0) {
                    FeaturedArticleCard(
                        article = article,
                        onClick = { onArticleClick(article) },
                        modifier = Modifier.animateItem(),
                    )
                } else {
                    Column(Modifier.animateItem()) {
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        ArticleRow(article = article, onClick = { onArticleClick(article) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CachedResultsNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = stringResource(R.string.search_cached_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview
@Composable
private fun ArticlesContentPreview() {
    NewsTheme {
        ArticlesContent(
            state = ArticlesState(headlines = ArticleListState(articles = previewArticles)),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onArticleClick = {},
        )
    }
}

@Preview
@Composable
private fun ArticlesOfflineErrorPreview() {
    NewsTheme {
        ArticlesContent(
            state = ArticlesState(
                headlines = ArticleListState(error = DataError.NoConnection),
                isOffline = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onArticleClick = {},
        )
    }
}
