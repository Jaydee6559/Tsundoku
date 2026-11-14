package com.tsundoku.app.library

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Finds every .epub on the phone using two sources:
 *  1. MediaStore — Android's own file index. One query, under a second.
 *  2. A walk through storage that skips folders that never hold books — catches anything
 *     MediaStore missed (some apps don't register their downloads).
 */
class EpubDiscovery(private val context: Context) {

    val storageRoot: File get() = Environment.getExternalStorageDirectory()

    /** @param excluded folders you chose to ignore (relative paths), skipped with everything inside. */
    fun findAll(excluded: Set<String> = emptySet()): List<FoundFile> {
        val root = storageRoot.absolutePath
        val paths = HashSet<String>()
        paths += fromMediaStore()
        paths += fromWalk(storageRoot, excluded)
        return paths
            // Never anything inside a hidden folder, above all Tsundoku's own /.tsundoku/trash.
            // The walk already skips them, but Android's file index (MediaStore) still lists
            // files there: a trashed book would otherwise be found again and re-added.
            .filterNot { WatchRules.isHiddenPath(it, root) }
            .filterNot { FolderRules.isExcluded(folderOf(it, root), excluded) }
            .mapNotNull { path ->
            val f = File(path)
            if (f.isFile && f.length() > 0) FoundFile(f.absolutePath, f.length(), f.lastModified()) else null
        }
    }

    private fun fromMediaStore(): List<String> = runCatching {
        val out = mutableListOf<String>()
        @Suppress("DEPRECATION") // DATA is the real path; fine for apps with All files access.
        val dataColumn = MediaStore.Files.FileColumns.DATA
        context.contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            arrayOf(dataColumn),
            "$dataColumn LIKE ?",
            arrayOf("%.epub"),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) cursor.getString(0)?.let(out::add)
        }
        out
    }.getOrDefault(emptyList())

    private fun fromWalk(root: File, excluded: Set<String>): List<String> {
        val out = mutableListOf<String>()
        val rootPath = root.absolutePath
        val stack = ArrayDeque<Pair<File, Int>>().apply { addLast(root to 0) }
        while (stack.isNotEmpty()) {
            val (dir, depth) = stack.removeLast()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (depth < MAX_DEPTH && !shouldSkip(child, rootPath, excluded)) stack.addLast(child to depth + 1)
                } else if (child.name.endsWith(".epub", ignoreCase = true)) {
                    out += child.absolutePath
                }
            }
        }
        return out
    }

    private fun shouldSkip(dir: File, rootPath: String, excluded: Set<String>): Boolean {
        val name = dir.name
        if (name.startsWith(".")) return true // hidden folders, incl. Tsundoku's own /.tsundoku
        if (name in SKIP_ANYWHERE) return true
        val relative = dir.absolutePath.removePrefix(rootPath).trimStart('/')
        return relative in SKIP_FROM_ROOT || FolderRules.isExcluded(relative, excluded)
    }

    companion object {
        private const val MAX_DEPTH = 16

        /** Top-level folders that never contain books (or that Android blocks anyway). */
        private val SKIP_FROM_ROOT = setOf(
            "Android/data", "Android/obb", "Android/media",
            "DCIM", "Pictures", "Movies", "Music", "Ringtones", "Alarms",
            "Notifications", "Podcasts", "Recordings", "Audiobooks",
        )

        /** Media folders apps create at any depth. */
        private val SKIP_ANYWHERE = setOf(
            "WhatsApp Images", "WhatsApp Video", "WhatsApp Audio", "WhatsApp Voice Notes",
            "WhatsApp Stickers", "WhatsApp Animated Gifs", "Telegram Images", "Telegram Video",
            "Telegram Audio", "Screenshots", "thumbnails", "Camera",
        )

        val skippedForDisplay = listOf("DCIM", "Pictures", "Movies", "Music", "Android/data")
    }
}

/** "Download/Novels" for a file at <storage>/Download/Novels/x.epub ("" at the root). */
fun folderOf(path: String, storageRoot: String): String {
    val parent = path.substringBeforeLast('/', "")
    return if (parent.startsWith(storageRoot)) parent.removePrefix(storageRoot).trimStart('/') else parent
}

/** Top-level folder used for the "Found in" summary. */
fun topFolderOf(folder: String): String = folder.substringBefore('/').ifEmpty { "Internal storage" }
