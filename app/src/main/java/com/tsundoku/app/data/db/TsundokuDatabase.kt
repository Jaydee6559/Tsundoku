package com.tsundoku.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The library database (stored privately on the phone).
 * Every schema change ships with a migration in [Migrations], so updates never wipe your data.
 */
@Database(
    entities = [
        BookEntity::class,
        TrackingEntity::class,
        TagEntity::class,
        BookTagEntity::class,
        StatusEventEntity::class,
        SeriesInfoEntity::class,
        UpNextEntity::class,
        DeletedBookEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class TsundokuDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun trackingDao(): TrackingDao
    abstract fun seriesDao(): SeriesDao
    abstract fun upNextDao(): UpNextDao
    abstract fun deletedBookDao(): DeletedBookDao

    companion object {
        private const val NAME = "tsundoku.db"

        fun create(context: Context): TsundokuDatabase {
            val previousVersion = existingVersion(context)
            val db = build(context)
            return try {
                db.openHelper.writableDatabase // opens now, so a failed upgrade is caught here
                db
            } catch (e: IllegalStateException) {
                // Safety net for the v1 -> v2 upgrade ONLY: v1 held nothing but scan results
                // (rebuilt automatically by the next scan), so starting fresh loses nothing.
                // From v2 on, your reading data lives here and this never applies.
                if (previousVersion != 1) throw e
                db.close()
                context.deleteDatabase(NAME)
                build(context)
            }
        }

        private fun build(context: Context): TsundokuDatabase =
            Room.databaseBuilder(context, TsundokuDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                .build()

        /** Version of the database already on the phone (0 = none yet). */
        private fun existingVersion(context: Context): Int = runCatching {
            val file = context.getDatabasePath(NAME)
            if (!file.exists()) return 0
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        }.getOrDefault(0)
    }
}
