package com.tsundoku.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrashRulesTest {

    private val day = TimeUnit.DAYS.toMillis(1)

    @Test
    fun `trash name round-trips to the original name`() {
        val name = TrashRules.trashFileName(42, 1_700_000_000_000, "My Book__v2.epub")
        assertEquals("42_1700000000000__My Book__v2.epub", name)
        assertEquals("My Book__v2.epub", TrashRules.originalNameOf(name))
        assertEquals("plain.epub", TrashRules.originalNameOf("plain.epub"))
    }

    @Test
    fun `restore goes back to the original path when it is free`() {
        assertEquals("/s/Download/a.epub", TrashRules.restoreTarget("/s/Download/a.epub") { false })
    }

    @Test
    fun `restore never overwrites, it picks a numbered name`() {
        val taken = setOf("/s/Download/a.epub", "/s/Download/a (1).epub")
        assertEquals("/s/Download/a (2).epub", TrashRules.restoreTarget("/s/Download/a.epub") { it in taken })
    }

    @Test
    fun `restore handles names without an extension`() {
        assertEquals("/s/notes (1)", TrashRules.restoreTarget("/s/notes") { it == "/s/notes" })
    }

    @Test
    fun `days left and expiry`() {
        val t = 1_000_000L
        assertEquals(30, TrashRules.daysLeft(t, t))
        assertEquals(28, TrashRules.daysLeft(t, t + 2 * day + 5))
        assertEquals(0, TrashRules.daysLeft(t, t + 45 * day))
        assertFalse(TrashRules.isExpired(t, t + 29 * day))
        assertTrue(TrashRules.isExpired(t, t + 30 * day))
    }
}
