package com.tsundoku.app.library

import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.SeriesInfoEntity
import com.tsundoku.app.data.db.SeriesStatus
import com.tsundoku.app.data.db.TsundokuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Series, grouped live from the library, plus the status you set for each. */
class SeriesRepository(
    db: TsundokuDatabase,
    tracking: TrackingRepository,
) {
    private val seriesDao = db.seriesDao()

    val series: Flow<List<SeriesSummary>> = combine(
        db.bookDao().observeLibrary(),
        tracking.observeStatuses(),
        seriesDao.observeAll(),
    ) { books, statuses, infos ->
        SeriesGrouper.group(
            books = books,
            statusOf = { statuses[it] ?: ReadingStatus.PLAN },
            seriesStatus = infos.associate { it.seriesKey to it.status },
            now = System.currentTimeMillis(),
        )
    }.flowOn(Dispatchers.Default)

    fun observeSeries(key: String): Flow<SeriesSummary?> = series.map { list -> list.firstOrNull { it.key == key } }

    /** Sets (or clears, with null) the series status. */
    suspend fun setStatus(key: String, status: SeriesStatus?) {
        if (status == null) {
            seriesDao.delete(key)
        } else {
            seriesDao.upsert(SeriesInfoEntity(seriesKey = key, status = status, updatedAt = System.currentTimeMillis()))
        }
    }
}
