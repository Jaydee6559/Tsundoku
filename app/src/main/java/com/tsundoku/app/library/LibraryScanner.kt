package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.epub.EpubParser
import com.tsundoku.app.epub.EpubResult
import com.tsundoku.app.epub.FileFingerprint
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
 * One full scan: find EPUBs -> compare with the database -> read only what's new or changed.
 * After the first scan, a rescan with no changes only touches file sizes/dates (fast).
 */
class LibraryScanner(
    private val discovery: EpubDiscovery,
    private val dao: BookDao,
    private val coversDir: File,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val io = Dispatchers.IO.limitedParallelism(PARALLELISM)

    suspend fun scan(onProgress: (ScanState) -> Unit) = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        onProgress(ScanState.Discovering)

        val root = discovery.storageRoot.absolutePath
        val found = discovery.findAll()
        val folders = found
            .groupingBy { topFolderOf(folderOf(it.path, root)) }
            .eachCount()
            .map { (name, count) -> FolderCount(name, count) }
            .sortedByDescending { it.count }

        val plan = ScanPlanner.plan(dao.scanRows(), found) { FileFingerprint.compute(File(it.path)) }
        val now = System.currentTimeMillis()

        // Cheap updates first: moves, returns, and files gone missing.
        for ((id, file) in plan.moved) {
            val f = File(file.path)
            dao.moveFile(id, file.path, f.name, folderOf(file.path, root), file.sizeBytes, file.lastModified, now)
        }
        plan.reappeared.chunked(BookDao.CHUNK).forEach { dao.markPresent(it, now) }
        plan.missing.chunked(BookDao.CHUNK).forEach { dao.markMissing(it, now) }

        // Expensive part: read metadata + covers, in parallel, saving in batches.
        val total = plan.newFiles.size + plan.changed.size
        val processed = AtomicInteger(0)
        val broken = AtomicInteger(0)
        fun report() = onProgress(ScanState.Reading(found.size, processed.get(), total, folders))
        report()

        plan.newFiles.chunked(BATCH).forEach { batch ->
            val entities = coroutineScope {
                batch.map { (file, fp) ->
                    async(io) {
                        readBook(file, fp, root, addedAt = now).also {
                            if (it.parseError != null) broken.incrementAndGet()
                            processed.incrementAndGet()
                        }
                    }
                }.awaitAll()
            }
            dao.insertInChunks(entities)
            report()
        }

        plan.changed.chunked(BATCH).forEach { batch ->
            val old = dao.getByIds(batch.map { it.first }).associateBy { it.id }
            val entities = coroutineScope {
                batch.mapNotNull { (id, file) ->
                    val previous = old[id] ?: return@mapNotNull null
                    async(io) {
                        val fp = FileFingerprint.compute(File(file.path))
                        readBook(file, fp, root, addedAt = previous.addedAt).copy(id = id).also {
                            if (it.parseError != null) broken.incrementAndGet()
                            processed.incrementAndGet()
                        }
                    }
                }.awaitAll()
            }
            dao.updateAll(entities)
            report()
        }

        onProgress(
            ScanState.Done(
                found = found.size,
                added = plan.newFiles.size,
                updated = plan.changed.size,
                moved = plan.moved.size,
                missing = plan.missing.size,
                broken = broken.get(),
                durationMs = System.currentTimeMillis() - started,
                folders = folders,
            ),
        )
    }

    /** Reads one EPUB into a database row. Never throws: broken files get [BookEntity.parseError]. */
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
            isMissing = false,
            addedAt = addedAt,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val PARALLELISM = 4
        const val BATCH = 40
    }
}
