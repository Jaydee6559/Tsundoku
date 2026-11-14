package com.tsundoku.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.library.BookDetail
import com.tsundoku.app.library.TrackingRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Loading state for the book screen: null detail + loaded = book no longer exists. */
data class BookDetailState(val loaded: Boolean = false, val detail: BookDetail? = null)

@OptIn(FlowPreview::class)
class BookDetailViewModel(
    private val bookId: Long,
    private val tracking: TrackingRepository,
) : ViewModel() {

    val state: StateFlow<BookDetailState> = MutableStateFlow(BookDetailState()).also { out ->
        viewModelScope.launch { tracking.observeDetail(bookId).collect { out.value = BookDetailState(true, it) } }
    }

    /** Notes are saved shortly after you stop typing (not on every key). */
    private val notesDraft = MutableStateFlow<String?>(null)
    private var lastSavedNotes: String? = null

    init {
        viewModelScope.launch {
            notesDraft.filterNotNull().debounce(NOTES_DELAY_MS).collect { save(it) }
        }
    }

    fun setStatus(status: ReadingStatus) = launch { tracking.setStatus(bookId, status) }
    fun setChapter(chapter: Int) = launch { tracking.setChapter(bookId, chapter) }
    fun setTotalChapters(total: Int?) = launch { tracking.setTotalChapters(bookId, total) }
    fun setRating(stars: Int) = launch { tracking.setRating(bookId, stars) }
    fun addTag(name: String) = launch { tracking.addTag(bookId, name) }
    fun removeTag(tagId: Long) = launch { tracking.removeTag(bookId, tagId) }
    fun markOpened() = launch { tracking.markOpened(bookId) }

    fun onNotesChanged(text: String) {
        notesDraft.value = text
    }

    private suspend fun save(notes: String) {
        if (notes == lastSavedNotes) return
        tracking.setNotes(bookId, notes)
        lastSavedNotes = notes
    }

    /** Leaving the screen within the typing delay must not lose the last words. */
    override fun onCleared() {
        val pending = notesDraft.value
        if (pending != null && pending != lastSavedNotes) tracking.saveNotesInBackground(bookId, pending)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val NOTES_DELAY_MS = 600L
    }
}
