package com.tsundoku.app.library

import java.util.concurrent.TimeUnit

/** Naming and timing rules for Trash. Pure functions, unit-tested. */
object TrashRules {

    const val KEEP_DAYS = 30

    /** File name inside the trash folder: unique even if two books share a name. */
    fun trashFileName(bookId: Long, now: Long, originalName: String): String =
        "${bookId}_${now}__$originalName"

    /** Undoes [trashFileName]; falls back to the whole name for files trashed some other way. */
    fun originalNameOf(trashFileName: String): String =
        trashFileName.substringAfter("__", missingDelimiterValue = trashFileName)

    /**
     * Where to restore a file. If something already sits at the old path, picks
     * "name (1).epub", "name (2).epub", ... so nothing is ever overwritten.
     */
    fun restoreTarget(originalPath: String, exists: (String) -> Boolean): String {
        if (!exists(originalPath)) return originalPath
        val dir = originalPath.substringBeforeLast('/', "")
        val file = originalPath.substringAfterLast('/')
        val base = file.substringBeforeLast('.', file)
        val ext = if ('.' in file) "." + file.substringAfterLast('.') else ""
        var n = 1
        while (true) {
            val candidate = (if (dir.isEmpty()) "" else "$dir/") + "$base ($n)$ext"
            if (!exists(candidate)) return candidate
            n++
        }
    }

    /** Whole days left before automatic deletion (0 = goes on the next cleanup). */
    fun daysLeft(trashedAt: Long, now: Long): Int {
        val elapsedDays = TimeUnit.MILLISECONDS.toDays((now - trashedAt).coerceAtLeast(0))
        return (KEEP_DAYS - elapsedDays).toInt().coerceAtLeast(0)
    }

    fun isExpired(trashedAt: Long, now: Long): Boolean =
        now - trashedAt >= TimeUnit.DAYS.toMillis(KEEP_DAYS.toLong())
}
