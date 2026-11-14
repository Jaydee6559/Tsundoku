package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryTest {

    private var nextId = 1L

    private fun book(
        title: String,
        authors: String? = null,
        series: String? = null,
        index: Double? = null,
        size: Long = 1000,
        addedAt: Long = 0,
        broken: Boolean = false,
    ) = BookEntity(
        id = nextId++,
        path = "/b/$title.epub",
        fileName = "$title.epub",
        folder = "Download",
        sizeBytes = size,
        lastModified = 0,
        fingerprint = "fp-$title",
        title = title,
        authors = authors,
        seriesName = series,
        seriesIndex = index,
        language = null,
        publisher = null,
        description = null,
        spineCount = 0,
        coverThumbPath = null,
        parseError = if (broken) "bad" else null,
        addedAt = addedAt,
        updatedAt = 0,
    )

    private fun titles(list: List<BookEntity>) = list.map { it.title }

    @Test
    fun `search matches title, author and series, all words required`() {
        val books = listOf(
            book("A Thousand Lanterns", authors = "Mei Arakawa", series = "Lantern Saga"),
            book("Red Lantern Street", authors = "Yuki Sendo"),
            book("Starfall", authors = "K. Ito"),
        )
        assertEquals(2, LibraryQuery.apply(books, "lantern", LibrarySort.TITLE, LibraryFilter.ALL).size)
        assertEquals(listOf("A Thousand Lanterns"), titles(LibraryQuery.apply(books, "arakawa lantern", LibrarySort.TITLE, LibraryFilter.ALL)))
        assertEquals(listOf("A Thousand Lanterns"), titles(LibraryQuery.apply(books, "saga", LibrarySort.TITLE, LibraryFilter.ALL)))
        assertTrue(LibraryQuery.apply(books, "nothing-like-this", LibrarySort.TITLE, LibraryFilter.ALL).isEmpty())
    }

    @Test
    fun `search ignores case, accents and punctuation`() {
        val books = listOf(book("Café: Re-Zero!"))
        assertEquals(1, LibraryQuery.apply(books, "cafe re zero", LibrarySort.TITLE, LibraryFilter.ALL).size)
        assertEquals(1, LibraryQuery.apply(books, "  CAFÉ  ", LibrarySort.TITLE, LibraryFilter.ALL).size)
    }

    @Test
    fun `filters`() {
        val books = listOf(
            book("One", series = "S", index = 1.0),
            book("Two"),
            book("Three", broken = true),
        )
        assertEquals(listOf("One"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.IN_SERIES)))
        assertEquals(listOf("Three", "Two"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.STANDALONE)))
        assertEquals(listOf("Three"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.BROKEN)))
        val counts = LibraryQuery.counts(books)
        assertEquals(3, counts[LibraryFilter.ALL])
        assertEquals(1, counts[LibraryFilter.IN_SERIES])
        assertEquals(2, counts[LibraryFilter.STANDALONE])
        assertEquals(1, counts[LibraryFilter.BROKEN])
    }

    @Test
    fun `title sort ignores leading articles and orders numbers naturally`() {
        val books = listOf(book("Vol 10"), book("The Last Alchemist"), book("Vol 2"), book("A Moonlit Night"))
        assertEquals(
            listOf("The Last Alchemist", "A Moonlit Night", "Vol 2", "Vol 10"),
            titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.ALL)),
        )
    }

    @Test
    fun `series sort groups series in volume order, standalone last`() {
        val books = listOf(
            book("Solo"),
            book("B3", series = "Beta", index = 3.0),
            book("A2", series = "Alpha", index = 2.0),
            book("B1", series = "Beta", index = 1.0),
            book("A1", series = "Alpha", index = 1.0),
        )
        assertEquals(
            listOf("A1", "A2", "B1", "B3", "Solo"),
            titles(LibraryQuery.apply(books, "", LibrarySort.SERIES, LibraryFilter.ALL)),
        )
    }

    @Test
    fun `recent, size and author sorts`() {
        val books = listOf(
            book("Old", authors = "Zed", size = 500, addedAt = 1),
            book("New", authors = "Amy", size = 100, addedAt = 3),
            book("Mid", authors = null, size = 900, addedAt = 2),
        )
        assertEquals(listOf("New", "Mid", "Old"), titles(LibraryQuery.apply(books, "", LibrarySort.RECENT, LibraryFilter.ALL)))
        assertEquals(listOf("Mid", "Old", "New"), titles(LibraryQuery.apply(books, "", LibrarySort.SIZE, LibraryFilter.ALL)))
        assertEquals(listOf("New", "Old", "Mid"), titles(LibraryQuery.apply(books, "", LibrarySort.AUTHOR, LibraryFilter.ALL)))
    }

    @Test
    fun `natural order`() {
        val sorted = listOf("vol 10", "vol 2", "vol 1", "vol 002b").sortedWith(NaturalOrder)
        assertEquals(listOf("vol 1", "vol 2", "vol 002b", "vol 10"), sorted)
    }

    @Test
    fun `status filters use tracking, untracked books count as plan to read`() {
        val a = book("A")
        val b = book("B")
        val c = book("C")
        val statuses = mapOf(a.id to ReadingStatus.READING, b.id to ReadingStatus.COMPLETED)
        val statusOf: (Long) -> ReadingStatus = { statuses[it] ?: ReadingStatus.PLAN }
        val books = listOf(a, b, c)
        assertEquals(listOf("A"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.READING, statusOf)))
        assertEquals(listOf("B"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.COMPLETED, statusOf)))
        assertEquals(listOf("C"), titles(LibraryQuery.apply(books, "", LibrarySort.TITLE, LibraryFilter.PLAN, statusOf)))
        val counts = LibraryQuery.counts(books, statusOf)
        assertEquals(1, counts[LibraryFilter.READING])
        assertEquals(0, counts[LibraryFilter.DROPPED])
    }
}
