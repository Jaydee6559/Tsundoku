package com.tsundoku.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tsundoku.app.TsundokuApplication
import com.tsundoku.app.storage.StoragePermission
import java.util.concurrent.TimeUnit

/**
 * Full library scan every few hours, even when the app isn't open. Catches anything the live
 * watcher missed (new sub-folders, files added while the app was closed). Android decides the
 * exact time and only runs it when the battery isn't low.
 */
class LibrarySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!StoragePermission.isGranted()) return Result.success() // nothing we may look at
        val container = (applicationContext as TsundokuApplication).container
        container.library.scanQuietly() // result goes to the notifier via LibraryRepository.quietResults
        container.trash.purgeExpiredInBackground()
        return Result.success()
    }

    companion object {
        private const val NAME = "library-sync"
        private const val EVERY_HOURS = 6L

        /** Safe to call on every app start: an existing schedule is kept, not duplicated. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LibrarySyncWorker>(EVERY_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
