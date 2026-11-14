package com.tsundoku.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The library database (stored privately on the phone).
 * Schema history is exported to app/schemas so upgrades never wipe your data.
 */
@Database(entities = [BookEntity::class], version = 1, exportSchema = true)
abstract class TsundokuDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        fun create(context: Context): TsundokuDatabase =
            Room.databaseBuilder(context, TsundokuDatabase::class.java, "tsundoku.db").build()
    }
}
