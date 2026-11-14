package com.tsundoku.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database upgrades. Each one changes the schema WITHOUT deleting your data.
 * The SQL must match the entity classes exactly (Room checks it when the app opens).
 */
object Migrations {

    /** v1 -> v2: adds reading tracking, tags and status history. The books table is untouched. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `tracking` (" +
                    "`bookId` INTEGER NOT NULL, `status` TEXT NOT NULL, `rating` INTEGER NOT NULL, " +
                    "`currentChapter` INTEGER NOT NULL, `totalChapters` INTEGER, `notes` TEXT, " +
                    "`startedAt` INTEGER, `finishedAt` INTEGER, `lastOpenedAt` INTEGER, `updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`bookId`), " +
                    "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `tags` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `book_tags` (" +
                    "`bookId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`bookId`, `tagId`), " +
                    "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_tags_tagId` ON `book_tags` (`tagId`)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `status_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL, `at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_status_events_bookId` ON `status_events` (`bookId`)")
        }
    }

    /** v2 -> v3: Trash. Two optional columns on books; nothing else changes. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `books` ADD COLUMN `trashedAt` INTEGER")
            db.execSQL("ALTER TABLE `books` ADD COLUMN `originalPath` TEXT")
        }
    }

    /** v3 -> v4: your series status (Ongoing / Completed / Hiatus). New table only. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `series_info` (" +
                    "`seriesKey` TEXT NOT NULL, `status` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`seriesKey`))",
            )
        }
    }

    /** v4 -> v5: Up Next reading queue. New table only. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `up_next` (" +
                    "`bookId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`bookId`), " +
                    "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
        }
    }

    /** v5 -> v6: EPUB tags (dc:subject). One optional column; filled in by the next scan. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `books` ADD COLUMN `subjects` TEXT")
        }
    }

    /** v6 -> v7: remembers books deleted forever, so identical copies don't come back. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `deleted_books` (" +
                    "`fingerprint` TEXT NOT NULL, `title` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, " +
                    "`timesRemoved` INTEGER NOT NULL, `lastSeenPath` TEXT, `lastSeenAt` INTEGER, " +
                    "PRIMARY KEY(`fingerprint`))",
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
}
