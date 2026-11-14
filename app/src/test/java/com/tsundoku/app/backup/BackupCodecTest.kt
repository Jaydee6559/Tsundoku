package com.tsundoku.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupData(
        version = BackupCodec.VERSION,
        exportedAt = 123L,
        books = listOf(
            BookBackup(
                fingerprint = "100-abc",
                path = "/s/Download/a.epub",
                title = "A \"Quoted\" Title",
                status = "READING",
                rating = 4,
                currentChapter = 12,
                totalChapters = 40,
                notes = "Line one\nLine two",
                startedAt = 10L,
                finishedAt = null,
                lastOpenedAt = 99L,
                tags = listOf("fantasy", "slow burn"),
                upNextPosition = 2,
                events = listOf(EventBackup("READING", 10L)),
            ),
        ),
        series = listOf(SeriesBackup("lantern saga", "ONGOING")),
    )

    @Test
    fun `round trip keeps everything`() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun `missing optional fields become null or defaults`() {
        val json = """{"app":"Tsundoku","version":1,"books":[{"fingerprint":"f1"}]}"""
        val book = BackupCodec.decode(json).books.single()
        assertEquals("f1", book.fingerprint)
        assertNull(book.status)
        assertNull(book.totalChapters)
        assertEquals(0, book.rating)
        assertEquals(emptyList<String>(), book.tags)
    }

    @Test
    fun `entries without fingerprint or path are skipped`() {
        val json = """{"app":"Tsundoku","books":[{"title":"x"},{"path":"/s/b.epub"}]}"""
        assertEquals(1, BackupCodec.decode(json).books.size)
    }

    @Test
    fun `other files are rejected`() {
        for (bad in listOf("not json", """{"app":"Other"}""", "[]")) {
            try {
                BackupCodec.decode(bad)
                fail("Expected rejection for: $bad")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }
}
