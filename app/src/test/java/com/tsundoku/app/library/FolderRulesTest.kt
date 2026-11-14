package com.tsundoku.app.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderRulesTest {

    private val excluded = setOf("Download/Manga", "WhatsApp")

    @Test
    fun `excluded folder and everything inside it`() {
        assertTrue(FolderRules.isExcluded("Download/Manga", excluded))
        assertTrue(FolderRules.isExcluded("Download/Manga/One Piece", excluded))
        assertTrue(FolderRules.isExcluded("download/manga", excluded))
        assertTrue(FolderRules.isExcluded("WhatsApp/Media", excluded))
    }

    @Test
    fun `similar names and parents are not excluded`() {
        assertFalse(FolderRules.isExcluded("Download", excluded))
        assertFalse(FolderRules.isExcluded("Download/MangaNovels", excluded))
        assertFalse(FolderRules.isExcluded("", excluded))
        assertFalse(FolderRules.isExcluded("Anything", setOf("")))
    }

    @Test
    fun `folder picker ids`() {
        assertEquals("Download/Novels", FolderRules.relativeFromTreeDocId("primary:Download/Novels"))
        assertEquals("", FolderRules.relativeFromTreeDocId("primary:"))
        assertNull(FolderRules.relativeFromTreeDocId("1A2B-3C4D:Books"))
    }
}
