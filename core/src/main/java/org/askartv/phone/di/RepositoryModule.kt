package org.askartv.phone.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.askartv.phone.AppPreferences
import org.askartv.phone.api.JellyfinApi
import org.askartv.phone.database.ServerDatabaseDao
import org.askartv.phone.repository.JellyfinRepository
import org.askartv.phone.repository.JellyfinRepositoryImpl
import org.askartv.phone.repository.JellyfinRepositoryOfflineImpl
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
        return when (appPreferences.offlineMode) {
            true -> jellyfinRepositoryOfflineImpl
            false -> jellyfinRepositoryImpl
        }
    }
}
