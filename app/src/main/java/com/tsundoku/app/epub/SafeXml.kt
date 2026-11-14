package com.tsundoku.app.epub

import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * SAX parsing hardened against malicious files (an EPUB is untrusted input):
 *  - external entities / DTDs are never fetched (blocks XXE: reading local files via the XML),
 *  - secure-processing limits entity expansion ("billion laughs"),
 *  - input size is capped by the caller.
 * Works on both Android and the JVM (unit tests), unlike Android's XmlPullParser.
 */
internal object SafeXml {

    private val factory: SAXParserFactory by lazy {
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            trySet(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            trySet("http://xml.org/sax/features/external-general-entities", false)
            trySet("http://xml.org/sax/features/external-parameter-entities", false)
            trySet("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
    }

    private fun SAXParserFactory.trySet(feature: String, value: Boolean) {
        // Not every platform supports every feature; the entity resolver below is the real guard.
        runCatching { setFeature(feature, value) }
    }

    fun parse(bytes: ByteArray, handler: ContentHandler) {
        val reader = factory.newSAXParser().xmlReader
        // Any external entity or DTD resolves to empty text: nothing outside the file is ever read.
        reader.setEntityResolver { _, _ -> InputSource(StringReader("")) }
        reader.contentHandler = handler
        reader.parse(InputSource(ByteArrayInputStream(bytes)))
    }
}
