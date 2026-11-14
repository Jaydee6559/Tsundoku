package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryLocationTest {

    private var nextId = 1L

    private fun book(folder: String) = BookEntity(
        id = nextId++,
        path = "/s/$folder/$nextId.epub",
        fileName = "$nextId.epub",
        folder = folder,
        sizeBytes = 1,
        lastModified = 0,
        fingerprint = "fp$nextId",
        title = "T$nextId",
        authors = null,
        seriesName = null,
        seriesIndex = null,
        language = null,
        publisher = null,
        description = null,
        spineCount = 0,
        coverThumbPath = null,
        parseError = null,
        addedAt = 0,
        updatedAt = 0,
    )

    @Test
    fun `readera copies are recognised wherever the folder sits`() {
        assertEquals("ReadEra", LibraryQuery.locationOf("ReadEra"))
        assertEquals("ReadEra", LibraryQuery.locationOf("Download/ReadEra/Books"))
        assertEquals("ReadEra", LibraryQuery.locationOf("Documents/readera"))
    }

    @Test
    fun `other books use their top folder, root files are Storage`() {
        assertEquals("Download", LibraryQuery.locationOf("Download/Novels/Fantasy"))
        assertEquals("Telegram", LibraryQuery.locationOf("Telegram"))
        assertEquals("Storage", LibraryQuery.locationOf(""))
    }

    @Test
    fun `locations are counted and filtered`() {
        val books = listOf(book("Download"), book("Download/Novels"), book("Download/ReadEra"), book("Telegram"))
        assertEquals(listOf("Download" to 2, "ReadEra" to 1, "Telegram" to 1), LibraryQuery.locations(books))
        assertEquals(1, LibraryQuery.inLocation(books, "ReadEra").size)
        assertEquals(2, LibraryQuery.inLocation(books, "Download").size)
        assertEquals(4, LibraryQuery.inLocation(books, null).size)
    }
}
