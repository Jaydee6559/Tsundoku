package com.tsundoku.app.library

import androidx.room.withTransaction
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.BookTagEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.StatusEventEntity
import com.tsundoku.app.data.db.TagEntity
import com.tsundoku.app.data.db.TrackingEntity
import com.tsundoku.app.data.db.TsundokuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Everything the book screen shows. */
data class BookDetail(
    val book: BookEntity,
    val tracking: TrackingEntity,
    val tags: List<TagEntity>,
) {
    val totalChapters: Int get() = TrackingRules.totalChapters(tracking, book.spineCount)
}

/** Reading status, progress, rating, notes and tags. */
class TrackingRepository(
    private val db: TsundokuDatabase,
    private val scope: CoroutineScope,
) {
    private val books = db.bookDao()
    private val dao = db.trackingDao()

    fun observeDetail(bookId: Long): Flow<BookDetail?> = combine(
        books.observeBook(bookId),
        dao.observeTracking(bookId),
        dao.observeTagsForBook(bookId),
    ) { book, tracking, tags ->
        book?.let { BookDetail(it, tracking ?: TrackingRules.default(bookId, 0), tags) }
    }

    /** bookId -> status, for the Library grid and its filters. Books without a row are "Plan to read". */
    fun observeStatuses(): Flow<Map<Long, ReadingStatus>> =
        dao.observeAll().map { list -> list.associate { it.bookId to it.status } }

    suspend fun setStatus(bookId: Long, status: ReadingStatus) =
        update(bookId) { t, total, now -> TrackingRules.withStatus(t, status, total, now) }

    suspend fun setChapter(bookId: Long, chapter: Int) =
        update(bookId) { t, total, now -> TrackingRules.withChapter(t, chapter, total, now) }

    suspend fun setTotalChapters(bookId: Long, total: Int?) =
        update(bookId) { t, _, now -> TrackingRules.withTotalChapters(t, total, now) }

    suspend fun setRating(bookId: Long, stars: Int) =
        update(bookId) { t, _, now -> TrackingRules.withRating(t, stars, now) }

    suspend fun setNotes(bookId: Long, notes: String) =
        update(bookId) { t, _, now ->
            val clean = notes.take(MAX_NOTES).ifBlank { null }
            if (clean == t.notes) t else t.copy(notes = clean, updatedAt = now)
        }

    /** Saves notes even if the screen is closing (runs in the app-wide scope). */
    fun saveNotesInBackground(bookId: Long, notes: String) {
        scope.launch { setNotes(bookId, notes) }
    }

    suspend fun markOpened(bookId: Long) =
        update(bookId) { t, _, now -> t.copy(lastOpenedAt = now, updatedAt = now) }

    suspend fun addTag(bookId: Long, rawName: String) {
        val name = rawName.trim().lowercase().replace(Regex("\\s+"), " ").take(MAX_TAG)
        if (name.isEmpty()) return
        db.withTransaction {
            val id = dao.findTagId(name) ?: dao.insertTag(TagEntity(name = name))
            if (id > 0) dao.addBookTag(BookTagEntity(bookId = bookId, tagId = id))
        }
    }

    suspend fun removeTag(bookId: Long, tagId: Long) {
        db.withTransaction {
            dao.removeBookTag(bookId, tagId)
            dao.deleteUnusedTags()
        }
    }

    /** Reads, changes and saves tracking in one transaction; logs status changes. */
    private suspend fun update(
        bookId: Long,
        change: (current: TrackingEntity, totalChapters: Int, now: Long) -> TrackingEntity,
    ) {
        db.withTransaction {
            val book = books.getById(bookId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val current = dao.getTracking(bookId) ?: TrackingRules.default(bookId, now)
            val next = change(current, TrackingRules.totalChapters(current, book.spineCount), now)
            if (next == current) return@withTransaction
            dao.upsert(next)
            if (next.status != current.status) {
                dao.insertEvent(StatusEventEntity(bookId = bookId, status = next.status, at = now))
            }
        }
    }

    private companion object {
        const val MAX_NOTES = 20_000
        const val MAX_TAG = 40
    }
}
