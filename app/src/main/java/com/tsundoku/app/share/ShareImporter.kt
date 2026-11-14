package com.tsundoku.app.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.tsundoku.app.data.db.BookDao
import com.tsundoku.app.data.settings.AppPreferences
import kotlinx.coroutines.flow.first
import com.tsundoku.app.epub.EpubParser
import com.tsundoku.app.epub.EpubResult
import com.tsundoku.app.epub.FileFingerprint
import com.tsundoku.app.epub.FilenameParser
import com.tsundoku.app.library.HomeRules
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.TrashRules
import com.tsundoku.app.library.UpNextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** A shared EPUB, copied to a private temp file and read, waiting for "Add to library". */
data class PreparedBook(
    val tempFile: File,
    val fileName: String,
    val title: String,
    val author: String?,
    val cover: Bitmap?,
    val alreadyInLibrary: Boolean,
)

/**
 * Handles "Share → Tsundoku" (from Chrome, Telegram, a file manager...).
 *
 * The shared file is untrusted, so: it's copied into the app's private cache first (size-capped),
 * read with the hardened EPUB parser, given a safe file name, and only then saved into
 * shared storage, never overwriting anything.
 */
class ShareImporter(
    private val context: Context,
    private val dao: BookDao,
    private val library: LibraryRepository,
    private val upNext: UpNextRepository,
    private val preferences: AppPreferences,
) {
    sealed interface Prepare {
        data class Ready(val book: PreparedBook) : Prepare
        data class Failed(val reason: FailReason, val detail: String? = null) : Prepare
    }

    enum class FailReason { NOT_EPUB, TOO_BIG, UNREADABLE }

    /** Where shared books are saved (Settings → Shared books folder), relative to internal storage. */
    suspend fun saveFolderLabel(): String = preferences.shareFolder.first()

    private suspend fun saveFolder(): File = File(Environment.getExternalStorageDirectory(), saveFolderLabel())

    suspend fun prepare(uri: Uri): Prepare = withContext(Dispatchers.IO) {
        val name = HomeRules.safeFileName(displayName(uri))
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // leftovers from an earlier, cancelled share
        val temp = File(dir, "${UUID.randomUUID()}.epub")

        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return@withContext Prepare.Failed(FailReason.TOO_BIG).also { temp.delete() }
                        out.write(buffer, 0, n)
                    }
                }
                true
            } ?: false
        }.getOrElse { false }
        if (!copied) {
            temp.delete()
            return@withContext Prepare.Failed(FailReason.UNREADABLE)
        }

        when (val result = EpubParser.parse(temp)) {
            is EpubResult.Failure -> {
                temp.delete()
                Prepare.Failed(FailReason.NOT_EPUB, result.reason)
            }
            is EpubResult.Success -> {
                val meta = result.metadata
                val cover = meta.coverPath?.let { EpubParser.readCover(temp, it) }?.let { decodePreview(it) }
                val duplicate = runCatching { dao.countWithFingerprint(FileFingerprint.compute(temp)) > 0 }.getOrDefault(false)
                Prepare.Ready(
                    PreparedBook(
                        tempFile = temp,
                        fileName = name,
                        title = meta.title?.takeIf { it.isNotBlank() } ?: FilenameParser.parse(name).title,
                        author = meta.authors.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        cover = cover,
                        alreadyInLibrary = duplicate,
                    ),
                )
            }
        }
    }

    /** Saves the book into [saveFolder], adds it to the library (and Up next). Returns its path. */
    suspend fun import(book: PreparedBook, addToUpNext: Boolean): String? = withContext(Dispatchers.IO) {
        runCatching {
            val folder = saveFolder()
            folder.mkdirs()
            val target = File(TrashRules.restoreTarget(File(folder, book.fileName).path) { File(it).exists() })
            book.tempFile.copyTo(target, overwrite = false)
            book.tempFile.delete()
            library.syncPaths(setOf(target.absolutePath)) // reads it and adds it right away
            if (addToUpNext) dao.idForPath(target.absolutePath)?.let { upNext.add(listOf(it)) }
            target.absolutePath
        }.getOrNull()
    }

    fun discard(book: PreparedBook) {
        book.tempFile.delete()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private fun decodePreview(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= PREVIEW_WIDTH) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private companion object {
        const val MAX_BYTES = 200L * 1024 * 1024
        const val PREVIEW_WIDTH = 240
    }
}
