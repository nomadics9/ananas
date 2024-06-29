package org.askartv.phone.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.askartv.phone.AppPreferences
import org.askartv.phone.database.ServerDatabaseDao
import org.askartv.phone.repository.JellyfinRepository
import org.askartv.phone.utils.Downloader
import org.askartv.phone.utils.DownloaderImpl
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
