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

    /** Books that exist on the phone (not missing, not in Trash). Updates the UI automatically. */
    @Query("SELECT * FROM books WHERE isMissing = 0 AND trashedAt IS NULL ORDER BY addedAt DESC, title COLLATE NOCASE")
    abstract fun observeLibrary(): Flow<List<BookEntity>>

    /** Books in Trash, most recently trashed first. */
    @Query("SELECT * FROM books WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    abstract fun observeTrash(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE trashedAt IS NOT NULL")
    abstract suspend fun getTrashed(): List<BookEntity>

    @Query("UPDATE books SET path = :trashPath, originalPath = :originalPath, trashedAt = :now, updatedAt = :now WHERE id = :id")
    abstract suspend fun markTrashed(id: Long, trashPath: String, originalPath: String, now: Long)

    @Query(
        "UPDATE books SET path = :path, fileName = :fileName, folder = :folder, originalPath = NULL, " +
            "trashedAt = NULL, isMissing = 0, updatedAt = :now WHERE id = :id",
    )
    abstract suspend fun markRestored(id: Long, path: String, fileName: String, folder: String, now: Long)

    /** Paths that begin with [prefix] (used to find a free name when restoring). */
    @Query("SELECT path FROM books WHERE substr(path, 1, length(:prefix)) = :prefix")
    abstract suspend fun pathsStartingWith(prefix: String): List<String>

    @Query("SELECT id FROM books WHERE path = :path")
    abstract suspend fun idForPath(path: String): Long?

    /** Best match for a backup entry: books on the phone first, then missing, then trashed. */
    @Query("SELECT * FROM books WHERE fingerprint = :fingerprint ORDER BY (trashedAt IS NOT NULL), isMissing LIMIT 1")
    abstract suspend fun findByFingerprint(fingerprint: String): BookEntity?

    @Query("SELECT * FROM books WHERE path = :path LIMIT 1")
    abstract suspend fun findByPath(path: String): BookEntity?

    // ---- counts for the Scanner status screen
    @Query("SELECT COUNT(*) FROM books WHERE trashedAt IS NULL AND isMissing = 0")
    abstract fun observeLibraryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE trashedAt IS NULL AND isMissing = 1")
    abstract fun observeMissingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE trashedAt IS NULL AND isMissing = 0 AND parseError IS NOT NULL")
    abstract fun observeBrokenCount(): Flow<Int>

    @Query("DELETE FROM books WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM books WHERE fingerprint = :fingerprint")
    abstract suspend fun countWithFingerprint(fingerprint: String): Int

    @Query("SELECT * FROM books WHERE id = :id")
    abstract fun observeBook(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    abstract suspend fun getById(id: Long): BookEntity?

    @Query("SELECT id, path, sizeBytes, lastModified, fingerprint, isMissing FROM books WHERE trashedAt IS NULL AND isMissing = 1")
    abstract suspend fun missingScanRows(): List<BookScanRow>

    @Query("SELECT id, path, sizeBytes, lastModified, fingerprint, isMissing FROM books WHERE trashedAt IS NULL AND path IN (:paths)")
    abstract suspend fun scanRowsForPaths(paths: List<String>): List<BookScanRow>

    /** Folders that currently hold books (watched for live updates). */
    @Query("SELECT DISTINCT folder FROM books WHERE trashedAt IS NULL AND isMissing = 0")
    abstract suspend fun bookFolders(): List<String>

    /** Trashed books are left out: their files live in a hidden folder the scan skips on purpose. */
    @Query("SELECT id, path, sizeBytes, lastModified, fingerprint, isMissing FROM books WHERE trashedAt IS NULL")
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
