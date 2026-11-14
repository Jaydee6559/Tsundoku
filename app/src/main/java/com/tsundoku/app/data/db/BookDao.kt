package com.tsundoku.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BookDao {

    /** Books that exist on the phone, newest first. Updates the UI automatically. */
    @Query("SELECT * FROM books WHERE isMissing = 0 ORDER BY addedAt DESC, title COLLATE NOCASE")
    abstract fun observeLibrary(): Flow<List<BookEntity>>

    @Query("SELECT id, path, sizeBytes, lastModified, fingerprint, isMissing FROM books")
    abstract suspend fun scanRows(): List<BookScanRow>

    @Query("SELECT * FROM books WHERE id IN (:ids)")
    abstract suspend fun getByIds(ids: List<Long>): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(books: List<BookEntity>)

    @Update
    abstract suspend fun updateAll(books: List<BookEntity>)

    @Query("UPDATE books SET isMissing = 1, updatedAt = :now WHERE id IN (:ids)")
    abstract suspend fun markMissing(ids: List<Long>, now: Long)

    @Query("UPDATE books SET isMissing = 0, updatedAt = :now WHERE id IN (:ids)")
    abstract suspend fun markPresent(ids: List<Long>, now: Long)

    @Query(
        """
        UPDATE books SET path = :path, fileName = :fileName, folder = :folder,
            sizeBytes = :sizeBytes, lastModified = :lastModified, isMissing = 0, updatedAt = :now
        WHERE id = :id
        """,
    )
    abstract suspend fun moveFile(
        id: Long,
        path: String,
        fileName: String,
        folder: String,
        sizeBytes: Long,
        lastModified: Long,
        now: Long,
    )

    @Transaction
    open suspend fun insertInChunks(books: List<BookEntity>) {
        books.chunked(CHUNK).forEach { insertAll(it) }
    }

    companion object {
        /** SQLite allows ~999 query parameters; stay well below. */
        const val CHUNK = 400
    }
}
