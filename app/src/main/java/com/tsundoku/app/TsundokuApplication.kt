package com.tsundoku.app

import android.app.Application
import com.tsundoku.app.data.db.TsundokuDatabase
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.diagnostics.CrashLog
import com.tsundoku.app.library.EpubDiscovery
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.LiveWatcher
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.library.SeriesRepository
import com.tsundoku.app.library.UpNextRepository
import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.backup.BackupManager
import com.tsundoku.app.library.LibraryScanner
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import com.tsundoku.app.notifications.NewBooksNotifier
import com.tsundoku.app.work.LibrarySyncWorker
import android.os.Environment
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
        container.startBackgroundWork()
        LibrarySyncWorker.schedule(this)
    }
}

class AppContainer(application: Application) {
    /** Lives as long as the app: background work (scans) runs here. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferences: AppPreferences by lazy { AppPreferences(application) }

    /** Taken by scans, live syncs and Trash so they never change the books table at the same time. */
    private val libraryLock = Mutex()

    private val database: TsundokuDatabase by lazy { TsundokuDatabase.create(application) }

    val library: LibraryRepository by lazy {
        LibraryRepository(
            dao = database.bookDao(),
            scanner = LibraryScanner(
                discovery = EpubDiscovery(application),
                dao = database.bookDao(),
                coversDir = File(application.filesDir, "covers"),
                excludedFolders = { preferences.excludedFoldersNow() },
            ),
            scope = appScope,
            lock = libraryLock,
        )
    }

    val tracking: TrackingRepository by lazy { TrackingRepository(database, appScope) }

    val trash: TrashRepository by lazy {
        TrashRepository(
            dao = database.bookDao(),
            storageRoot = { Environment.getExternalStorageDirectory() },
            coversDir = File(application.filesDir, "covers"),
            scope = appScope,
            lock = libraryLock,
        )
    }

    val series: SeriesRepository by lazy { SeriesRepository(database, tracking) }

    val upNext: UpNextRepository by lazy { UpNextRepository(database) }

    val bookDao: BookDao get() = database.bookDao()

    val backup: BackupManager by lazy {
        BackupManager(application, database, preferences, storageRoot = { Environment.getExternalStorageDirectory() })
    }

    val notifier: NewBooksNotifier by lazy { NewBooksNotifier(application) }

    /** Instant updates for folders that hold books (and common download folders). */
    val watcher: LiveWatcher by lazy {
        LiveWatcher(
            scope = appScope,
            storageRoot = { Environment.getExternalStorageDirectory() },
            bookFolders = { root -> library.bookFolders(root) },
            onChanges = { paths -> library.syncPaths(paths) },
            excluded = { preferences.excludedFoldersNow() },
        )
    }

    /**
     * Hooks things together once at app start:
     *  - quiet updates (live syncs, background scans) -> one "novels added" notification,
     *  - any finished scan or sync -> re-check which folders to watch.
     */
    fun startBackgroundWork() {
        appScope.launch {
            library.quietResults.collect { done ->
                notifier.onBooksAdded(done.added, done.addedTitles)
                if (done.added > 0 || done.moved > 0) watcher.refresh()
            }
        }
        appScope.launch {
            library.scanState.collect { if (it is ScanState.Done) watcher.refresh() }
        }
    }
}
