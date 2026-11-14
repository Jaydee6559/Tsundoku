package com.tsundoku.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Whether the author is still writing the series. Set by you; stored by name. */
enum class SeriesStatus { ONGOING, COMPLETED, HIATUS }

/**
 * Your info about one series (DB v4). Series themselves aren't stored: they're grouped live from
 * the books' series names, so this table only holds what you set by hand.
 */
@Entity(tableName = "series_info")
data class SeriesInfoEntity(
    /** Normalized series name (see SeriesGrouper.keyOf), so "Lantern Saga" and "lantern saga" match. */
    @PrimaryKey val seriesKey: String,
    val status: SeriesStatus,
    val updatedAt: Long,
)

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series_info")
    fun observeAll(): Flow<List<SeriesInfoEntity>>

    @Upsert
    suspend fun upsert(info: SeriesInfoEntity)

    @Query("SELECT * FROM series_info")
    suspend fun getAll(): List<SeriesInfoEntity>

    @Query("DELETE FROM series_info WHERE seriesKey = :key")
    suspend fun delete(key: String)
}
