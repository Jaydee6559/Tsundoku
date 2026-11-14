package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.TrackingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsRulesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(utc).apply { clear(); set(year, month - 1, day) }.timeInMillis

    private fun book(id: Long, author: String? = "Ann", subjects: String? = null) = BookEntity(
        id = id, path = "/b/$id.epub", fileName = "$id.epub", folder = "Download", sizeBytes = 1, lastModified = 0,
        fingerprint = "fp$id", title = "T$id", authors = author, seriesName = null, seriesIndex = null, language = null,
        publisher = null, description = null, spineCount = 0, coverThumbPath = null, parseError = null,
        addedAt = 0, updatedAt = 0, subjects = subjects,
    )

    private fun done(id: Long, started: Long?, finished: Long) =
        TrackingRules.default(id, 0).copy(status = ReadingStatus.COMPLETED, startedAt = started, finishedAt = finished)

    @Test
    fun `finished books are counted per month of the chosen year`() {
        val books = (1L..4L).map { book(it) }
        val tracking = listOf(
            done(1, date(2026, 1, 1), date(2026, 1, 11)),
            done(2, date(2026, 3, 1), date(2026, 3, 21)),
            done(3, null, date(2026, 3, 30)),
            done(4, null, date(2025, 12, 31)),
        )
        val y = StatsRules.year(2026, books, tracking, { emptyList() }, utc)
        assertEquals(3, y.finished)
        assertEquals(1, y.perMonth[0])
        assertEquals(2, y.perMonth[2])
        assertEquals(15, y.averageDaysToFinish) // (10 + 20) / 2, book 3 has no start date
        assertEquals(listOf("T3", "T2", "T1"), y.finishedTitles)
    }

    @Test
    fun `top author and tags come from books finished that year`() {
        val books = listOf(book(1, "Ann", "Fantasy"), book(2, "Ann, Bob", "fantasy\nRomance"), book(3, "Cy"))
        val tracking = listOf(
            done(1, null, date(2026, 5, 1)),
            done(2, null, date(2026, 6, 1)),
            done(3, null, date(2026, 7, 1)),
        )
        val y = StatsRules.year(2026, books, tracking, { if (it == 3L) listOf("favourite") else emptyList() }, utc)
        assertEquals("Ann", y.topAuthor)
        assertEquals(listOf("fantasy", "favourite", "romance"), y.topTags)
    }

    @Test
    fun `empty year has no averages`() {
        val y = StatsRules.year(2030, emptyList(), emptyList(), { emptyList() }, utc)
        assertEquals(0, y.finished)
        assertNull(y.averageDaysToFinish)
        assertNull(y.topAuthor)
    }

    @Test
    fun `years list includes the current year and finished years, newest first`() {
        val tracking: List<TrackingEntity> = listOf(done(1, null, date(2024, 2, 2)), done(2, null, date(2026, 1, 1)))
        assertEquals(listOf(2027, 2026, 2024), StatsRules.years(tracking, now = date(2027, 6, 1), zone = utc))
    }
}
