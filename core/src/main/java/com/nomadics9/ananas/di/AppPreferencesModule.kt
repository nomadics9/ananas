package com.nomadics9.ananas.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.nomadics9.ananas.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppPreferencesModule {
    @Singleton
    @Provides
    fun provideAppPreferences(sp: SharedPreferences): AppPreferences {
        return AppPreferences(sp)
    }
}
