package com.tsundoku.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.library.LibraryFilter
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.LibrarySort
import com.tsundoku.app.library.LibraryViewMode
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.library.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loaded: Boolean = false,
    /** Books after search + filter + sort. */
    val books: List<BookEntity> = emptyList(),
    val totalCount: Int = 0,
    val totalBytes: Long = 0,
    val counts: Map<LibraryFilter, Int> = emptyMap(),
    val sort: LibrarySort = LibrarySort.RECENT,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val view: LibraryViewMode = LibraryViewMode.GRID,
    val query: String = "",
    /** bookId -> status; books not in the map are "Plan to read". */
    val statuses: Map<Long, ReadingStatus> = emptyMap(),
) {
    fun statusOf(bookId: Long): ReadingStatus = statuses[bookId] ?: ReadingStatus.PLAN
}

class LibraryViewModel(
    private val library: LibraryRepository,
    private val tracking: TrackingRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchText: StateFlow<String> = query.asStateFlow()

    val scanState: StateFlow<ScanState> = library.scanState

    private val booksWithStatus = combine(library.books, tracking.observeStatuses()) { books, statuses ->
        books to statuses
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        booksWithStatus,
        query,
        preferences.librarySort,
        preferences.libraryFilter,
        preferences.libraryView,
    ) { (books, statuses), q, sort, filter, view ->
        val statusOf: (Long) -> ReadingStatus = { statuses[it] ?: ReadingStatus.PLAN }
        LibraryUiState(
            loaded = true,
            books = LibraryQuery.apply(books, q, sort, filter, statusOf),
            totalCount = books.size,
            totalBytes = books.sumOf { it.sizeBytes },
            counts = LibraryQuery.counts(books, statusOf),
            statuses = statuses,
            sort = sort,
            filter = filter,
            view = view,
            query = q,
        )
    }
        .flowOn(Dispatchers.Default) // sorting never runs on the UI thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(text: String) {
        query.value = text
    }

    fun setSort(sort: LibrarySort) {
        viewModelScope.launch { preferences.setLibrarySort(sort) }
    }

    fun setFilter(filter: LibraryFilter) {
        viewModelScope.launch { preferences.setLibraryFilter(filter) }
    }

    fun toggleView(current: LibraryViewMode) {
        val next = if (current == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
        viewModelScope.launch { preferences.setLibraryView(next) }
    }

    fun scan() {
        library.startScan()
    }
}
