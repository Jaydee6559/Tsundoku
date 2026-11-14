package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import java.text.Normalizer

enum class LibraryViewMode { GRID, LIST }

enum class LibrarySort { RECENT, TITLE, AUTHOR, SERIES, SIZE }

/** Filter chips, in display order. Status filters use your tracking; the rest use book data. */
enum class LibraryFilter { ALL, READING, PLAN, COMPLETED, ON_HOLD, DROPPED, IN_SERIES, STANDALONE, BROKEN }

/**
 * Search, filter and sort, done in memory. For a library of hundreds (even a few
 * thousand) books this takes about a millisecond, so every keystroke feels instant.
 * Pure Kotlin: fully unit-tested.
 */
object LibraryQuery {

    fun apply(
        books: List<BookEntity>,
        query: String,
        sort: LibrarySort,
        filter: LibraryFilter,
        statusOf: (Long) -> ReadingStatus = { ReadingStatus.PLAN },
    ): List<BookEntity> {
        val tokens = normalize(query).split(' ').filter { it.isNotEmpty() }
        return books
            .asSequence()
            .filter { matchesFilter(it, filter, statusOf) }
            .filter { tokens.isEmpty() || matchesSearch(it, tokens) }
            .sortedWith(comparator(sort))
            .toList()
    }

    /** How many books each filter chip would show (ignores the search text). */
    fun counts(
        books: List<BookEntity>,
        statusOf: (Long) -> ReadingStatus = { ReadingStatus.PLAN },
    ): Map<LibraryFilter, Int> =
        LibraryFilter.entries.associateWith { f -> books.count { matchesFilter(it, f, statusOf) } }

    private fun matchesFilter(book: BookEntity, filter: LibraryFilter, statusOf: (Long) -> ReadingStatus): Boolean = when (filter) {
        LibraryFilter.ALL -> true
        LibraryFilter.READING -> statusOf(book.id) == ReadingStatus.READING
        LibraryFilter.PLAN -> statusOf(book.id) == ReadingStatus.PLAN
        LibraryFilter.COMPLETED -> statusOf(book.id) == ReadingStatus.COMPLETED
        LibraryFilter.ON_HOLD -> statusOf(book.id) == ReadingStatus.ON_HOLD
        LibraryFilter.DROPPED -> statusOf(book.id) == ReadingStatus.DROPPED
        LibraryFilter.IN_SERIES -> book.seriesName != null
        LibraryFilter.STANDALONE -> book.seriesName == null
        LibraryFilter.BROKEN -> book.parseError != null
    }

    /** Every word typed must appear somewhere in title, author, series or file name. */
    private fun matchesSearch(book: BookEntity, tokens: List<String>): Boolean {
        val haystack = normalize(
            listOfNotNull(book.title, book.authors, book.seriesName, book.fileName).joinToString(" "),
        )
        return tokens.all { it in haystack }
    }

    /** Lowercase, accents removed ("Café" -> "cafe"), punctuation as spaces. */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
            .replace(NON_WORD, " ")
            .trim()

    private fun comparator(sort: LibrarySort): Comparator<BookEntity> {
        val byTitle = Comparator<BookEntity> { a, b -> NaturalOrder.compare(sortTitle(a.title), sortTitle(b.title)) }
        val bySeries = compareBy<BookEntity, String?>(nullsLast(NaturalOrder)) { it.seriesName?.lowercase() }
            .thenBy(nullsLast()) { it.seriesIndex }
        return when (sort) {
            LibrarySort.RECENT -> compareByDescending<BookEntity> { it.addedAt }.then(byTitle)
            LibrarySort.TITLE -> byTitle
            LibrarySort.AUTHOR -> compareBy<BookEntity, String?>(nullsLast(NaturalOrder)) { it.authors?.lowercase() }
                .then(bySeries)
                .then(byTitle)
            LibrarySort.SERIES -> bySeries.then(byTitle)
            LibrarySort.SIZE -> compareByDescending<BookEntity> { it.sizeBytes }.then(byTitle)
        }
    }

    /** "The Last Alchemist" sorts under L. */
    private fun sortTitle(title: String): String {
        val t = title.lowercase().trim()
        for (article in ARTICLES) if (t.startsWith(article)) return t.removePrefix(article)
        return t
    }

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    private val ARTICLES = listOf("the ", "a ", "an ")
}

/**
 * "Vol 2" before "Vol 10": compares runs of digits as numbers, everything else as text.
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isDigit()) i++
                while (j < b.length && b[j].isDigit()) j++
                val numA = a.substring(startA, i).trimStart('0')
                val numB = b.substring(startB, j).trimStart('0')
                if (numA.length != numB.length) return numA.length - numB.length
                val cmp = numA.compareTo(numB)
                if (cmp != 0) return cmp
            } else {
                val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (cmp != 0) return cmp
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
