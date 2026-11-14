package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.TrackingEntity
import com.tsundoku.app.data.db.subjectList
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** One year of reading, for the stats screen and yearly wrap-up. */
data class YearStats(
    val year: Int,
    val finished: Int,
    /** Books finished in each month, January first. */
    val perMonth: List<Int>,
    val started: Int,
    /** Average days from starting to finishing, for books finished this year (null = no data). */
    val averageDaysToFinish: Int?,
    val topAuthor: String?,
    /** Most common tags (EPUB + yours) among books finished this year. */
    val topTags: List<String>,
    /** Titles finished this year, newest first. */
    val finishedTitles: List<String>,
)

data class OverallStats(val reading: Int, val plan: Int, val completed: Int, val total: Int)

/** Turns tracking data into numbers. Pure Kotlin, unit-tested. */
object StatsRules {

    fun overall(books: List<BookEntity>, tracking: List<TrackingEntity>): OverallStats {
        val c = HomeRules.counts(books, tracking.associate { it.bookId to it.status })
        return OverallStats(c.reading, c.plan, c.completed, books.size)
    }

    /** Years that have any finished book, plus the current one, newest first. */
    fun years(tracking: List<TrackingEntity>, now: Long, zone: TimeZone = TimeZone.getDefault()): List<Int> =
        (tracking.mapNotNull { it.finishedAt?.let { t -> yearOf(t, zone) } } + yearOf(now, zone))
            .distinct()
            .sortedDescending()

    fun year(
        year: Int,
        books: List<BookEntity>,
        tracking: List<TrackingEntity>,
        userTagsOf: (Long) -> List<String>,
        zone: TimeZone = TimeZone.getDefault(),
    ): YearStats {
        val byId = books.associateBy { it.id }
        val finished = tracking
            .filter { it.status == ReadingStatus.COMPLETED && it.finishedAt != null && yearOf(it.finishedAt, zone) == year }
            .filter { it.bookId in byId }
            .sortedByDescending { it.finishedAt }

        val perMonth = MutableList(12) { 0 }
        finished.forEach { perMonth[monthOf(it.finishedAt!!, zone)]++ }

        val durations = finished.mapNotNull { t ->
            val start = t.startedAt ?: return@mapNotNull null
            val end = t.finishedAt ?: return@mapNotNull null
            if (end >= start) TimeUnit.MILLISECONDS.toDays(end - start).toInt() else null
        }

        val finishedBooks = finished.mapNotNull { byId[it.bookId] }
        val topAuthor = finishedBooks
            .mapNotNull { b -> b.authors?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } }
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
            ?.key
        val topTags = finishedBooks
            .flatMap { b -> (b.subjectList + userTagsOf(b.id)).map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct() }
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(TOP_TAGS)
            .map { it.key }

        return YearStats(
            year = year,
            finished = finished.size,
            perMonth = perMonth,
            started = tracking.count { it.startedAt != null && yearOf(it.startedAt, zone) == year && it.bookId in byId },
            averageDaysToFinish = durations.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            topAuthor = topAuthor,
            topTags = topTags,
            finishedTitles = finishedBooks.map { it.title },
        )
    }

    fun yearOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.YEAR)

    private fun monthOf(millis: Long, zone: TimeZone): Int =
        Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.MONTH)

    private const val TOP_TAGS = 3
}
