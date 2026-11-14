package com.tsundoku.app.library

import android.os.FileObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Watches "hot" folders (ones that hold books, plus common download folders) and reports
 * EPUBs that appear, change, move or disappear, within a few seconds.
 *
 * Android can only watch one folder at a time (no sub-folders), so there is one observer per
 * folder. Events are collected for [DEBOUNCE_MS] first: a download writes in many small steps,
 * and copying 80 books at once should be one update, not 80.
 * Anything this misses (new sub-folders, the app being closed) is caught by the full scans.
 */
class LiveWatcher(
    private val scope: CoroutineScope,
    private val storageRoot: () -> File,
    private val bookFolders: suspend (root: String) -> List<String>,
    private val onChanges: suspend (Set<String>) -> Unit,
    private val excluded: suspend () -> Set<String> = { emptySet() },
) {
    /** How many folders are being watched right now (shown on the Scanner status screen). */
    private val _watchedCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val watchedCount: kotlinx.coroutines.flow.StateFlow<Int> = _watchedCount
    private val observers = mutableListOf<FileObserver>()
    private val pending = LinkedHashSet<String>()
    private val pendingLock = Any()
    private var flushJob: Job? = null
    private val restartLock = Mutex()

    /** (Re)starts watching. Safe to call repeatedly, e.g. after each scan finds new folders. */
    fun refresh() {
        scope.launch {
            restartLock.withLock {
                val root = storageRoot().absolutePath
                val skip = excluded()
                val folders = (bookFolders(root) + DEFAULT_FOLDERS.map { "$root/$it" })
                    .map { File(it) }
                    .filter { it.isDirectory && !WatchRules.isHiddenPath(it.absolutePath, root) }
                    .filterNot { FolderRules.isExcluded(it.absolutePath.removePrefix(root).trim('/'), skip) }
                    .distinctBy { it.absolutePath }
                    .take(MAX_FOLDERS)

                stopObservers()
                folders.forEach { dir -> observers += observerFor(dir).also { it.startWatching() } }
                _watchedCount.value = observers.size
            }
        }
    }

    fun stop() {
        synchronized(observers) { stopObservers() }
    }

    private fun stopObservers() {
        observers.forEach { runCatching { it.stopWatching() } }
        observers.clear()
    }

    private fun observerFor(dir: File): FileObserver = object : FileObserver(dir, EVENTS) {
        override fun onEvent(event: Int, path: String?) {
            if (path == null || !WatchRules.isEpubName(path)) return
            if ((event and EVENTS) == 0) return
            enqueue(File(dir, path).absolutePath)
        }
    }

    private fun enqueue(path: String) {
        synchronized(pendingLock) { pending += path }
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(DEBOUNCE_MS)
            val batch = synchronized(pendingLock) { pending.toSet().also { pending.clear() } }
            if (batch.isNotEmpty()) onChanges(batch)
        }
    }

    companion object {
        /** Finished writing, moved in/out, deleted. (Not CREATE: the file is still downloading then.) */
        private const val EVENTS =
            FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM or FileObserver.DELETE

        const val DEBOUNCE_MS = 2_500L
        private const val MAX_FOLDERS = 300

        /** Common places where books arrive, watched even before they hold any book. */
        private val DEFAULT_FOLDERS = listOf(
            "Download", "Documents", "Books", "Telegram", "Download/Telegram",
            "Android/media/org.telegram.messenger/Telegram/Telegram Documents",
        )
    }
}

/** Pure rules for the watcher (no Android classes, so they're unit-testable). */
object WatchRules {
    fun isEpubName(name: String): Boolean = name.endsWith(".epub", ignoreCase = true) && !name.startsWith(".")

    /** True for anything inside a hidden folder (e.g. Tsundoku's own /.tsundoku/trash). */
    fun isHiddenPath(path: String, root: String): Boolean =
        path.removePrefix(root).split('/').any { it.startsWith(".") }
}
