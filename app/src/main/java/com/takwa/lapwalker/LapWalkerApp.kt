package com.takwa.lapwalker

import android.app.Application
import com.takwa.lapwalker.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LapWalkerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@LapWalkerApp)
            modules(appModule)
        }
    }
}
