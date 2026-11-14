package com.tsundoku.app.epub

/** Everything Tsundoku reads from one EPUB. All fields are optional: real-world EPUBs are messy. */
data class EpubMetadata(
    val title: String?,
    val authors: List<String>,
    val language: String?,
    val publisher: String?,
    val identifier: String?,
    val description: String?,
    /** Series from EPUB metadata (Calibre or EPUB 3 collections). */
    val seriesName: String?,
    /** Position in the series, e.g. 2.0 or 2.5. */
    val seriesIndex: Double?,
    /** Path of the cover image inside the zip, if one was found. */
    val coverPath: String?,
    /** Number of reading-order items (roughly the chapter count). */
    val spineCount: Int,
    /** "2.0", "3.0", ... as declared in the OPF. */
    val epubVersion: String?,
    /** Tags / genres from the EPUB (dc:subject), cleaned and de-duplicated. */
    val subjects: List<String> = emptyList(),
)

/** Result of parsing: either metadata, or a short reason why the file couldn't be read. */
sealed interface EpubResult {
    data class Success(val metadata: EpubMetadata) : EpubResult
    data class Failure(val reason: String) : EpubResult
}
