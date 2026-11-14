package com.tsundoku.app.epub

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Reads EPUB metadata quickly and safely.
 *
 * Speed: an EPUB is a zip. [ZipFile] reads the zip's index at the end of the file and jumps
 * straight to the 2–3 small entries we need (container.xml, the OPF, optionally the cover),
 * so a book takes milliseconds no matter how big it is.
 *
 * Safety: files are only read, never written. Every entry read is size-capped (zip bombs),
 * and XML is parsed with [SafeXml] (no external entities).
 */
object EpubParser {

    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val MAX_XML_BYTES = 2L * 1024 * 1024        // 2 MB is far above any real OPF
    const val MAX_COVER_BYTES = 15L * 1024 * 1024             // 15 MB

    private val IMAGE_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")

    fun parse(file: File): EpubResult = try {
        openZip(file).use { zip -> EpubResult.Success(readMetadata(zip)) }
    } catch (e: EpubException) {
        EpubResult.Failure(e.message ?: "Unreadable EPUB")
    } catch (e: ZipException) {
        EpubResult.Failure("Not a valid EPUB (zip) file")
    } catch (e: IOException) {
        EpubResult.Failure("Couldn't read file: ${e.javaClass.simpleName}")
    } catch (e: org.xml.sax.SAXException) {
        EpubResult.Failure("Broken metadata (XML)")
    } catch (e: RuntimeException) {
        // Last line of defense: a weird file must never crash the scanner.
        EpubResult.Failure("Unexpected error: ${e.javaClass.simpleName}")
    }

    /** Returns the cover image bytes, or null if there is none / it is too large / unreadable. */
    fun readCover(file: File, coverPath: String): ByteArray? = runCatching {
        openZip(file).use { zip ->
            val entry = zip.findEntry(coverPath) ?: return@use null
            zip.getInputStream(entry).use { it.readCapped(MAX_COVER_BYTES) }
        }
    }.getOrNull()

    // ------------------------------------------------------------------ internals

    private fun readMetadata(zip: ZipFile): EpubMetadata {
        val containerEntry = zip.findEntry(CONTAINER_PATH) ?: throw EpubException("Missing META-INF/container.xml")
        val container = ContainerHandler().also { SafeXml.parse(zip.readEntry(containerEntry), it) }
        val opfPath = container.opfPath?.let(::decodePath) ?: throw EpubException("No package file listed")
        val opfEntry = zip.findEntry(opfPath) ?: throw EpubException("Package file not found")

        val opf = OpfHandler().also { SafeXml.parse(zip.readEntry(opfEntry), it) }
        val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")

        val (seriesName, seriesIndex) = findSeries(opf)
        return EpubMetadata(
            title = opf.titles.firstOrNull(),
            authors = opf.creators.distinct(),
            language = opf.language,
            publisher = opf.publisher,
            identifier = opf.identifier,
            description = opf.description,
            seriesName = seriesName,
            seriesIndex = seriesIndex,
            coverPath = findCover(opf)?.let { href -> resolve(opfDir, href) }?.takeIf { zip.findEntry(it) != null },
            spineCount = opf.spineCount,
            epubVersion = opf.version,
        )
    }

    private fun findSeries(opf: OpfHandler): Pair<String?, Double?> {
        opf.calibreSeries?.cleanText()?.takeIf { it.isNotEmpty() }?.let { name ->
            return name to opf.calibreSeriesIndex?.trim()?.toDoubleOrNull()
        }
        val (id, name) = opf.collections.entries.firstOrNull()?.toPair() ?: return null to null
        return name to opf.collectionPositions[id]?.trim()?.toDoubleOrNull()
    }

    /** Finds the cover image href, trying EPUB 3, then EPUB 2, then common naming. */
    private fun findCover(opf: OpfHandler): String? {
        val images = opf.manifest.filter { it.mediaType.lowercase() in IMAGE_TYPES }
        images.firstOrNull { "cover-image" in it.properties.split(' ') }?.let { return it.href }
        opf.coverId?.let { id -> opf.manifest.firstOrNull { it.id == id && it.mediaType.lowercase() in IMAGE_TYPES } }
            ?.let { return it.href }
        images.firstOrNull { it.id.contains("cover", ignoreCase = true) }?.let { return it.href }
        images.firstOrNull { it.href.contains("cover", ignoreCase = true) }?.let { return it.href }
        return null
    }

    private fun openZip(file: File): ZipFile = try {
        ZipFile(file, Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        // Entry names that aren't valid UTF-8: fall back to the old zip default charset.
        ZipFile(file, charset("IBM437"))
    }

    private fun ZipFile.readEntry(entry: ZipEntry): ByteArray =
        getInputStream(entry).use { it.readCapped(MAX_XML_BYTES) }
            ?: throw EpubException("Metadata file is too large")

    /** Exact lookup first, then case-insensitive (some EPUBs get the case wrong). */
    private fun ZipFile.findEntry(path: String): ZipEntry? {
        getEntry(path)?.let { return it }
        val target = path.lowercase()
        return entries().asSequence().firstOrNull { !it.isDirectory && it.name.lowercase() == target }
    }

    /** Reads at most [max] bytes; returns null if the stream is longer (protects against zip bombs). */
    private fun InputStream.readCapped(max: Long): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            total += n
            if (total > max) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    /** Resolves an href from the OPF folder into a zip path, handling "../", "#fragments" and %20. */
    internal fun resolve(baseDir: String, href: String): String {
        val clean = decodePath(href.substringBefore('#'))
        val parts = ArrayDeque<String>()
        if (!clean.startsWith("/")) baseDir.split('/').filter { it.isNotEmpty() }.forEach(parts::addLast)
        for (segment in clean.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> parts.removeLastOrNull()
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }

    private fun decodePath(path: String): String = runCatching {
        URLDecoder.decode(path.replace("+", "%2B"), "UTF-8")
    }.getOrDefault(path)

    private class EpubException(message: String) : Exception(message)
}
