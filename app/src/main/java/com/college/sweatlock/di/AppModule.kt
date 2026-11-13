package com.college.sweatlock.di

import android.content.Context
import com.college.sweatlock.data.local.SweatLockDao
import com.college.sweatlock.data.local.SweatLockDatabase
import com.college.sweatlock.data.preferences.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SweatLockDatabase =
        SweatLockDatabase.getInstance(context)

    @Provides
    fun provideDao(db: SweatLockDatabase): SweatLockDao = db.dao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
