package com.assessment.minilogbook

import android.app.Application
import com.assessment.minilogbook.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MiniLogbookApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MiniLogbookApplication)
            modules(appModule)
        }
    }
}

