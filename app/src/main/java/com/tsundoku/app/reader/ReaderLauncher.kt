package com.tsundoku.app.reader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens a book in ReadEra (Pro first, then the free version), or in any reader as a fallback.
 *
 * Android doesn't allow sharing raw file paths between apps, so the file is passed through
 * a FileProvider "content://" link with READ-only permission for that one file.
 */
object ReaderLauncher {

    private val READERA_PACKAGES = listOf("org.readera.premium", "org.readera")
    private const val EPUB_MIME = "application/epub+zip"

    enum class Result { OPENED_READERA, OPENED_OTHER, FILE_MISSING, NO_READER }

    fun open(context: Context, path: String): Result {
        val file = File(path)
        if (!file.exists()) return Result.FILE_MISSING

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        } catch (e: IllegalArgumentException) {
            return Result.NO_READER // file outside shared storage (shouldn't happen)
        }

        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, EPUB_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        for (pkg in READERA_PACKAGES) {
            try {
                context.startActivity(Intent(view).setPackage(pkg))
                return Result.OPENED_READERA
            } catch (_: ActivityNotFoundException) {
                // not installed, try the next one
            }
        }

        return try {
            context.startActivity(Intent.createChooser(view, null))
            Result.OPENED_OTHER
        } catch (_: ActivityNotFoundException) {
            Result.NO_READER
        }
    }
}
