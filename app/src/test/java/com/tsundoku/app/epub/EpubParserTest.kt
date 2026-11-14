package com.tsundoku.app.epub

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EpubParserTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun success(file: File): EpubMetadata = when (val r = EpubParser.parse(file)) {
        is EpubResult.Success -> r.metadata
        is EpubResult.Failure -> throw AssertionError("Expected success but got: ${r.reason}")
    }

    private fun failure(file: File): String = when (val r = EpubParser.parse(file)) {
        is EpubResult.Success -> throw AssertionError("Expected failure but parsed: ${r.metadata}")
        is EpubResult.Failure -> r.reason
    }

    @Test
    fun `reads EPUB 2 metadata, calibre series and meta cover`() {
        val file = TestEpubs.epub(
            tmp.root, "book.epub", "OEBPS/content.opf", TestEpubs.EPUB2_OPF,
            extra = mapOf("OEBPS/images/front.jpg" to TestEpubs.FAKE_JPEG),
        )
        val m = success(file)

        assertEquals("A Thousand Lanterns", m.title) // whitespace collapsed
        assertEquals(listOf("Mei Arakawa", "Second Author"), m.authors) // duplicates removed
        assertEquals("en", m.language)
        assertEquals("Test Press", m.publisher)
        assertEquals("urn:uuid:1234", m.identifier)
        assertEquals("Lantern Saga", m.seriesName)
        assertEquals(2.0, m.seriesIndex!!, 0.0)
        assertEquals("OEBPS/images/front.jpg", m.coverPath)
        assertEquals(3, m.spineCount)
        assertEquals("2.0", m.epubVersion)
        assertEquals(listOf("Fantasy", "Romance", "Slow Burn"), m.subjects) // split, trimmed, no duplicates
    }

    @Test
    fun `subject cleaning caps and splits`() {
        assertEquals(listOf("A", "B", "C"), EpubParser.cleanSubjects(listOf("A; B / C", "a", "")))
        assertEquals(20, EpubParser.cleanSubjects((1..50).map { "tag$it" }).size)
        assertEquals(40, EpubParser.cleanSubjects(listOf("x".repeat(100))).single().length)
    }

    @Test
    fun `reads EPUB 3 collection series and cover-image with relative and encoded href`() {
        val file = TestEpubs.epub(
            tmp.root, "book3.epub", "OPS/Text/package.opf", TestEpubs.EPUB3_OPF,
            extra = mapOf("OPS/Images/Cover Art.png" to TestEpubs.FAKE_JPEG),
        )
        val m = success(file)

        assertEquals("The Ashen Archive", m.title)
        assertEquals(listOf("R. K. Vale"), m.authors)
        assertEquals("Ashen Archive", m.seriesName)
        assertEquals(4.0, m.seriesIndex!!, 0.0)
        assertEquals("OPS/Images/Cover Art.png", m.coverPath)
        assertEquals(2, m.spineCount)
        assertEquals("3.0", m.epubVersion)
    }

    @Test
    fun `readCover returns the image bytes`() {
        val file = TestEpubs.epub(
            tmp.root, "c.epub", "content.opf", TestEpubs.EPUB2_OPF,
            extra = mapOf("images/front.jpg" to TestEpubs.FAKE_JPEG),
        )
        val m = success(file)
        assertEquals("images/front.jpg", m.coverPath)
        assertArrayEquals(TestEpubs.FAKE_JPEG, EpubParser.readCover(file, m.coverPath!!))
    }

    @Test
    fun `cover listed in OPF but missing from zip gives null coverPath`() {
        val file = TestEpubs.epub(tmp.root, "nocover.epub", "content.opf", TestEpubs.EPUB2_OPF)
        assertNull(success(file).coverPath)
    }

    @Test
    fun `missing title and series are null, book still parses`() {
        val opf = """
            <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:language>en</dc:language></metadata>
              <manifest/><spine/>
            </package>
        """.trimIndent()
        val m = success(TestEpubs.epub(tmp.root, "bare.epub", "content.opf", opf))
        assertNull(m.title)
        assertTrue(m.authors.isEmpty())
        assertNull(m.seriesName)
        assertNull(m.coverPath)
        assertEquals(0, m.spineCount)
    }

    @Test
    fun `finds container entry case-insensitively`() {
        val file = TestEpubs.zip(
            tmp.root, "case.epub",
            mapOf(
                "meta-inf/CONTAINER.xml" to TestEpubs.container("content.opf").toByteArray(),
                "content.opf" to TestEpubs.EPUB2_OPF.toByteArray(),
            ),
        )
        assertEquals("A Thousand Lanterns", success(file).title)
    }

    @Test
    fun `not a zip file fails cleanly`() {
        val file = File(tmp.root, "fake.epub").apply { writeText("this is not a zip") }
        assertTrue(failure(file).isNotBlank())
    }

    @Test
    fun `missing container fails cleanly`() {
        val file = TestEpubs.zip(tmp.root, "nocontainer.epub", mapOf("content.opf" to "x".toByteArray()))
        assertEquals("Missing META-INF/container.xml", failure(file))
    }

    @Test
    fun `container pointing to missing OPF fails cleanly`() {
        val file = TestEpubs.zip(
            tmp.root, "noopf.epub",
            mapOf("META-INF/container.xml" to TestEpubs.container("gone.opf").toByteArray()),
        )
        assertEquals("Package file not found", failure(file))
    }

    @Test
    fun `broken OPF XML fails cleanly`() {
        val file = TestEpubs.epub(tmp.root, "broken.epub", "content.opf", "<package><metadata><dc:title>oops")
        assertTrue(failure(file).isNotBlank())
    }

    @Test
    fun `external entities are never read (XXE)`() {
        val secret = File(tmp.root, "secret.txt").apply { writeText("TOP-SECRET-DATA") }
        val opf = """
            <?xml version="1.0"?>
            <!DOCTYPE package [ <!ENTITY xxe SYSTEM "${secret.toURI()}"> ]>
            <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Hi &xxe;</dc:title></metadata>
              <manifest/><spine/>
            </package>
        """.trimIndent()
        val file = TestEpubs.epub(tmp.root, "xxe.epub", "content.opf", opf)
        when (val r = EpubParser.parse(file)) {
            is EpubResult.Success -> assertFalse(r.metadata.title.orEmpty().contains("TOP-SECRET"))
            is EpubResult.Failure -> Unit // refusing the file is also safe
        }
    }

    @Test
    fun `resolve handles parent folders, fragments and absolute paths`() {
        assertEquals("OPS/img/a.png", EpubParser.resolve("OPS/Text", "../img/a.png"))
        assertEquals("OPS/Text/ch.xhtml", EpubParser.resolve("OPS/Text", "ch.xhtml#part2"))
        assertEquals("img/a b.png", EpubParser.resolve("", "img/a%20b.png"))
        assertEquals("root.png", EpubParser.resolve("OPS", "/root.png"))
        assertEquals("a+b.png", EpubParser.resolve("", "a+b.png"))
    }
}
