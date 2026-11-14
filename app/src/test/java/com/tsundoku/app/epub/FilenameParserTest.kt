package com.tsundoku.app.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilenameParserTest {

    private fun check(fileName: String, series: String?, volume: Double?) {
        val info = FilenameParser.parse(fileName)
        assertEquals("series for '$fileName'", series, info.seriesName)
        if (volume == null) assertNull(info.volume) else assertEquals(volume, info.volume!!, 0.0)
    }

    @Test fun `vol keyword`() = check("Lantern Saga Vol 03.epub", "Lantern Saga", 3.0)
    @Test fun `vol with dot`() = check("Overlord vol.12.epub", "Overlord", 12.0)
    @Test fun `v prefix with group tag and year`() = check("[TL Group] Lantern Saga - v02 (2021).epub", "Lantern Saga", 2.0)
    @Test fun `volume word with underscores`() = check("lantern_saga_volume_4.epub", "lantern saga", 4.0)
    @Test fun `decimal volume`() = check("Lantern Saga Volume 3.5.epub", "Lantern Saga", 3.5)
    @Test fun `book keyword`() = check("Saltwind Book 2.epub", "Saltwind", 2.0)
    @Test fun `part keyword`() = check("Sword Saint Part 7.epub", "Sword Saint", 7.0)
    @Test fun `dash number`() = check("Starfall - 05.epub", "Starfall", 5.0)
    @Test fun `bare small number`() = check("Book of Ash 2.epub", "Book of Ash", 2.0)
    @Test fun `big number is a title not a volume`() = check("Fahrenheit 451.epub", null, null)
    @Test fun `plain title`() = check("The Last Alchemist.epub", null, null)
    @Test fun `words starting with v are not volumes`() = check("Vengeance of the Valkyrie.epub", null, null)
    @Test fun `part inside a word is not a volume`() = check("Partners in Crime.epub", null, null)

    @Test
    fun `clean makes readable titles`() {
        assertEquals("The Last Alchemist", FilenameParser.clean("The_Last_Alchemist.epub"))
        assertEquals("Lantern Saga - v02", FilenameParser.clean("[Group] Lantern Saga - v02 (2021) (Digital).EPUB"))
        assertEquals("Moonlit", FilenameParser.clean("/storage/emulated/0/Download/Moonlit.epub"))
    }
}
