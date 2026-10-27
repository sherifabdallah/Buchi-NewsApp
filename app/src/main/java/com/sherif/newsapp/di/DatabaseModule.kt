package com.sherif.newsapp.di

import android.content.Context
import androidx.room.Room
import com.sherif.newsapp.data.local.ArticleDao
import com.sherif.newsapp.data.local.NewsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase =
        Room.databaseBuilder(context, NewsDatabase::class.java, "news.db")
            // Everything in here is a copy of API data that can be fetched again,
            // so dropping it on a schema change is fine.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()
}
