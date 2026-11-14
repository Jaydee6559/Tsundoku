package com.tsundoku.app.library

import com.tsundoku.app.data.db.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TrackingRulesTest {

    private val start = TrackingRules.default(bookId = 1, now = 0)

    @Test
    fun `new book is plan to read with no progress`() {
        assertEquals(ReadingStatus.PLAN, start.status)
        assertEquals(0, start.currentChapter)
        assertEquals(0, start.rating)
    }

    @Test
    fun `reading sets started date once`() {
        val r1 = TrackingRules.withStatus(start, ReadingStatus.READING, 60, now = 100)
        assertEquals(100L, r1.startedAt)
        val held = TrackingRules.withStatus(r1, ReadingStatus.ON_HOLD, 60, now = 200)
        val r2 = TrackingRules.withStatus(held, ReadingStatus.READING, 60, now = 300)
        assertEquals(100L, r2.startedAt) // kept from the first time
    }

    @Test
    fun `completed fills progress and sets finished date`() {
        val reading = TrackingRules.withChapter(start, 10, 60, now = 50)
        val done = TrackingRules.withStatus(reading, ReadingStatus.COMPLETED, 60, now = 99)
        assertEquals(60, done.currentChapter)
        assertEquals(99L, done.finishedAt)
    }

    @Test
    fun `completed with unknown chapter count keeps progress`() {
        val done = TrackingRules.withStatus(start, ReadingStatus.COMPLETED, 0, now = 5)
        assertEquals(0, done.currentChapter)
    }

    @Test
    fun `reading again after completing clears finished date`() {
        val done = TrackingRules.withStatus(start, ReadingStatus.COMPLETED, 10, now = 5)
        val again = TrackingRules.withStatus(done, ReadingStatus.READING, 10, now = 6)
        assertNull(again.finishedAt)
    }

    @Test
    fun `same status changes nothing`() {
        assertSame(start, TrackingRules.withStatus(start, ReadingStatus.PLAN, 10, now = 1))
    }

    @Test
    fun `chapter is kept within 0 and total`() {
        assertEquals(60, TrackingRules.withChapter(start, 999, 60, now = 1).currentChapter)
        assertEquals(0, TrackingRules.withChapter(start, -3, 60, now = 1).currentChapter)
        assertEquals(500, TrackingRules.withChapter(start, 500, 0, now = 1).currentChapter) // unknown total
    }

    @Test
    fun `moving a plan-to-read book forward starts it`() {
        val moved = TrackingRules.withChapter(start, 1, 60, now = 7)
        assertEquals(ReadingStatus.READING, moved.status)
        assertEquals(7L, moved.startedAt)
    }

    @Test
    fun `own chapter total clamps progress and can be reset`() {
        val at50 = TrackingRules.withChapter(start, 50, 60, now = 1)
        val total40 = TrackingRules.withTotalChapters(at50, 40, now = 2)
        assertEquals(40, total40.totalChapters)
        assertEquals(40, total40.currentChapter)
        assertNull(TrackingRules.withTotalChapters(total40, 0, now = 3).totalChapters)
        assertEquals(55, TrackingRules.totalChapters(null, 55))
        assertEquals(40, TrackingRules.totalChapters(total40, 55))
    }

    @Test
    fun `rating toggles off when tapped again`() {
        val four = TrackingRules.withRating(start, 4, now = 1)
        assertEquals(4, four.rating)
        assertEquals(0, TrackingRules.withRating(four, 4, now = 2).rating)
        assertEquals(5, TrackingRules.withRating(four, 9, now = 3).rating)
    }

    @Test
    fun `progress fraction`() {
        assertEquals(0.5f, TrackingRules.progress(30, 60), 0.0001f)
        assertEquals(0f, TrackingRules.progress(5, 0), 0.0001f)
        assertEquals(1f, TrackingRules.progress(80, 60), 0.0001f)
    }
}
