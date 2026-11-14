package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.TrackingEntity

/** Pure rules for Home, Up Next and sharing. Unit-tested. */
object HomeRules {

    /** The book to show in "Continue reading": the Reading book you opened (or updated) most recently. */
    fun continueReading(books: List<BookEntity>, tracking: List<TrackingEntity>): Pair<BookEntity, TrackingEntity>? {
        val byId = books.associateBy { it.id }
        return tracking
            .filter { it.status == ReadingStatus.READING && it.bookId in byId }
            .maxByOrNull { it.lastOpenedAt ?: it.updatedAt }
            ?.let { byId.getValue(it.bookId) to it }
    }

    data class Counts(val reading: Int, val plan: Int, val completed: Int)

    fun counts(books: List<BookEntity>, statuses: Map<Long, ReadingStatus>): Counts {
        var reading = 0
        var plan = 0
        var completed = 0
        for (b in books) {
            when (statuses[b.id] ?: ReadingStatus.PLAN) {
                ReadingStatus.READING -> reading++
                ReadingStatus.PLAN -> plan++
                ReadingStatus.COMPLETED -> completed++
                ReadingStatus.ON_HOLD, ReadingStatus.DROPPED -> Unit
            }
        }
        return Counts(reading, plan, completed)
    }

    /** Moves [id] up (delta -1) or down (+1) in the queue; returns the new order. */
    fun move(order: List<Long>, id: Long, delta: Int): List<Long> {
        val from = order.indexOf(id)
        if (from < 0) return order
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return order
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }

    /**
     * A safe file name for a shared book: no folder separators or characters Android storage
     * rejects, not hidden, not too long, always ending in .epub.
     */
    fun safeFileName(raw: String?): String {
        var name = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        name = name.replace(UNSAFE_CHARS, "_").trim().trimStart('.')
        if (name.isBlank()) name = "shared-book"
        val base = name.removeSuffix(".epub").removeSuffix(".EPUB").take(MAX_NAME)
        return "$base.epub"
    }

    // \p{Cc} (control characters) works on both desktop Java and Android's stricter ICU engine.
    private val UNSAFE_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cc}]")
    private const val MAX_NAME = 120
}
