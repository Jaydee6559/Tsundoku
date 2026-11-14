package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.db.BookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Safe deleting.
 *
 * "Delete" moves the file into a hidden folder (/.tsundoku/trash) on the same storage, which is
 * an instant rename. The book keeps all its tracking data and can be restored for 30 days.
 * Only "Delete forever" / "Empty trash" (always confirmed in the UI) really removes anything.
 */
class TrashRepository(
    private val dao: BookDao,
    private val storageRoot: () -> File,
    private val coversDir: File,
    private val scope: CoroutineScope,
    /** Shared with scans and live syncs, so file moves and database updates never interleave. */
    private val lock: Mutex,
) {
    private val trashDir: File get() = File(storageRoot(), ".tsundoku/trash")

    val trashed: Flow<List<BookEntity>> = dao.observeTrash()

    /** Moves books to Trash. Returns how many were moved. */
    suspend fun moveToTrash(ids: Collection<Long>): Int = locked {
        val now = System.currentTimeMillis()
        var moved = 0
        for (book in dao.getByIds(ids.toList())) {
            if (book.trashedAt != null) continue
            runCatching {
                val src = File(book.path)
                val dest = File(trashDir, TrashRules.trashFileName(book.id, now, src.name))
                // A file that's already gone still leaves the library (Trash cleans up its record later).
                val existed = src.exists()
                if (existed && !moveFile(src, dest)) return@runCatching
                dao.markTrashed(book.id, trashPath = if (existed) dest.path else book.path, originalPath = book.path, now = now)
                moved++
            }
        }
        moved
    }

    /** Same as [moveToTrash], but keeps running after the calling screen closes. */
    fun moveToTrashInBackground(ids: Collection<Long>) {
        scope.launch { moveToTrash(ids) }
    }

    /** Puts books back where they were (never overwriting). Returns how many were restored. */
    suspend fun restore(ids: Collection<Long>): Int = locked {
        val root = storageRoot().absolutePath
        val now = System.currentTimeMillis()
        var restored = 0
        for (book in dao.getByIds(ids.toList())) {
            if (book.trashedAt == null) continue
            val original = book.originalPath ?: continue
            runCatching {
                val src = File(book.path)
                if (!src.exists()) return@runCatching
                // Free = no file there AND no other book record using that path.
                val taken = dao.pathsStartingWith(original.substringBeforeLast('.')).toSet()
                val target = File(TrashRules.restoreTarget(original) { File(it).exists() || it in taken })
                if (moveFile(src, target)) {
                    dao.markRestored(book.id, target.path, target.name, folderOf(target.path, root), now)
                    restored++
                }
            }
        }
        restored
    }

    /** Permanently deletes books from Trash: file, cover thumbnail and all tracking data. */
    suspend fun deleteForever(ids: Collection<Long>): Int = locked {
        val trashed = dao.getByIds(ids.toList()).filter { it.trashedAt != null }
        // Only forget a book once its file is really gone. If Android refuses to delete it,
        // it stays in Trash instead of silently lingering on storage (and coming back later).
        val books = trashed.filter { book ->
            val file = File(book.path)
            !file.exists() || file.delete() || !file.exists()
        }
        books.map { it.id }.chunked(BookDao.CHUNK).forEach { dao.deleteByIds(it) }
        books.forEach { removeUnusedThumb(it) }
        books.size
    }

    suspend fun emptyTrash(): Int = deleteForever(dao.getTrashed().map { it.id })

    /** Deletes books that have been in Trash for 30+ days. Runs quietly at app start. */
    fun purgeExpiredInBackground() {
        scope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val expired = dao.getTrashed().filter { TrashRules.isExpired(it.trashedAt ?: now, now) }
                if (expired.isNotEmpty()) deleteForever(expired.map { it.id })
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private suspend fun <T> locked(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { lock.withLock { block() } }

    /** Rename (instant on the same storage); falls back to copy + delete across storages. */
    private fun moveFile(src: File, dest: File): Boolean {
        dest.parentFile?.mkdirs()
        if (src.renameTo(dest)) return true
        return runCatching {
            src.copyTo(dest, overwrite = false)
            if (dest.length() == src.length()) {
                src.delete()
            } else {
                dest.delete() // incomplete copy: keep the original, report failure
                false
            }
        }.getOrDefault(false)
    }

    private suspend fun removeUnusedThumb(book: BookEntity) {
        val thumb = book.coverThumbPath ?: return
        if (dao.countWithFingerprint(book.fingerprint) == 0) File(thumb).takeIf { it.parentFile == coversDir }?.delete()
    }
}
