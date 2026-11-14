package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.db.BookEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single entry point the UI uses for the library.
 * Scans run in the app-wide [scope], so leaving a screen doesn't stop them.
 */
class LibraryRepository(
    private val dao: BookDao,
    private val scanner: LibraryScanner,
    private val scope: CoroutineScope,
) {
    val books: Flow<List<BookEntity>> = dao.observeLibrary()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanning = AtomicBoolean(false)

    /** Starts a scan unless one is already running. */
    fun startScan() {
        if (!scanning.compareAndSet(false, true)) return
        scope.launch {
            try {
                scanner.scan { _scanState.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scanState.value = ScanState.Failed(e.message ?: e.javaClass.simpleName)
            } finally {
                scanning.set(false)
            }
        }
    }
}
