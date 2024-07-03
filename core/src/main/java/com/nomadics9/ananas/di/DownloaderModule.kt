package com.nomadics9.ananas.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.database.ServerDatabaseDao
import com.nomadics9.ananas.repository.JellyfinRepository
import com.nomadics9.ananas.utils.Downloader
import com.nomadics9.ananas.utils.DownloaderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {
    @Singleton
    @Provides
    fun provideDownloader(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        jellyfinRepository: JellyfinRepository,
        appPreferences: AppPreferences,
    ): Downloader {
        return DownloaderImpl(application, serverDatabase, jellyfinRepository, appPreferences)
    }
}
