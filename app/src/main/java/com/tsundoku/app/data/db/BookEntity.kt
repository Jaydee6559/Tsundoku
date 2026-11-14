package com.tsundoku.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One EPUB file on the phone.
 *
 * The [id] never changes, even when the file is moved or renamed (matched by [fingerprint]),
 * so reading status / notes attached to it later (Batch 7) are never lost.
 * Files deleted outside the app are marked [isMissing] instead of being removed.
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["fingerprint"]),
        Index(value = ["seriesName"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // ---- file
    val path: String,
    val fileName: String,
    /** Folder relative to internal storage, e.g. "Download/Novels" ("" = storage root). */
    val folder: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val fingerprint: String,

    // ---- metadata (EPUB first, file name as fallback)
    val title: String,
    /** Comma-separated author names, or null. */
    val authors: String?,
    val seriesName: String?,
    val seriesIndex: Double?,
    val language: String?,
    val publisher: String?,
    val description: String?,
    val spineCount: Int,
    /** Small cover image saved in the app's private storage, or null. */
    val coverThumbPath: String?,
    /** Why the EPUB couldn't be read (null = read fine). Shown on the "Broken files" shelf. */
    val parseError: String?,

    // ---- state
    @ColumnInfo(defaultValue = "0") val isMissing: Boolean = false,
    val addedAt: Long,
    val updatedAt: Long,

    // ---- trash (DB v3). While trashed, [path] points into /.tsundoku/trash.
    /** When the book was moved to Trash (null = not in Trash). */
    val trashedAt: Long? = null,
    /** Where the file lived before it was trashed; used by Restore. */
    val originalPath: String? = null,
)

/** The few columns the scanner needs to compare the database with what's on disk. */
data class BookScanRow(
    val id: Long,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val fingerprint: String,
    val isMissing: Boolean,
)
