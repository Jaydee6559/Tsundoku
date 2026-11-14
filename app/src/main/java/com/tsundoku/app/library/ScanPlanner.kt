package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookScanRow

/** A file found on storage during a scan. */
data class FoundFile(val path: String, val sizeBytes: Long, val lastModified: Long)

/** What has to happen to the database after comparing it with storage. */
data class ScanPlan(
    /** Brand-new books (with their fingerprint already computed). */
    val newFiles: List<Pair<FoundFile, String>>,
    /** Same path, but the file changed: re-read metadata. */
    val changed: List<Pair<Long, FoundFile>>,
    /** Same book at a new path (moved / renamed): just update the path, keep everything else. */
    val moved: List<Pair<Long, FoundFile>>,
    /** Was missing, now back at the same path. */
    val reappeared: List<Long>,
    /** In the database but no longer on storage: mark missing (never delete tracking data). */
    val missing: List<Long>,
    val unchangedCount: Int,
)

/**
 * Pure comparison logic (no Android, no disk access except through [fingerprintOf]),
 * so it can be unit-tested thoroughly. This is what protects your data when files move.
 */
object ScanPlanner {

    fun plan(
        existing: List<BookScanRow>,
        found: List<FoundFile>,
        fingerprintOf: (FoundFile) -> String,
    ): ScanPlan {
        val byPath = existing.associateBy { it.path }
        val foundPaths = found.mapTo(HashSet()) { it.path }

        val changed = mutableListOf<Pair<Long, FoundFile>>()
        val reappeared = mutableListOf<Long>()
        val candidates = mutableListOf<FoundFile>() // not in DB by path: new or moved
        var unchanged = 0

        for (file in found) {
            val row = byPath[file.path]
            when {
                row == null -> candidates += file
                row.sizeBytes != file.sizeBytes || row.lastModified != file.lastModified -> changed += row.id to file
                row.isMissing -> reappeared += row.id
                else -> unchanged++
            }
        }

        // Rows whose file is gone from its old path: either moved somewhere, or really missing.
        val goneByFingerprint = existing
            .filter { it.path !in foundPaths }
            .groupByTo(HashMap()) { it.fingerprint }

        val newFiles = mutableListOf<Pair<FoundFile, String>>()
        val moved = mutableListOf<Pair<Long, FoundFile>>()
        for (file in candidates) {
            val fp = fingerprintOf(file)
            val match = goneByFingerprint[fp]?.removeFirstOrNull()
            if (match != null) moved += match.id to file else newFiles += file to fp
        }

        val missing = goneByFingerprint.values.flatten().filter { !it.isMissing }.map { it.id }

        return ScanPlan(
            newFiles = newFiles,
            changed = changed,
            moved = moved,
            reappeared = reappeared,
            missing = missing,
            unchangedCount = unchanged,
        )
    }
}
