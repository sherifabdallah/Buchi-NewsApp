package com.sherif.newsapp.presentation.articles.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherif.newsapp.R
import com.sherif.newsapp.presentation.theme.MaxContentWidth

// Same shape as the real list (one featured card, then rows) so nothing jumps when content arrives.
@Composable
fun ArticleListSkeleton(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val description = stringResource(R.string.loading_articles)

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .semantics { contentDescription = description }
                // Read in the layer block so the pulse only redraws instead of recomposing every frame.
                .graphicsLayer { alpha = pulse },
        ) {
            Column(Modifier.padding(16.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(16f / 9f), MaterialTheme.shapes.large)
                Spacer(Modifier.height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.3f).height(12.dp))
                Spacer(Modifier.height(10.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(22.dp))
                Spacer(Modifier.height(6.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(22.dp))
            }
            repeat(4) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(12.dp))
                        Spacer(Modifier.height(10.dp))
                        SkeletonBlock(Modifier.fillMaxWidth().height(16.dp))
                        Spacer(Modifier.height(6.dp))
                        SkeletonBlock(Modifier.fillMaxWidth(0.8f).height(16.dp))
                    }
                    SkeletonBlock(Modifier.size(88.dp), MaterialTheme.shapes.medium)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
