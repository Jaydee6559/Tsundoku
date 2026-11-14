package com.tsundoku.app.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tsundoku.app.data.db.BookTagEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.SeriesInfoEntity
import com.tsundoku.app.data.db.SeriesStatus
import com.tsundoku.app.data.db.StatusEventEntity
import com.tsundoku.app.data.db.TagEntity
import com.tsundoku.app.data.db.TrackingEntity
import com.tsundoku.app.data.db.TsundokuDatabase
import com.tsundoku.app.data.db.UpNextEntity
import com.tsundoku.app.data.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Backups of your reading data: statuses, progress, ratings, notes, tags, Up next, series status.
 *
 * Saved as readable JSON in /.tsundoku/backup on the phone (hidden from scans), newest 7 kept.
 * Nothing leaves the phone: the app has no internet permission. You can copy the files anywhere.
 */
class BackupManager(
    private val context: Context,
    private val db: TsundokuDatabase,
    private val preferences: AppPreferences,
    private val storageRoot: () -> File,
) {
    data class RestoreResult(val restored: Int, val notFound: Int)

    val backupDir: File get() = File(storageRoot(), ".tsundoku/backup")

    fun listBackups(): List<File> =
        backupDir.listFiles { f -> f.isFile && f.name.startsWith(PREFIX) && f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            .orEmpty()

    /** Writes a new backup now. Returns the file, or null if it couldn't be written. */
    suspend fun backupNow(): File? = withContext(Dispatchers.IO) {
        runCatching {
            val json = BackupCodec.encode(collect())
            backupDir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "$PREFIX$stamp.json")
            val tmp = File(backupDir, file.name + ".tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(file)) error("Couldn't save backup")
            listBackups().drop(KEEP).forEach { it.delete() }
            preferences.setLastBackupAt(System.currentTimeMillis())
            file
        }.getOrNull()
    }

    /** Daily automatic backup (called at app start and by the background job). */
    suspend fun backupIfDue() {
        if (!preferences.autoBackup.first()) return
        val last = preferences.lastBackupAt.first()
        if (System.currentTimeMillis() - last < TimeUnit.HOURS.toMillis(DUE_HOURS)) return
        if (db.trackingDao().getAll().isEmpty() && db.upNextDao().getAll().isEmpty()) return // nothing to save yet
        backupNow()
    }

    suspend fun restore(file: File): RestoreResult = withContext(Dispatchers.IO) {
        require(file.length() <= MAX_BYTES) { "Backup file is too large" }
        restoreJson(file.readText())
    }

    suspend fun restore(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readNBytesCompat(MAX_BYTES + 1)
            require(bytes.size <= MAX_BYTES) { "Backup file is too large" }
            String(bytes, Charsets.UTF_8)
        } ?: throw IllegalArgumentException("Couldn't open the file")
        restoreJson(text)
    }

    // ------------------------------------------------------------------ internals

    private suspend fun collect(): BackupData {
        val books = db.bookDao()
        val tracking = db.trackingDao()
        val trackingById = tracking.getAll().associateBy { it.bookId }
        val tagsById = tracking.getAllTagNames().groupBy({ it.bookId }, { it.name })
        val eventsById = tracking.getAllEvents().groupBy { it.bookId }
        val queue = db.upNextDao().getAll().associate { it.bookId to it.position }

        val ids = (trackingById.keys + tagsById.keys + queue.keys).toList()
        val entries = ids.chunked(400).flatMap { books.getByIds(it) }.map { book ->
            val t = trackingById[book.id]
            BookBackup(
                fingerprint = book.fingerprint,
                path = book.originalPath ?: book.path,
                title = book.title,
                status = t?.status?.name,
                rating = t?.rating ?: 0,
                currentChapter = t?.currentChapter ?: 0,
                totalChapters = t?.totalChapters,
                notes = t?.notes,
                startedAt = t?.startedAt,
                finishedAt = t?.finishedAt,
                lastOpenedAt = t?.lastOpenedAt,
                tags = tagsById[book.id].orEmpty(),
                upNextPosition = queue[book.id],
                events = eventsById[book.id].orEmpty().map { EventBackup(it.status.name, it.at) },
            )
        }
        val series = db.seriesDao().getAll().map { SeriesBackup(it.seriesKey, it.status.name) }
        return BackupData(BackupCodec.VERSION, System.currentTimeMillis(), entries, series)
    }

    /** Puts backup data onto the books currently in the library (matched by fingerprint, then path). */
    private suspend fun restoreJson(json: String): RestoreResult {
        val data = BackupCodec.decode(json)
        val books = db.bookDao()
        val tracking = db.trackingDao()
        val upNext = db.upNextDao()
        var restored = 0
        var notFound = 0
        val now = System.currentTimeMillis()

        db.withTransaction {
            for (b in data.books.sortedBy { it.upNextPosition ?: Int.MAX_VALUE }) {
                val book = (if (b.fingerprint.isNotEmpty()) books.findByFingerprint(b.fingerprint) else null)
                    ?: (if (b.path.isNotEmpty()) books.findByPath(b.path) else null)
                if (book == null) {
                    notFound++
                    continue
                }
                val status = b.status?.let { s -> ReadingStatus.entries.firstOrNull { it.name == s } }
                if (status != null) {
                    tracking.upsert(
                        TrackingEntity(
                            bookId = book.id,
                            status = status,
                            rating = b.rating.coerceIn(0, 5),
                            currentChapter = b.currentChapter.coerceAtLeast(0),
                            totalChapters = b.totalChapters?.takeIf { it > 0 },
                            notes = b.notes?.take(20_000),
                            startedAt = b.startedAt,
                            finishedAt = b.finishedAt,
                            lastOpenedAt = b.lastOpenedAt,
                            updatedAt = now,
                        ),
                    )
                }
                for (name in b.tags) {
                    val clean = name.trim().lowercase().take(40)
                    if (clean.isEmpty()) continue
                    val id = tracking.findTagId(clean) ?: tracking.insertTag(TagEntity(name = clean))
                    if (id > 0) tracking.addBookTag(BookTagEntity(bookId = book.id, tagId = id))
                }
                if (b.upNextPosition != null) {
                    upNext.insert(UpNextEntity(bookId = book.id, position = upNext.maxPosition() + 1, addedAt = now))
                }
                // History is only added to books that have none yet, so restoring twice doesn't double it.
                if (b.events.isNotEmpty() && tracking.countEvents(book.id) == 0) {
                    b.events.forEach { e ->
                        ReadingStatus.entries.firstOrNull { it.name == e.status }?.let { s ->
                            tracking.insertEvent(StatusEventEntity(bookId = book.id, status = s, at = e.at))
                        }
                    }
                }
                restored++
            }
            data.series.forEach { s ->
                SeriesStatus.entries.firstOrNull { it.name == s.status }?.let { status ->
                    db.seriesDao().upsert(SeriesInfoEntity(seriesKey = s.key, status = status, updatedAt = now))
                }
            }
        }
        return RestoreResult(restored, notFound)
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val PREFIX = "tsundoku-backup-"
        const val KEEP = 7
        const val DUE_HOURS = 24L
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
