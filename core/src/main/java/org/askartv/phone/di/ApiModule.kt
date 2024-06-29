package org.askartv.phone.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.askartv.phone.AppPreferences
import org.askartv.phone.api.JellyfinApi
import org.askartv.phone.database.ServerDatabaseDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        @ApplicationContext application: Context,
        appPreferences: AppPreferences,
        database: ServerDatabaseDao,
    ): JellyfinApi {
        val jellyfinApi = JellyfinApi.getInstance(
            context = application,
            requestTimeout = appPreferences.requestTimeout,
            connectTimeout = appPreferences.connectTimeout,
            socketTimeout = appPreferences.socketTimeout,
        )

        val serverId = appPreferences.currentServer ?: return jellyfinApi

        val serverWithAddressAndUser = database.getServerWithAddressAndUser(serverId) ?: return jellyfinApi
        val serverAddress = serverWithAddressAndUser.address ?: return jellyfinApi
        val user = serverWithAddressAndUser.user

        jellyfinApi.apply {
            api.update(
                baseUrl = serverAddress.address,
                accessToken = user?.accessToken,
            )
            userId = user?.id
        }

        return jellyfinApi
    }
}
