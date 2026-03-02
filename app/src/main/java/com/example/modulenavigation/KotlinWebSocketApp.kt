package com.example.modulenavigation

import android.app.Application
import com.example.modulenavigation.di.webSocketModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Custom Application class to initialize Koin dependency injection
 */
class KotlinWebSocketApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidContext(this@KotlinWebSocketApp)
            modules(
                webSocketModule
            )
        }
    }
}

