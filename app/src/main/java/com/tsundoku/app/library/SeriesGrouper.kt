package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.SeriesStatus
import java.util.concurrent.TimeUnit

/** One series, built live from the books that carry its name. */
data class SeriesSummary(
    val key: String,
    /** The most common spelling of the name among its books. */
    val name: String,
    val author: String?,
    /** Volumes in reading order (by volume number, then title). */
    val books: List<BookEntity>,
    val completedCount: Int,
    val readingCount: Int,
    /** Whole volume numbers between 1 and the highest one that aren't on the phone. */
    val missingVolumes: List<Int>,
    /** Ids of books that arrived recently (after the first scan). */
    val newBookIds: Set<Long>,
    val status: SeriesStatus?,
    val lastAddedAt: Long,
) {
    val volumeCount: Int get() = books.size
    val hasGaps: Boolean get() = missingVolumes.isNotEmpty()
    val hasNew: Boolean get() = newBookIds.isNotEmpty()
}

/** One line in the series screen: a book, or a placeholder for a missing volume. */
sealed interface SeriesEntry {
    data class Book(val book: BookEntity) : SeriesEntry
    data class Missing(val volume: Int) : SeriesEntry
}

/** Groups books into series and finds gaps. Pure Kotlin, unit-tested. */
object SeriesGrouper {

    /** Books added this long after the very first scan count as "New" for a week. */
    private val NEW_WINDOW_MS = TimeUnit.DAYS.toMillis(7)
    private val FIRST_SCAN_GRACE_MS = TimeUnit.HOURS.toMillis(1)

    /** Numbering this high is probably not a volume number (a year, an ISBN chunk...): no gap check. */
    private const val MAX_GAP_CHECK = 200

    fun keyOf(seriesName: String): String = LibraryQuery.normalize(seriesName)

    fun group(
        books: List<BookEntity>,
        statusOf: (Long) -> ReadingStatus,
        seriesStatus: Map<String, SeriesStatus>,
        now: Long,
    ): List<SeriesSummary> {
        // The library's first scan adds everything at once; only later arrivals are "new".
        val firstAdded = books.minOfOrNull { it.addedAt } ?: now
        val newCutoff = maxOf(firstAdded + FIRST_SCAN_GRACE_MS, now - NEW_WINDOW_MS)

        return books
            .filter { !it.seriesName.isNullOrBlank() }
            .groupBy { keyOf(it.seriesName!!) }
            .filterKeys { it.isNotEmpty() }
            .map { (key, group) ->
                val sorted = group.sortedWith(
                    compareBy<BookEntity, Double?>(nullsLast()) { it.seriesIndex }
                        .thenComparator { a, b -> NaturalOrder.compare(a.title, b.title) },
                )
                SeriesSummary(
                    key = key,
                    name = mostCommon(group.mapNotNull { it.seriesName?.trim() }) ?: key,
                    author = mostCommon(group.mapNotNull { it.authors }),
                    books = sorted,
                    completedCount = group.count { statusOf(it.id) == ReadingStatus.COMPLETED },
                    readingCount = group.count { statusOf(it.id) == ReadingStatus.READING },
                    missingVolumes = missingVolumes(group.mapNotNull { it.seriesIndex }),
                    newBookIds = group.filter { it.addedAt > newCutoff }.map { it.id }.toSet(),
                    status = seriesStatus[key],
                    lastAddedAt = group.maxOf { it.addedAt },
                )
            }
            .sortedWith(compareBy<SeriesSummary, String>(NaturalOrder) { it.name.lowercase() })
    }

    /** Whole numbers from 1 up to the highest volume that are missing. Decimals (2.5) fill nothing. */
    fun missingVolumes(indexes: List<Double>): List<Int> {
        val whole = indexes.filter { it >= 1 && it % 1.0 == 0.0 }.map { it.toInt() }.toSet()
        val max = whole.maxOrNull() ?: return emptyList()
        if (max > MAX_GAP_CHECK) return emptyList()
        return (1..max).filter { it !in whole }
    }

    /** Books in order, with placeholders where a missing volume belongs. */
    fun entries(series: SeriesSummary): List<SeriesEntry> {
        val missing = series.missingVolumes.toMutableList()
        val out = mutableListOf<SeriesEntry>()
        for (book in series.books) {
            val index = book.seriesIndex
            while (index != null && missing.isNotEmpty() && missing.first() < index) {
                out += SeriesEntry.Missing(missing.removeAt(0))
            }
            out += SeriesEntry.Book(book)
        }
        missing.forEach { out += SeriesEntry.Missing(it) }
        return out
    }

    private fun mostCommon(values: List<String>): String? =
        values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}
