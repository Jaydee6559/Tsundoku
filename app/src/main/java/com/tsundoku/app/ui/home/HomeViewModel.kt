package com.tsundoku.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.TrackingEntity
import com.tsundoku.app.library.HomeRules
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrackingRules
import com.tsundoku.app.library.UpNextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContinueCard(val book: BookEntity, val tracking: TrackingEntity, val totalChapters: Int)

data class HomeState(
    val loaded: Boolean = false,
    val counts: HomeRules.Counts = HomeRules.Counts(0, 0, 0),
    val continueReading: ContinueCard? = null,
    val upNext: List<BookEntity> = emptyList(),
    val recent: List<BookEntity> = emptyList(),
)

class HomeViewModel(
    library: LibraryRepository,
    private val tracking: TrackingRepository,
    private val upNext: UpNextRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        library.books,
        tracking.observeAllTracking(),
        upNext.queue,
    ) { books, trackingRows, queue ->
        val statuses = trackingRows.associate { it.bookId to it.status }
        HomeState(
            loaded = true,
            counts = HomeRules.counts(books, statuses),
            continueReading = HomeRules.continueReading(books, trackingRows)?.let { (book, t) ->
                ContinueCard(book, t, TrackingRules.totalChapters(t, book.spineCount))
            },
            upNext = queue,
            recent = books.sortedByDescending { it.addedAt }.take(RECENT),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun markOpened(bookId: Long) {
        viewModelScope.launch { tracking.markOpened(bookId) }
    }

    /** "Start" the first book in Up next: it becomes Reading and leaves the queue. */
    fun startNext(bookId: Long) {
        viewModelScope.launch {
            tracking.setStatus(bookId, com.tsundoku.app.data.db.ReadingStatus.READING)
            upNext.remove(bookId)
        }
    }

    private companion object {
        const val RECENT = 5
    }
}
