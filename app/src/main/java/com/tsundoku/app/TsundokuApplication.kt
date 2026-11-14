package com.tsundoku.app

import android.app.Application
import com.tsundoku.app.data.db.TsundokuDatabase
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.diagnostics.CrashLog
import com.tsundoku.app.library.EpubDiscovery
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.LibraryScanner
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * App-wide objects live here (simple manual dependency injection).
 */
class TsundokuApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this) // first, so even startup crashes are recorded
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    /** Lives as long as the app: background work (scans) runs here. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferences: AppPreferences by lazy { AppPreferences(application) }

    private val database: TsundokuDatabase by lazy { TsundokuDatabase.create(application) }

    val library: LibraryRepository by lazy {
        LibraryRepository(
            dao = database.bookDao(),
            scanner = LibraryScanner(
                discovery = EpubDiscovery(application),
                dao = database.bookDao(),
                coversDir = File(application.filesDir, "covers"),
            ),
            scope = appScope,
        )
    }

    val tracking: TrackingRepository by lazy { TrackingRepository(database, appScope) }

    val trash: TrashRepository by lazy {
        TrashRepository(
            dao = database.bookDao(),
            storageRoot = { android.os.Environment.getExternalStorageDirectory() },
            coversDir = File(application.filesDir, "covers"),
            scope = appScope,
        )
    }
}
