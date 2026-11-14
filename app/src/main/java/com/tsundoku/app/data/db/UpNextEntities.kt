package com.tsundoku.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One book in your Up Next reading queue (DB v5). Lower [position] = read sooner. */
@Entity(
    tableName = "up_next",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class UpNextEntity(
    @PrimaryKey val bookId: Long,
    val position: Int,
    val addedAt: Long,
)

@Dao
interface UpNextDao {

    /** Queue in order. Books in Trash or missing from the phone are skipped (but stay queued). */
    @Query(
        "SELECT books.* FROM up_next INNER JOIN books ON books.id = up_next.bookId " +
            "WHERE books.trashedAt IS NULL AND books.isMissing = 0 ORDER BY up_next.position",
    )
    fun observeQueue(): Flow<List<BookEntity>>

    @Query("SELECT bookId FROM up_next")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT * FROM up_next ORDER BY position")
    suspend fun getAll(): List<UpNextEntity>

    @Query("SELECT bookId FROM up_next ORDER BY position")
    suspend fun orderedIds(): List<Long>

    @Query("SELECT COALESCE(MAX(position), -1) FROM up_next")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: UpNextEntity)

    @Query("DELETE FROM up_next WHERE bookId = :bookId")
    suspend fun remove(bookId: Long)

    @Query("UPDATE up_next SET position = :position WHERE bookId = :bookId")
    suspend fun setPosition(bookId: Long, position: Int)
}
