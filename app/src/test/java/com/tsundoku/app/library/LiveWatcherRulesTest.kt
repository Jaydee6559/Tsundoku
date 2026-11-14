package com.tsundoku.app.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveWatcherRulesTest {

    @Test
    fun `only epub files count, partial downloads and hidden files do not`() {
        assertTrue(WatchRules.isEpubName("Book.epub"))
        assertTrue(WatchRules.isEpubName("BOOK.EPUB"))
        assertFalse(WatchRules.isEpubName("Book.epub.crdownload"))
        assertFalse(WatchRules.isEpubName(".pending-123-Book.epub"))
        assertFalse(WatchRules.isEpubName("cover.jpg"))
    }

    @Test
    fun `hidden folders like the trash are never watched`() {
        val root = "/storage/emulated/0"
        assertTrue(WatchRules.isHiddenPath("$root/.tsundoku/trash", root))
        assertTrue(WatchRules.isHiddenPath("$root/Download/.thumbs", root))
        assertFalse(WatchRules.isHiddenPath("$root/Download/Novels", root))
        assertFalse(WatchRules.isHiddenPath(root, root))
    }
}
