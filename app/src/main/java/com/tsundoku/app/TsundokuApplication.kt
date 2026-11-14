package com.tsundoku.app

import android.app.Application
import com.tsundoku.app.data.settings.AppPreferences

/**
 * App-wide objects live here (simple manual dependency injection).
 * Later batches add the database, scanner and repositories to [AppContainer].
 */
class TsundokuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val preferences: AppPreferences by lazy { AppPreferences(application) }
}
