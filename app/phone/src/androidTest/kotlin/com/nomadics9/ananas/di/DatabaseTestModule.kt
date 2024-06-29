package com.nomadics9.ananas.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.nomadics9.ananas.database.ServerDatabase
import com.nomadics9.ananas.database.ServerDatabaseDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseTestModule {
    @Singleton
    @Provides
    fun provideServerDatabaseDao(@ApplicationContext app: Context): ServerDatabaseDao {
        return Room.inMemoryDatabaseBuilder(
            app.applicationContext,
            ServerDatabase::class.java,
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
            .getServerDatabaseDao()
    }
}
