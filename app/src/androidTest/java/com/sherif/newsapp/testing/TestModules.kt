package com.sherif.newsapp.testing

import android.content.Context
import androidx.room.Room
import com.sherif.newsapp.data.local.ArticleDao
import com.sherif.newsapp.data.local.NewsDatabase
import com.sherif.newsapp.data.remote.NewsApi
import com.sherif.newsapp.di.DatabaseModule
import com.sherif.newsapp.di.NetworkModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

// Everything else in the app graph is real; only the network and the database file are swapped.
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
abstract class FakeNetworkModule {

    @Binds
    abstract fun bindNewsApi(api: FakeNewsApi): NewsApi
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase =
        Room.inMemoryDatabaseBuilder(context, NewsDatabase::class.java).build()

    @Provides
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()
}
