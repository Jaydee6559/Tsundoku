package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTagFilterTest {

    private var nextId = 1L

    private fun book(subjects: String?) = BookEntity(
        id = nextId++, path = "/b/$nextId.epub", fileName = "$nextId.epub", folder = "Download", sizeBytes = 1,
        lastModified = 0, fingerprint = "fp$nextId", title = "T$nextId", authors = null, seriesName = null,
        seriesIndex = null, language = null, publisher = null, description = null, spineCount = 0,
        coverThumbPath = null, parseError = null, addedAt = 0, updatedAt = 0, subjects = subjects,
    )

    @Test
    fun `counts merge epub tags and your tags, ignoring letter case`() {
        val a = book("Fantasy\nRomance")
        val b = book("fantasy")
        val c = book(null)
        val mine = mapOf(c.id to listOf("fantasy", "favourite"))
        val counts = LibraryQuery.tagCounts(listOf(a, b, c)) { mine[it].orEmpty() }
        assertEquals(listOf("fantasy" to 3, "favourite" to 1, "Romance" to 1), counts)
    }

    @Test
    fun `a tag on both the epub and your list counts once per book`() {
        val a = book("Isekai")
        val counts = LibraryQuery.tagCounts(listOf(a)) { listOf("isekai") }
        assertEquals(listOf("Isekai" to 1), counts)
    }

    @Test
    fun `filtering by tag uses both sources`() {
        val a = book("Isekai")
        val b = book("")
        val c = book(null)
        val mine = mapOf(b.id to listOf("isekai"))
        val result = LibraryQuery.withTag(listOf(a, b, c), "ISEKAI") { mine[it].orEmpty() }
        assertEquals(listOf(a.id, b.id), result.map { it.id })
        assertEquals(3, LibraryQuery.withTag(listOf(a, b, c), null) { emptyList() }.size)
    }

    @Test
    fun `search finds your own tags too`() {
        val a = book(null)
        val found = LibraryQuery.apply(listOf(a), "favourite", LibrarySort.TITLE, LibraryFilter.ALL, userTagsOf = { listOf("favourite") })
        assertEquals(1, found.size)
    }
}
