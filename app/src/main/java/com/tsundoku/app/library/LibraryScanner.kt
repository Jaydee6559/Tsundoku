package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.epub.EpubParser
import com.tsundoku.app.epub.EpubResult
import com.tsundoku.app.epub.FileFingerprint
import com.tsundoku.app.epub.FilenameInfo
import com.tsundoku.app.epub.FilenameParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps the database in step with storage.
 *  - [scan]: full pass over the whole phone (app start, "Scan now", background job).
 *  - [syncPaths]: only the files a folder watcher just reported (instant updates).
 * Both compare with the database and read only what's new or changed.
 */
class LibraryScanner(
    private val discovery: EpubDiscovery,
    private val dao: BookDao,
    private val coversDir: File,
    /** Folders you chose to ignore (Settings → Excluded folders). Books there count as missing. */
    private val excludedFolders: suspend () -> Set<String> = { emptySet() },
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(PARALLELISM)

    suspend fun scan(onProgress: (ScanState) -> Unit): ScanState.Done = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        onProgress(ScanState.Discovering)

        val root = discovery.storageRoot.absolutePath
        // Remove copies of trashed books that older versions wrongly re-added from the Trash folder.
        dao.deleteStrayTrashCopies()
        val found = discovery.findAll(excludedFolders())
        val folders = found
            .groupingBy { topFolderOf(folderOf(it.path, root)) }
            .eachCount()
            .map { (name, count) -> FolderCount(name, count) }
            .sortedByDescending { it.count }

        val basePlan = ScanPlanner.plan(dao.scanRows(), found) { safeFingerprint(it) }
        // Books scanned by an older version are read once more to pick up newer details (EPUB tags).
        val alreadyQueued = basePlan.changed.map { it.first }.toSet() + basePlan.moved.map { it.first }
        val refresh = dao.rowsNeedingMetadata()
            .filter { it.id !in alreadyQueued && File(it.path).isFile }
            .map { it.id to FoundFile(it.path, it.sizeBytes, it.lastModified) }
        val plan = basePlan.copy(changed = basePlan.changed + refresh)
        applyPlan(plan, root, found.size, folders, started, onProgress)
    }

    /**
     * Updates only [paths] (files created, changed, moved or deleted in a watched folder).
     * Missing books are included so a file that reappears elsewhere is recognised as a move.
     */
    suspend fun syncPaths(paths: Set<String>): ScanState.Done = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val root = discovery.storageRoot.absolutePath
        val excluded = excludedFolders()
        val found = paths.mapNotNull { path ->
            val f = File(path)
            if (WatchRules.isHiddenPath(f.absolutePath, root)) return@mapNotNull null // e.g. our own Trash
            if (FolderRules.isExcluded(folderOf(f.absolutePath, root), excluded)) return@mapNotNull null
            if (f.isFile && f.length() > 0 && f.name.endsWith(".epub", ignoreCase = true)) {
                FoundFile(f.absolutePath, f.length(), f.lastModified())
            } else {
                null
            }
        }
        val rows = (
            dao.missingScanRows() +
                paths.toList().chunked(BookDao.CHUNK).flatMap { dao.scanRowsForPaths(it) }
            ).distinctBy { it.id }
        val plan = ScanPlanner.plan(rows, found) { safeFingerprint(it) }
        applyPlan(plan, root, found.size, emptyList(), started) {}
    }

    /** Writes a plan to the database: cheap updates first, then reading new/changed books. */
    private suspend fun applyPlan(
        plan: ScanPlan,
        root: String,
        foundCount: Int,
        folders: List<FolderCount>,
        started: Long,
        onProgress: (ScanState) -> Unit,
    ): ScanState.Done = coroutineScope {
        val now = System.currentTimeMillis()

        for ((id, file) in plan.moved) {
            val f = File(file.path)
            dao.moveFile(id, file.path, f.name, folderOf(file.path, root), file.sizeBytes, file.lastModified, now)
        }
        plan.reappeared.chunked(BookDao.CHUNK).forEach { dao.markPresent(it, now) }
        plan.missing.chunked(BookDao.CHUNK).forEach { dao.markMissing(it, now) }

        val total = plan.newFiles.size + plan.changed.size
        val processed = AtomicInteger(0)
        val broken = AtomicInteger(0)
        val addedTitles = mutableListOf<String>()
        fun report() = onProgress(ScanState.Reading(foundCount, processed.get(), total, folders))
        report()

        plan.newFiles.chunked(BATCH).forEach { batch ->
            val entities = batch.map { (file, fp) ->
                async(io) {
                    safeReadBook(file, fp, root, addedAt = now).also {
                        if (it.parseError != null) broken.incrementAndGet()
                        processed.incrementAndGet()
                    }
                }
            }.awaitAll()
            dao.insertInChunks(entities)
            if (addedTitles.size < MAX_TITLES) addedTitles += entities.take(MAX_TITLES - addedTitles.size).map { it.title }
            report()
        }

        plan.changed.chunked(BATCH).forEach { batch ->
            val old = dao.getByIds(batch.map { it.first }).associateBy { it.id }
            val entities = batch.mapNotNull { (id, file) ->
                val previous = old[id] ?: return@mapNotNull null
                async(io) {
                    val fp = safeFingerprint(file)
                    safeReadBook(file, fp, root, addedAt = previous.addedAt).copy(id = id).also {
                        if (it.parseError != null) broken.incrementAndGet()
                        processed.incrementAndGet()
                    }
                }
            }.awaitAll()
            dao.updateAll(entities)
            report()
        }

        ScanState.Done(
            found = foundCount,
            added = plan.newFiles.size,
            updated = plan.changed.size,
            moved = plan.moved.size,
            missing = plan.missing.size,
            broken = broken.get(),
            durationMs = System.currentTimeMillis() - started,
            folders = folders,
            addedTitles = addedTitles.toList(),
        ).also(onProgress)
    }

    /** A file we can't read still gets a stable id-like value instead of stopping the scan. */
    private fun safeFingerprint(file: FoundFile): String = try {
        FileFingerprint.compute(File(file.path))
    } catch (e: Exception) {
        "unreadable-${file.sizeBytes}-${file.path.hashCode()}"
    }

    /**
     * Like [readBook], but survives anything (even running out of memory on a giant cover):
     * the book is then saved from its file name only, marked as broken.
     */
    private fun safeReadBook(file: FoundFile, fingerprint: String, root: String, addedAt: Long): BookEntity = try {
        readBook(file, fingerprint, root, addedAt)
    } catch (t: Throwable) {
        if (t is kotlinx.coroutines.CancellationException) throw t
        val f = File(file.path)
        // Even the fallback must not fail: use the bare file name if parsing it breaks too.
        val fromName = runCatching { FilenameParser.parse(f.name) }
            .getOrElse { FilenameInfo(title = f.nameWithoutExtension, seriesName = null, volume = null) }
        BookEntity(
            path = file.path,
            fileName = f.name,
            folder = folderOf(file.path, root),
            sizeBytes = file.sizeBytes,
            lastModified = file.lastModified,
            fingerprint = fingerprint,
            title = fromName.title,
            authors = null,
            seriesName = fromName.seriesName,
            seriesIndex = fromName.volume,
            language = null,
            publisher = null,
            description = null,
            spineCount = 0,
            coverThumbPath = null,
            parseError = "Couldn't read: ${t.javaClass.simpleName}",
            subjects = "",
            isMissing = false,
            addedAt = addedAt,
            updatedAt = System.currentTimeMillis(),
        )
    }

    /** Reads one EPUB into a database row. Broken files get [BookEntity.parseError]. */
    private fun readBook(file: FoundFile, fingerprint: String, root: String, addedAt: Long): BookEntity {
        val f = File(file.path)
        val fromName = FilenameParser.parse(f.name)
        val result = EpubParser.parse(f)
        val meta = (result as? EpubResult.Success)?.metadata

        val thumb = meta?.coverPath?.let { coverPath ->
            val out = File(coversDir, "$fingerprint.webp")
            if (out.exists()) {
                out.absolutePath
            } else {
                EpubParser.readCover(f, coverPath)
                    ?.takeIf { CoverThumbnailer.save(it, out) }
                    ?.let { out.absolutePath }
            }
        }

        return BookEntity(
            path = file.path,
            fileName = f.name,
            folder = folderOf(file.path, root),
            sizeBytes = file.sizeBytes,
            lastModified = file.lastModified,
            fingerprint = fingerprint,
            title = meta?.title?.takeIf { it.isNotBlank() } ?: fromName.title,
            authors = meta?.authors?.takeIf { it.isNotEmpty() }?.joinToString(", "),
            seriesName = meta?.seriesName ?: fromName.seriesName,
            seriesIndex = meta?.seriesIndex ?: fromName.volume,
            language = meta?.language,
            publisher = meta?.publisher,
            description = meta?.description,
            spineCount = meta?.spineCount ?: 0,
            coverThumbPath = thumb,
            parseError = (result as? EpubResult.Failure)?.reason,
            subjects = meta?.subjects?.joinToString("\n").orEmpty(),
            isMissing = false,
            addedAt = addedAt,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val PARALLELISM = 4
        const val BATCH = 40
        const val MAX_TITLES = 5
    }
}
