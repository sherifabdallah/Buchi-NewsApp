package com.sherif.newsapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Position of each article in the cached headlines feed. It lives in its own table so search
// results can be saved in `articles` too without showing up in the feed.
@Entity(
    tableName = "top_headlines",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["url"],
            childColumns = ["article_url"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["article_url"], unique = true)],
)
data class TopHeadlineEntity(
    @PrimaryKey val position: Int,
    @ColumnInfo(name = "article_url") val articleUrl: String,
)
