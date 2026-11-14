package com.tsundoku.app.library

import androidx.room.withTransaction
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.TsundokuDatabase
import com.tsundoku.app.data.db.UpNextEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Your reading queue: what to read after the current book. */
class UpNextRepository(private val db: TsundokuDatabase) {

    private val dao = db.upNextDao()

    val queue: Flow<List<BookEntity>> = dao.observeQueue()
    val ids: Flow<Set<Long>> = dao.observeIds().map { it.toSet() }

    /** Adds books to the end of the queue (books already queued keep their place). */
    suspend fun add(bookIds: Collection<Long>) {
        db.withTransaction {
            var position = dao.maxPosition()
            val now = System.currentTimeMillis()
            bookIds.forEach { id -> dao.insert(UpNextEntity(bookId = id, position = ++position, addedAt = now)) }
        }
    }

    suspend fun remove(bookId: Long) = dao.remove(bookId)

    /** delta = -1 moves the book one place earlier, +1 one place later. */
    suspend fun move(bookId: Long, delta: Int) {
        db.withTransaction {
            val order = HomeRules.move(dao.orderedIds(), bookId, delta)
            order.forEachIndexed { index, id -> dao.setPosition(id, index) }
        }
    }
}
