package com.tsundoku.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {

    @Query("SELECT * FROM tracking WHERE bookId = :bookId")
    fun observeTracking(bookId: Long): Flow<TrackingEntity?>

    @Query("SELECT * FROM tracking WHERE bookId = :bookId")
    suspend fun getTracking(bookId: Long): TrackingEntity?

    @Query("SELECT * FROM tracking")
    fun observeAll(): Flow<List<TrackingEntity>>

    @Upsert
    suspend fun upsert(tracking: TrackingEntity)

    @Insert
    suspend fun insertEvent(event: StatusEventEntity)

    // ---- tags

    @Query(
        "SELECT tags.* FROM tags INNER JOIN book_tags ON book_tags.tagId = tags.id " +
            "WHERE book_tags.bookId = :bookId ORDER BY tags.name",
    )
    fun observeTagsForBook(bookId: Long): Flow<List<TagEntity>>

    @Query("SELECT id FROM tags WHERE name = :name")
    suspend fun findTagId(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookTag(link: BookTagEntity)

    @Query("DELETE FROM book_tags WHERE bookId = :bookId AND tagId = :tagId")
    suspend fun removeBookTag(bookId: Long, tagId: Long)

    /** Removes tags no book uses any more. */
    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM book_tags)")
    suspend fun deleteUnusedTags()
}
