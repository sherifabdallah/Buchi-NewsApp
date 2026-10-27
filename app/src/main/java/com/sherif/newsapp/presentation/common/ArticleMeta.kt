package com.sherif.newsapp.presentation.common

import android.text.format.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import java.time.Instant

// "CNN · 2 hr. ago"
@Composable
fun ArticleMeta(
    sourceName: String?,
    publishedAt: Instant?,
    modifier: Modifier = Modifier,
) {
    val time = publishedAt?.let { relativeTime(it) }
    if (sourceName == null && time == null) return

    val sourceColor = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            if (sourceName != null) {
                withStyle(SpanStyle(color = sourceColor, fontWeight = FontWeight.SemiBold)) {
                    append(sourceName)
                }
            }
            if (sourceName != null && time != null) append("  ·  ")
            if (time != null) append(time)
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun relativeTime(instant: Instant): String = remember(instant) {
    val now = System.currentTimeMillis()
    // Clamp so a publisher clock that's slightly ahead doesn't produce "in 2 min."
    val time = instant.toEpochMilli().coerceAtMost(now)
    DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
        .toString()
}
