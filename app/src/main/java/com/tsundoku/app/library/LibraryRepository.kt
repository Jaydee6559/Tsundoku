package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.db.BookEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single entry point the UI uses for the library.
 *
 * Every change to the books table (full scans, live syncs, Trash) takes [lock], so they run
 * one after another and never see each other's half-finished work.
 */
class LibraryRepository(
    private val dao: BookDao,
    private val scanner: LibraryScanner,
    private val scope: CoroutineScope,
    private val lock: Mutex,
) {
    val books: Flow<List<BookEntity>> = dao.observeLibrary()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /** Results of quiet updates (live syncs + background scans), for the "novels added" notification. */
    private val _quietResults = MutableSharedFlow<ScanState.Done>(extraBufferCapacity = 8)
    val quietResults: SharedFlow<ScanState.Done> = _quietResults.asSharedFlow()

    private val scanQueued = AtomicBoolean(false)

    /** Starts a full scan unless one is already running or waiting. Shows progress on screen. */
    fun startScan() {
        if (!scanQueued.compareAndSet(false, true)) return
        scope.launch {
            try {
                lock.withLock { scanner.scan { _scanState.value = it } }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // Never let a scan problem close the app; show it on screen instead.
                _scanState.value = ScanState.Failed(t.message ?: t.javaClass.simpleName)
            } finally {
                scanQueued.set(false)
            }
        }
    }

    /** Full scan that waits for the result, without on-screen progress (background job). */
    suspend fun scanQuietly(): ScanState.Done? = runCatching {
        lock.withLock { scanner.scan {} }
    }.getOrNull()?.also { _quietResults.tryEmit(it) }

    /** Updates only the given files (reported by the folder watcher). */
    suspend fun syncPaths(paths: Set<String>): ScanState.Done? = runCatching {
        lock.withLock { scanner.syncPaths(paths) }
    }.getOrNull()?.also { _quietResults.tryEmit(it) }

    /** Folders that currently contain books, as full paths. */
    suspend fun bookFolders(storageRoot: String): List<String> = runCatching {
        dao.bookFolders().map { if (it.isEmpty()) storageRoot else "$storageRoot/$it" }
    }.getOrDefault(emptyList())
}
