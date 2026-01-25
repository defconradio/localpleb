package com.example.pleb2.di

import com.example.nostr.NostrDataSource
import com.example.nostr.NostrRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NostrModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.websocket.WebSockets)
    }

    @Provides
    @Singleton
    fun provideNostrDataSource(client: HttpClient): NostrDataSource = NostrDataSource(client)

    @Provides
    @Singleton
    fun provideNostrRepository(dataSource: NostrDataSource): NostrRepository = NostrRepository(dataSource)
}
