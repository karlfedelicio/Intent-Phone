package com.intentphone.launcher

import android.app.Application
import com.intentphone.launcher.data.repository.InstalledAppsRepository
import com.intentphone.launcher.data.repository.IntentPreferencesRepository

class IntentPhoneApp : Application() {

    lateinit var preferencesRepository: IntentPreferencesRepository
        private set

    lateinit var appsRepository: InstalledAppsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = IntentPreferencesRepository(applicationContext)
        appsRepository = InstalledAppsRepository(applicationContext)
    }

    companion object {
        lateinit var instance: IntentPhoneApp
            private set
    }
}