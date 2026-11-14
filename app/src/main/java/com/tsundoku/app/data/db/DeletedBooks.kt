package com.tsundoku.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A book you deleted forever (DB v7). If a byte-identical copy of it appears again on the phone
 * (another app re-saving it, a sync, a re-download), the scanner deletes that copy too instead of
 * adding it back, and records where it was found.
 */
@Entity(tableName = "deleted_books")
data class DeletedBookEntity(
    /** File fingerprint (size + head/tail hash): identifies identical copies anywhere. */
    @PrimaryKey val fingerprint: String,
    val title: String,
    val deletedAt: Long,
    /** How many reappearing copies were removed again. */
    val timesRemoved: Int,
    /** Where the last reappearing copy was found (helps tell which app re-creates it). */
    val lastSeenPath: String?,
    val lastSeenAt: Long?,
)

@Dao
interface DeletedBookDao {
    @Query("SELECT * FROM deleted_books ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<DeletedBookEntity>>

    @Query("SELECT fingerprint FROM deleted_books")
    suspend fun fingerprints(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DeletedBookEntity)

    @Query(
        "UPDATE deleted_books SET timesRemoved = timesRemoved + 1, lastSeenPath = :path, lastSeenAt = :at " +
            "WHERE fingerprint = :fingerprint",
    )
    suspend fun recordRemovedAgain(fingerprint: String, path: String, at: Long)

    /** "Allow again": the book may come back into the library. */
    @Query("DELETE FROM deleted_books WHERE fingerprint = :fingerprint")
    suspend fun allow(fingerprint: String)
}
