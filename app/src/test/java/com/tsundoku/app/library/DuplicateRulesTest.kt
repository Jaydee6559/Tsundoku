package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateRulesTest {

    private var nextId = 1L

    private fun book(title: String, fp: String, author: String? = "Ann", addedAt: Long = nextId) = BookEntity(
        id = nextId++, path = "/b/$nextId.epub", fileName = "$nextId.epub", folder = "Download", sizeBytes = 1,
        lastModified = 0, fingerprint = fp, title = title, authors = author, seriesName = null, seriesIndex = null,
        language = null, publisher = null, description = null, spineCount = 0, coverThumbPath = null,
        parseError = null, addedAt = addedAt, updatedAt = 0,
    )

    @Test
    fun `exact copies are grouped by fingerprint, oldest first`() {
        val original = book("Lanterns", "fp1", addedAt = 1)
        val readEraCopy = book("Lanterns", "fp1", addedAt = 5)
        val other = book("Starfall", "fp2")
        val groups = DuplicateRules.find(listOf(readEraCopy, other, original))
        assertEquals(1, groups.size)
        assertTrue(groups[0].exact)
        assertEquals(listOf(original.id, readEraCopy.id), groups[0].books.map { it.id })
    }

    @Test
    fun `same title and author in different files is a possible duplicate`() {
        val a = book("The Last Alchemist", "fpA")
        val b = book("the last alchemist!", "fpB")
        val c = book("The Last Alchemist", "fpC", author = "Someone Else")
        val groups = DuplicateRules.find(listOf(a, b, c))
        assertEquals(1, groups.size)
        assertFalse(groups[0].exact)
        assertEquals(setOf(a.id, b.id), groups[0].books.map { it.id }.toSet())
    }

    @Test
    fun `a book in an exact group is not listed again as similar`() {
        val a = book("Lanterns", "fp1")
        val b = book("Lanterns", "fp1")
        val c = book("Lanterns", "fp9")
        val groups = DuplicateRules.find(listOf(a, b, c))
        assertEquals(1, groups.size) // c alone is left; no second group
        assertTrue(groups[0].exact)
    }

    @Test
    fun `very short titles are not matched`() {
        assertEquals(emptyList<DuplicateGroup>(), DuplicateRules.find(listOf(book("1", "x"), book("1", "y"))))
    }
}
