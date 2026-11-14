package com.tsundoku.app.epub

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

private const val DC_NS = "http://purl.org/dc/elements/1.1/"

/** Reads META-INF/container.xml and finds the path of the OPF ("package") file. */
internal class ContainerHandler : DefaultHandler() {
    var opfPath: String? = null
        private set

    override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
        if (opfPath == null && localName.ifEmpty { qName } == "rootfile") {
            opfPath = attrs.getValue("full-path")?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}

internal data class ManifestItem(val id: String, val href: String, val mediaType: String, val properties: String)

/** Reads the OPF file: metadata, manifest (file list) and spine (reading order). */
internal class OpfHandler : DefaultHandler() {
    var version: String? = null
    val titles = mutableListOf<String>()
    val creators = mutableListOf<String>()
    var language: String? = null
    var publisher: String? = null
    var identifier: String? = null
    var description: String? = null

    var calibreSeries: String? = null
    var calibreSeriesIndex: String? = null
    var coverId: String? = null

    /** EPUB 3: <meta property="belongs-to-collection" id="x">Name</meta> */
    val collections = linkedMapOf<String, String>()
    /** EPUB 3: <meta refines="#x" property="group-position">2</meta> */
    val collectionPositions = mutableMapOf<String, String>()

    val manifest = mutableListOf<ManifestItem>()
    var spineCount = 0

    private var inMetadata = false
    private var textTarget: String? = null // which field the current text belongs to
    private var metaProperty: String? = null
    private var metaId: String? = null
    private var metaRefines: String? = null
    private val text = StringBuilder()

    override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
        val name = localName.ifEmpty { qName.substringAfter(':') }
        when (name) {
            "package" -> version = attrs.getValue("version")
            "metadata" -> inMetadata = true
            "item" -> {
                val id = attrs.getValue("id") ?: return
                val href = attrs.getValue("href") ?: return
                manifest += ManifestItem(
                    id = id,
                    href = href,
                    mediaType = attrs.getValue("media-type").orEmpty(),
                    properties = attrs.getValue("properties").orEmpty(),
                )
            }
            "itemref" -> spineCount++
            "meta" -> if (inMetadata) startMeta(attrs)
            else -> if (inMetadata && uri == DC_NS) startText(name)
        }
    }

    private fun startMeta(attrs: Attributes) {
        val metaName = attrs.getValue("name")
        val content = attrs.getValue("content")
        if (metaName != null) {
            // EPUB 2 style: <meta name="..." content="..."/>
            when (metaName) {
                "calibre:series" -> calibreSeries = content
                "calibre:series_index" -> calibreSeriesIndex = content
                "cover" -> coverId = content
            }
            return
        }
        // EPUB 3 style: <meta property="...">text</meta>
        metaProperty = attrs.getValue("property")
        metaId = attrs.getValue("id")
        metaRefines = attrs.getValue("refines")?.removePrefix("#")
        if (metaProperty != null) startText("meta")
    }

    private fun startText(target: String) {
        textTarget = target
        text.setLength(0)
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (textTarget != null && text.length < MAX_TEXT) text.append(ch, start, length)
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        val name = localName.ifEmpty { qName.substringAfter(':') }
        if (name == "metadata") inMetadata = false
        val target = textTarget ?: return
        if (target != name) return
        val value = text.toString().cleanText()
        textTarget = null
        if (value.isEmpty()) return
        when (target) {
            "title" -> titles += value
            "creator" -> creators += value
            "language" -> if (language == null) language = value
            "publisher" -> if (publisher == null) publisher = value
            "identifier" -> if (identifier == null) identifier = value
            "description" -> if (description == null) description = value
            "meta" -> when (metaProperty) {
                "belongs-to-collection" -> metaId?.let { collections[it] = value } ?: run {
                    collections["_${collections.size}"] = value
                }
                "group-position" -> metaRefines?.let { collectionPositions[it] = value }
            }
        }
    }

    private companion object {
        const val MAX_TEXT = 20_000
    }
}

/** Collapses whitespace and trims. */
private val WHITESPACE = Regex("\\s+")

internal fun String.cleanText(): String = replace(WHITESPACE, " ").trim()
