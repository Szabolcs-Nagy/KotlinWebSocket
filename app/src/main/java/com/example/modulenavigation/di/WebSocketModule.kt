package com.example.modulenavigation.di

import com.example.modulenavigation.websocket.WebSocketManager
import okhttp3.OkHttpClient
import org.koin.dsl.module

/**
 * Koin dependency injection module for WebSocket
 */
val webSocketModule = module {
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .build()
    }

    single<WebSocketManager> {
        WebSocketManager(get<OkHttpClient>())
    }
}

