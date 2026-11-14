package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.SeriesStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SeriesGrouperTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val start = 1_000_000_000_000L
    private var nextId = 1L

    private fun book(series: String?, index: Double?, title: String = "B$nextId", addedAt: Long = start, author: String? = "Ann") =
        BookEntity(
            id = nextId++,
            path = "/b/$nextId.epub",
            fileName = "$nextId.epub",
            folder = "Download",
            sizeBytes = 1,
            lastModified = 0,
            fingerprint = "fp$nextId",
            title = title,
            authors = author,
            seriesName = series,
            seriesIndex = index,
            language = null,
            publisher = null,
            description = null,
            spineCount = 0,
            coverThumbPath = null,
            parseError = null,
            addedAt = addedAt,
            updatedAt = 0,
        )

    private val plan: (Long) -> ReadingStatus = { ReadingStatus.PLAN }

    @Test
    fun `groups by series name ignoring case and punctuation, standalone books left out`() {
        val books = listOf(
            book("Lantern Saga", 1.0),
            book("lantern saga", 2.0),
            book("Lantern Saga:", 3.0),
            book(null, null),
        )
        val series = SeriesGrouper.group(books, plan, emptyMap(), now = start)
        assertEquals(1, series.size)
        assertEquals("Lantern Saga", series[0].name)
        assertEquals(3, series[0].volumeCount)
    }

    @Test
    fun `volumes are ordered by number, unnumbered last`() {
        val books = listOf(book("S", 10.0, "Ten"), book("S", null, "Extra"), book("S", 2.0, "Two"))
        val titles = SeriesGrouper.group(books, plan, emptyMap(), start)[0].books.map { it.title }
        assertEquals(listOf("Two", "Ten", "Extra"), titles)
    }

    @Test
    fun `missing volumes`() {
        assertEquals(listOf(3), SeriesGrouper.missingVolumes(listOf(1.0, 2.0, 4.0, 5.0)))
        assertEquals(listOf(1, 2), SeriesGrouper.missingVolumes(listOf(3.0)))
        assertEquals(emptyList<Int>(), SeriesGrouper.missingVolumes(listOf(1.0, 1.5, 2.0)))
        assertEquals(emptyList<Int>(), SeriesGrouper.missingVolumes(listOf(1.0, 2021.0)))
        assertEquals(emptyList<Int>(), SeriesGrouper.missingVolumes(emptyList()))
    }

    @Test
    fun `entries put placeholders where missing volumes belong`() {
        val books = listOf(book("S", 1.0), book("S", 4.0), book("S", 2.0))
        val entries = SeriesGrouper.entries(SeriesGrouper.group(books, plan, emptyMap(), start)[0])
        val shape = entries.map {
            when (it) {
                is SeriesEntry.Book -> "v${it.book.seriesIndex!!.toInt()}"
                is SeriesEntry.Missing -> "missing${it.volume}"
            }
        }
        assertEquals(listOf("v1", "v2", "missing3", "v4"), shape)
    }

    @Test
    fun `read counts use tracking`() {
        val a = book("S", 1.0)
        val b = book("S", 2.0)
        val c = book("S", 3.0)
        val statuses = mapOf(a.id to ReadingStatus.COMPLETED, b.id to ReadingStatus.READING)
        val s = SeriesGrouper.group(listOf(a, b, c), { statuses[it] ?: ReadingStatus.PLAN }, emptyMap(), start)[0]
        assertEquals(1, s.completedCount)
        assertEquals(1, s.readingCount)
    }

    @Test
    fun `first scan books are not new, later arrivals are new for a week`() {
        val old = book("S", 1.0, addedAt = start)
        val fresh = book("S", 2.0, addedAt = start + 10 * day)
        val s = SeriesGrouper.group(listOf(old, fresh), plan, emptyMap(), now = start + 11 * day)[0]
        assertEquals(setOf(fresh.id), s.newBookIds)
        val later = SeriesGrouper.group(listOf(old, fresh), plan, emptyMap(), now = start + 30 * day)[0]
        assertTrue(later.newBookIds.isEmpty())
    }

    @Test
    fun `series status comes from your settings by key`() {
        val books = listOf(book("Lantern Saga", 1.0))
        val key = SeriesGrouper.keyOf("Lantern Saga")
        val s = SeriesGrouper.group(books, plan, mapOf(key to SeriesStatus.HIATUS), start)[0]
        assertEquals(SeriesStatus.HIATUS, s.status)
    }
}
