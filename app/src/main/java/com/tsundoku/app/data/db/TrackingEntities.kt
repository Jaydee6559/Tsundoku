package com.tsundoku.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Reading status. Stored by name, so the order here can change safely. */
enum class ReadingStatus { PLAN, READING, COMPLETED, ON_HOLD, DROPPED }

/**
 * Your reading data for one book. Kept separate from [BookEntity] so rescans
 * (which rewrite book metadata) can never touch it.
 */
@Entity(
    tableName = "tracking",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class TrackingEntity(
    @PrimaryKey val bookId: Long,
    val status: ReadingStatus,
    /** 0 = not rated, 1..5 stars. */
    val rating: Int,
    val currentChapter: Int,
    /** Your own chapter total; null = use the EPUB's chapter count. */
    val totalChapters: Int?,
    val notes: String?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val lastOpenedAt: Long?,
    val updatedAt: Long,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stored trimmed and lowercase, so "Fantasy" and "fantasy" are one tag. */
    val name: String,
)

@Entity(
    tableName = "book_tags",
    primaryKeys = ["bookId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["tagId"])],
)
data class BookTagEntity(val bookId: Long, val tagId: Long)

/** Every status change, with its time. Not shown yet; it powers reading stats later. */
@Entity(
    tableName = "status_events",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["bookId"])],
)
data class StatusEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val status: ReadingStatus,
    val at: Long,
)

/** A book id with one of its tag names (for backups). */
data class BookTagName(val bookId: Long, val name: String)
