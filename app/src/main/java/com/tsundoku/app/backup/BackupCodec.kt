package com.tsundoku.app.backup

import org.json.JSONArray
import org.json.JSONObject

/** Everything you created in Tsundoku (not the books themselves). */
data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val books: List<BookBackup>,
    val series: List<SeriesBackup>,
)

/**
 * Reading data for one book. Books are matched back by [fingerprint] (survives renames,
 * moves and reinstalls), with [path] as a fallback.
 */
data class BookBackup(
    val fingerprint: String,
    val path: String,
    val title: String,
    val status: String?,
    val rating: Int,
    val currentChapter: Int,
    val totalChapters: Int?,
    val notes: String?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val lastOpenedAt: Long?,
    val tags: List<String>,
    val upNextPosition: Int?,
    val events: List<EventBackup>,
)

data class EventBackup(val status: String, val at: Long)

data class SeriesBackup(val key: String, val status: String)

/**
 * Reads and writes backups as plain JSON, so they stay readable and future versions of the
 * app can still restore old files. Unknown or missing fields are tolerated.
 */
object BackupCodec {

    const val VERSION = 1

    fun encode(data: BackupData): String {
        val root = JSONObject()
            .put("app", "Tsundoku")
            .put("version", data.version)
            .put("exportedAt", data.exportedAt)
        val books = JSONArray()
        data.books.forEach { b ->
            books.put(
                JSONObject()
                    .put("fingerprint", b.fingerprint)
                    .put("path", b.path)
                    .put("title", b.title)
                    .putOpt("status", b.status)
                    .put("rating", b.rating)
                    .put("currentChapter", b.currentChapter)
                    .putOpt("totalChapters", b.totalChapters)
                    .putOpt("notes", b.notes)
                    .putOpt("startedAt", b.startedAt)
                    .putOpt("finishedAt", b.finishedAt)
                    .putOpt("lastOpenedAt", b.lastOpenedAt)
                    .put("tags", JSONArray(b.tags))
                    .putOpt("upNextPosition", b.upNextPosition)
                    .put(
                        "events",
                        JSONArray().also { arr -> b.events.forEach { e -> arr.put(JSONObject().put("status", e.status).put("at", e.at)) } },
                    ),
            )
        }
        root.put("books", books)
        root.put("series", JSONArray().also { arr -> data.series.forEach { arr.put(JSONObject().put("key", it.key).put("status", it.status)) } })
        return root.toString(2)
    }

    /** @throws IllegalArgumentException if this isn't a Tsundoku backup. */
    fun decode(json: String): BackupData {
        val root = runCatching { JSONObject(json) }.getOrElse { throw IllegalArgumentException("Not a backup file") }
        require(root.optString("app") == "Tsundoku") { "Not a Tsundoku backup" }
        val books = root.optJSONArray("books") ?: JSONArray()
        val series = root.optJSONArray("series") ?: JSONArray()
        return BackupData(
            version = root.optInt("version", 1),
            exportedAt = root.optLong("exportedAt", 0),
            books = (0 until books.length()).mapNotNull { i -> books.optJSONObject(i)?.let(::bookFrom) },
            series = (0 until series.length()).mapNotNull { i ->
                series.optJSONObject(i)?.let { o ->
                    val key = o.optString("key")
                    val status = o.optString("status")
                    if (key.isNotEmpty() && status.isNotEmpty()) SeriesBackup(key, status) else null
                }
            },
        )
    }

    private fun bookFrom(o: JSONObject): BookBackup? {
        val fingerprint = o.optString("fingerprint")
        val path = o.optString("path")
        if (fingerprint.isEmpty() && path.isEmpty()) return null
        val tags = o.optJSONArray("tags") ?: JSONArray()
        val events = o.optJSONArray("events") ?: JSONArray()
        return BookBackup(
            fingerprint = fingerprint,
            path = path,
            title = o.optString("title"),
            status = o.optStringOrNull("status"),
            rating = o.optInt("rating", 0),
            currentChapter = o.optInt("currentChapter", 0),
            totalChapters = o.optIntOrNull("totalChapters"),
            notes = o.optStringOrNull("notes"),
            startedAt = o.optLongOrNull("startedAt"),
            finishedAt = o.optLongOrNull("finishedAt"),
            lastOpenedAt = o.optLongOrNull("lastOpenedAt"),
            tags = (0 until tags.length()).mapNotNull { tags.optString(it).takeIf { s -> s.isNotBlank() } },
            upNextPosition = o.optIntOrNull("upNextPosition"),
            events = (0 until events.length()).mapNotNull { i ->
                events.optJSONObject(i)?.let { e ->
                    val status = e.optString("status")
                    if (status.isNotEmpty()) EventBackup(status, e.optLong("at")) else null
                }
            },
        )
    }

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)
    private fun JSONObject.optIntOrNull(name: String): Int? = if (isNull(name)) null else optInt(name)
    private fun JSONObject.optLongOrNull(name: String): Long? = if (isNull(name)) null else optLong(name)
}
