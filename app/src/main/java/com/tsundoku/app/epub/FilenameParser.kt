package com.tsundoku.app.epub

/** What we can guess from a file name when the EPUB's own metadata is missing. */
data class FilenameInfo(
    /** A readable title made from the file name (always present). */
    val title: String,
    val seriesName: String?,
    val volume: Double?,
)

/**
 * Guesses title / series / volume from names like:
 *  "Lantern Saga Vol 03.epub", "[Group] Lantern Saga - v02 (2021).epub",
 *  "lantern_saga_volume_4.epub", "Lantern Saga - 05.epub", "Lantern Saga Book 2.epub".
 */
object FilenameParser {

    private val EXTENSION = Regex("\\.epub$", RegexOption.IGNORE_CASE)
    // Every bracket escaped: Android's regex engine (ICU) is stricter than desktop Java's.
    private val BRACKETS = Regex("\\[[^\\]]*\\]|\\{[^\\}]*\\}")
    private val TRAILING_PARENS = Regex("\\s*\\([^)]*\\)\\s*$")
    private val SEPARATORS = " -–—:,._#"

    /** "Title Vol 3", "Title v03", "Title Volume 3.5", "Title Book 2", "Title Part 4" */
    private val KEYWORD_VOLUME = Regex(
        "^(.*?)[\\s\\-–—:,]*\\b(?:volume|vol|v|book|bk|part|pt)\\.?\\s*(\\d{1,4}(?:\\.\\d+)?)\\b.*$",
        RegexOption.IGNORE_CASE,
    )

    /** "Title - 05" */
    private val DASH_NUMBER = Regex("^(.*?)\\s+[-–—]\\s+(\\d{1,4}(?:\\.\\d+)?)$")

    /** "Title 05" (only 1–2 digits, so "Fahrenheit 451" stays a title) */
    private val BARE_NUMBER = Regex("^(.*?)\\s+#?(\\d{1,2})$")

    fun parse(fileName: String): FilenameInfo {
        val title = clean(fileName)
        for (pattern in listOf(KEYWORD_VOLUME, DASH_NUMBER, BARE_NUMBER)) {
            val match = pattern.matchEntire(title) ?: continue
            val series = match.groupValues[1].trim { it in SEPARATORS || it.isWhitespace() }
            val volume = match.groupValues[2].toDoubleOrNull()
            if (series.isNotEmpty() && volume != null) {
                return FilenameInfo(title = title, seriesName = series, volume = volume)
            }
        }
        return FilenameInfo(title = title, seriesName = null, volume = null)
    }

    /** Removes extension, [tags], trailing (year/notes), underscores and extra spaces. */
    fun clean(fileName: String): String {
        var s = fileName.substringAfterLast('/').substringAfterLast('\\')
        s = EXTENSION.replace(s, "")
        s = s.replace('_', ' ')
        s = BRACKETS.replace(s, " ")
        repeat(3) { s = TRAILING_PARENS.replace(s, "") }
        s = s.cleanText().trim { it in SEPARATORS || it.isWhitespace() }
        return s.ifEmpty { fileName }
    }
}
