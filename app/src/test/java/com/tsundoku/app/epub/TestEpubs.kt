package com.tsundoku.app.epub

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds tiny fake EPUB files for tests, so no real (copyrighted) books are needed in the repo.
 */
object TestEpubs {

    fun container(opfPath: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    /** Writes a zip with the given entries (path -> content). */
    fun zip(dir: File, name: String, entries: Map<String, ByteArray>): File {
        val file = File(dir, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            // The EPUB spec puts "mimetype" first.
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()
            for ((path, bytes) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    fun epub(dir: File, name: String, opfPath: String, opf: String, extra: Map<String, ByteArray> = emptyMap()): File =
        zip(
            dir,
            name,
            mapOf(
                "META-INF/container.xml" to container(opfPath).toByteArray(),
                opfPath to opf.toByteArray(),
            ) + extra,
        )

    /** A fake "image": parsers only need the bytes to exist. */
    val FAKE_JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4)

    /** EPUB 2 book with Calibre series data and a <meta name="cover"> cover. */
    val EPUB2_OPF = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="uid">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
            <dc:title>A Thousand   Lanterns</dc:title>
            <dc:creator opf:role="aut">Mei Arakawa</dc:creator>
            <dc:creator opf:role="aut">Second Author</dc:creator>
            <dc:creator opf:role="aut">Mei Arakawa</dc:creator>
            <dc:language>en</dc:language>
            <dc:publisher>Test Press</dc:publisher>
            <dc:subject>Fantasy, Romance</dc:subject>
            <dc:subject>fantasy</dc:subject>
            <dc:subject>  Slow   Burn  </dc:subject>
            <dc:identifier id="uid">urn:uuid:1234</dc:identifier>
            <meta name="calibre:series" content="Lantern Saga"/>
            <meta name="calibre:series_index" content="2.0"/>
            <meta name="cover" content="cover-img"/>
          </metadata>
          <manifest>
            <item id="cover-img" href="images/front.jpg" media-type="image/jpeg"/>
            <item id="c1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/>
            <item id="c2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/>
            <item id="c3" href="text/ch3.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine>
            <itemref idref="c1"/>
            <itemref idref="c2"/>
            <itemref idref="c3"/>
          </spine>
        </package>
    """.trimIndent()

    /** EPUB 3 book: collection-based series, cover-image property, OPF in a subfolder, %20 in href. */
    val EPUB3_OPF = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:identifier id="pub-id">isbn:000</dc:identifier>
            <dc:title>The Ashen Archive</dc:title>
            <dc:creator id="a1">R. K. Vale</dc:creator>
            <dc:language>ja</dc:language>
            <meta property="dcterms:modified">2026-01-01T00:00:00Z</meta>
            <meta property="belongs-to-collection" id="c01">Ashen Archive</meta>
            <meta refines="#c01" property="collection-type">series</meta>
            <meta refines="#c01" property="group-position">4</meta>
          </metadata>
          <manifest>
            <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
            <item id="img" href="../Images/Cover%20Art.png" media-type="image/png" properties="cover-image"/>
            <item id="ch" href="ch.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine><itemref idref="nav"/><itemref idref="ch"/></spine>
        </package>
    """.trimIndent()
}
