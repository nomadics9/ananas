package com.nomadics9.ananas.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.api.JellyfinApi
import com.nomadics9.ananas.database.ServerDatabaseDao
import com.nomadics9.ananas.repository.JellyfinRepository
import com.nomadics9.ananas.repository.JellyfinRepositoryImpl
import com.nomadics9.ananas.repository.JellyfinRepositoryOfflineImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideJellyfinRepositoryImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): JellyfinRepositoryImpl {
        println("Creating new jellyfinRepositoryImpl")
        return JellyfinRepositoryImpl(application, jellyfinApi, serverDatabase, appPreferences)
    }

    @Singleton
    @Provides
    fun provideJellyfinRepositoryOfflineImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): JellyfinRepositoryOfflineImpl {
        println("Creating new jellyfinRepositoryOfflineImpl")
        return JellyfinRepositoryOfflineImpl(application, jellyfinApi, serverDatabase, appPreferences)
    }

    @Provides
    fun provideJellyfinRepository(
        jellyfinRepositoryImpl: JellyfinRepositoryImpl,
        jellyfinRepositoryOfflineImpl: JellyfinRepositoryOfflineImpl,
        appPreferences: AppPreferences,
    ): JellyfinRepository {
        println("Creating new JellyfinRepository")
        return when (appPreferences.isOffline) {
            true -> jellyfinRepositoryOfflineImpl
            false -> jellyfinRepositoryImpl
        }
    }
}
