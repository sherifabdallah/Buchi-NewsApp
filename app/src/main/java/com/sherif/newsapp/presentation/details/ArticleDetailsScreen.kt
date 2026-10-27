package com.sherif.newsapp.presentation.details

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.presentation.common.ArticleImage
import com.sherif.newsapp.presentation.common.ArticleMeta
import com.sherif.newsapp.presentation.common.MessageView
import com.sherif.newsapp.presentation.common.previewArticles
import com.sherif.newsapp.presentation.common.wideImageSize
import com.sherif.newsapp.presentation.theme.MaxContentWidth
import com.sherif.newsapp.presentation.theme.NewsTheme
import kotlinx.coroutines.launch

@Composable
fun ArticleDetailsScreen(
    articleUrl: String,
    onBack: () -> Unit,
    viewModel: ArticleDetailsViewModel = hiltViewModel<ArticleDetailsViewModel, ArticleDetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(articleUrl) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArticleDetailsContent(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailsContent(
    state: ArticleDetailsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val article = (state as? ArticleDetailsState.Loaded)?.article

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (article != null) ShareButton(article)
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            // The article comes from Room in a few milliseconds, a spinner would just flash.
            ArticleDetailsState.Loading -> Unit

            ArticleDetailsState.NotFound -> MessageView(
                icon = Icons.Rounded.LinkOff,
                title = stringResource(R.string.article_not_found_title),
                message = stringResource(R.string.article_not_found_message),
                modifier = Modifier.padding(padding),
            )

            is ArticleDetailsState.Loaded -> ArticleDetailsBody(
                article = state.article,
                contentPadding = padding,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

@Composable
private fun ArticleDetailsBody(
    article: Article,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val noBrowserMessage = stringResource(R.string.no_browser)
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    // content is only a ~200 character teaser and for paywalled sources it's a subscription pitch,
    // so it's used only when there's no description at all.
    val summary = article.description ?: article.content

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = MaxContentWidth)) {
            ArticleImage(
                url = article.imageUrl,
                modifier = Modifier.wideImageSize(maxHeight = windowHeight / 2),
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ArticleMeta(sourceName = article.sourceName, publishedAt = article.publishedAt)
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Byline(author = article.author)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = summary ?: stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (summary != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Button(
                    onClick = {
                        runCatching { uriHandler.openUri(article.url) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar(noBrowserMessage) } }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.read_full_article))
                }
            }
        }
    }
}

@Composable
private fun Byline(author: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (author != null) {
                Text(
                    text = author.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = if (author != null) stringResource(R.string.byline, author) else stringResource(R.string.unknown_author),
            style = MaterialTheme.typography.labelLarge,
            color = if (author != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShareButton(article: Article) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, article.title)
                putExtra(Intent.EXTRA_TEXT, article.url)
            }
            context.startActivity(Intent.createChooser(send, null))
        },
    ) {
        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
    }
}

@Preview
@Composable
private fun ArticleDetailsPreview() {
    NewsTheme {
        ArticleDetailsContent(state = ArticleDetailsState.Loaded(previewArticles[0]), onBack = {})
    }
}

@Preview
@Composable
private fun ArticleDetailsMissingFieldsPreview() {
    NewsTheme {
        ArticleDetailsContent(state = ArticleDetailsState.Loaded(previewArticles[1]), onBack = {})
    }
}
