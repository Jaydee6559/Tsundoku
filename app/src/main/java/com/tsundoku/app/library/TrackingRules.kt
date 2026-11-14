package com.tsundoku.app.library

import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.TrackingEntity

/**
 * How tracking changes when you tap things. Pure functions, unit-tested.
 */
object TrackingRules {

    /** A book you haven't touched yet: "Plan to read", no progress. */
    fun default(bookId: Long, now: Long) = TrackingEntity(
        bookId = bookId,
        status = ReadingStatus.PLAN,
        rating = 0,
        currentChapter = 0,
        totalChapters = null,
        notes = null,
        startedAt = null,
        finishedAt = null,
        lastOpenedAt = null,
        updatedAt = now,
    )

    /** Chapter total: yours if you set one, otherwise the EPUB's. */
    fun totalChapters(tracking: TrackingEntity?, spineCount: Int): Int = tracking?.totalChapters ?: spineCount

    fun progress(current: Int, total: Int): Float =
        if (total <= 0) 0f else (current.toFloat() / total).coerceIn(0f, 1f)

    fun withStatus(t: TrackingEntity, status: ReadingStatus, totalChapters: Int, now: Long): TrackingEntity {
        if (t.status == status) return t
        val base = t.copy(status = status, updatedAt = now)
        return when (status) {
            ReadingStatus.READING -> base.copy(startedAt = t.startedAt ?: now, finishedAt = null)
            ReadingStatus.COMPLETED -> base.copy(
                startedAt = t.startedAt ?: now,
                finishedAt = now,
                currentChapter = if (totalChapters > 0) totalChapters else t.currentChapter,
            )
            ReadingStatus.PLAN -> base.copy(finishedAt = null)
            ReadingStatus.ON_HOLD, ReadingStatus.DROPPED -> base
        }
    }

    /** Sets the chapter (kept within 0..total). Moving past 0 on a "Plan to read" book starts it. */
    fun withChapter(t: TrackingEntity, chapter: Int, totalChapters: Int, now: Long): TrackingEntity {
        val max = if (totalChapters > 0) totalChapters else Int.MAX_VALUE
        val c = chapter.coerceIn(0, max)
        if (c == t.currentChapter) return t
        val moved = t.copy(currentChapter = c, updatedAt = now)
        return if (t.status == ReadingStatus.PLAN && c > 0) withStatus(moved, ReadingStatus.READING, totalChapters, now) else moved
    }

    /** Your own chapter total (null or 0 = go back to the EPUB's count). Progress is kept within it. */
    fun withTotalChapters(t: TrackingEntity, total: Int?, now: Long): TrackingEntity {
        val clean = total?.takeIf { it > 0 }
        return t.copy(
            totalChapters = clean,
            currentChapter = if (clean != null) t.currentChapter.coerceAtMost(clean) else t.currentChapter,
            updatedAt = now,
        )
    }

    /** Tapping the current star again clears the rating. */
    fun withRating(t: TrackingEntity, stars: Int, now: Long): TrackingEntity {
        val next = if (stars == t.rating) 0 else stars.coerceIn(0, 5)
        return t.copy(rating = next, updatedAt = now)
    }
}
