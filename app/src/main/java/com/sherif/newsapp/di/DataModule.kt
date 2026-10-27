package com.sherif.newsapp.di

import com.sherif.newsapp.data.connectivity.ConnectivityManagerNetworkMonitor
import com.sherif.newsapp.data.repository.OfflineFirstArticlesRepository
import com.sherif.newsapp.domain.connectivity.NetworkMonitor
import com.sherif.newsapp.domain.repository.ArticlesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindArticlesRepository(repository: OfflineFirstArticlesRepository): ArticlesRepository

    @Binds
    abstract fun bindNetworkMonitor(monitor: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
