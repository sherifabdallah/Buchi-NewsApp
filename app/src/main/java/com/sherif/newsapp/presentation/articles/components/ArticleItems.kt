package com.sherif.newsapp.presentation.articles.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sherif.newsapp.R
import com.sherif.newsapp.domain.model.Article
import com.sherif.newsapp.presentation.common.ArticleImage
import com.sherif.newsapp.presentation.common.ArticleMeta
import com.sherif.newsapp.presentation.common.previewArticles
import com.sherif.newsapp.presentation.common.wideImageSize
import com.sherif.newsapp.presentation.theme.NewsTheme

@Composable
fun FeaturedArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.open_article), onClick = onClick)
            .padding(16.dp),
    ) {
        ArticleImage(
            url = article.imageUrl,
            modifier = Modifier
                .wideImageSize(maxHeight = windowHeight / 2)
                .clip(MaterialTheme.shapes.large),
        )
        Spacer(Modifier.height(12.dp))
        ArticleMeta(sourceName = article.sourceName, publishedAt = article.publishedAt)
        Spacer(Modifier.height(6.dp))
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (article.description != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = article.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ArticleRow(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.open_article), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            ArticleMeta(sourceName = article.sourceName, publishedAt = article.publishedAt)
            Spacer(Modifier.height(4.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ArticleImage(
            url = article.imageUrl,
            modifier = Modifier
                .size(88.dp)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

@Preview
@Composable
private fun ArticleItemsPreview() {
    NewsTheme {
        Surface {
            Column {
                FeaturedArticleCard(article = previewArticles[0], onClick = {})
                ArticleRow(article = previewArticles[1], onClick = {})
            }
        }
    }
}
