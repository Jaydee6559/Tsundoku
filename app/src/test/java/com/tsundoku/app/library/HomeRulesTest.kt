package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRulesTest {

    private fun book(id: Long) = BookEntity(
        id = id, path = "/b/$id.epub", fileName = "$id.epub", folder = "Download", sizeBytes = 1, lastModified = 0,
        fingerprint = "fp$id", title = "T$id", authors = null, seriesName = null, seriesIndex = null, language = null,
        publisher = null, description = null, spineCount = 0, coverThumbPath = null, parseError = null,
        addedAt = 0, updatedAt = 0,
    )

    private fun track(id: Long, status: ReadingStatus, opened: Long? = null, updated: Long = 0) =
        TrackingRules.default(id, updated).copy(status = status, lastOpenedAt = opened)

    @Test
    fun `continue reading picks the most recently opened reading book`() {
        val books = listOf(book(1), book(2), book(3))
        val tracking = listOf(
            track(1, ReadingStatus.READING, opened = 100),
            track(2, ReadingStatus.READING, opened = 300),
            track(3, ReadingStatus.COMPLETED, opened = 900),
        )
        assertEquals(2L, HomeRules.continueReading(books, tracking)!!.first.id)
    }

    @Test
    fun `continue reading falls back to last update and ignores books not in the library`() {
        val books = listOf(book(1))
        val tracking = listOf(track(1, ReadingStatus.READING, updated = 50), track(9, ReadingStatus.READING, opened = 999))
        assertEquals(1L, HomeRules.continueReading(books, tracking)!!.first.id)
        assertNull(HomeRules.continueReading(emptyList(), tracking))
    }

    @Test
    fun `counts treat untracked books as plan to read`() {
        val c = HomeRules.counts(listOf(book(1), book(2), book(3)), mapOf(1L to ReadingStatus.READING, 2L to ReadingStatus.COMPLETED))
        assertEquals(HomeRules.Counts(reading = 1, plan = 1, completed = 1), c)
    }

    @Test
    fun `queue moves stay inside the list`() {
        val order = listOf(1L, 2L, 3L)
        assertEquals(listOf(2L, 1L, 3L), HomeRules.move(order, 2, -1))
        assertEquals(listOf(1L, 3L, 2L), HomeRules.move(order, 2, +1))
        assertEquals(order, HomeRules.move(order, 1, -1))
        assertEquals(order, HomeRules.move(order, 3, +1))
        assertEquals(order, HomeRules.move(order, 42, +1))
    }

    @Test
    fun `shared file names are made safe`() {
        assertEquals("Lantern Saga v2.epub", HomeRules.safeFileName("Lantern Saga v2.epub"))
        assertEquals("evil.epub", HomeRules.safeFileName("../../evil.epub"))
        assertEquals("a_b_c.epub", HomeRules.safeFileName("a:b?c"))
        assertEquals("hidden.epub", HomeRules.safeFileName(".hidden.epub"))
        assertEquals("shared-book.epub", HomeRules.safeFileName(null))
        assertEquals(125, HomeRules.safeFileName("x".repeat(500)).length)
    }
}
