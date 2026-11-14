package com.tsundoku.app.library

data class FolderCount(val name: String, val count: Int)

/** Live progress of a library scan, shown on the first-scan screen and the Library tab. */
sealed interface ScanState {
    data object Idle : ScanState

    /** Looking through storage for .epub files. */
    data object Discovering : ScanState

    /** Reading titles and covers of new or changed books. */
    data class Reading(
        val found: Int,
        val processed: Int,
        val total: Int,
        val folders: List<FolderCount>,
    ) : ScanState

    data class Done(
        val found: Int,
        val added: Int,
        val updated: Int,
        val moved: Int,
        val missing: Int,
        val broken: Int,
        val durationMs: Long,
        val folders: List<FolderCount>,
        /** Titles of the first few new books (for the "novels added" notification). */
        val addedTitles: List<String> = emptyList(),
    ) : ScanState

    data class Failed(val message: String) : ScanState
}

val ScanState.isRunning: Boolean get() = this is ScanState.Discovering || this is ScanState.Reading
