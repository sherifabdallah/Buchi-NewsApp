package com.sherif.newsapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val content: String?,
    val author: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "published_at") val publishedAt: Long?,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
