package com.example.pleb2.di

import android.content.Context
import com.example.data.settings.AccountRepository
import com.example.data.settings.AndroidAccountRepository
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
    fun provideAccountRepository(@ApplicationContext context: Context): AccountRepository {
        return AndroidAccountRepository(context)
    }
}
