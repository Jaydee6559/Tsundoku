package com.tsundoku.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.data.settings.LibraryDisplay
import com.tsundoku.app.library.LibraryFilter
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.LibrarySort
import com.tsundoku.app.library.LibraryViewMode
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrashRepository
import com.tsundoku.app.library.UpNextRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
    /** Chosen location, or null for everywhere. */
    val location: String? = null,
    /** Chosen tag, or null for all books. */
    val tag: String? = null,
    /** All tags (EPUB + yours) with book counts, for the tag menu. */
    val tags: List<Pair<String, Int>> = emptyList(),
    /** All locations with book counts, for the location menu. */
    val locations: List<Pair<String, Int>> = emptyList(),
) {
    fun statusOf(bookId: Long): ReadingStatus = statuses[bookId] ?: ReadingStatus.PLAN
}

class LibraryViewModel(
    private val library: LibraryRepository,
    private val tracking: TrackingRepository,
    private val trash: TrashRepository,
    private val upNext: UpNextRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    /** One-off messages for the snackbar (e.g. "Moved 3 to Trash · Undo"). */
    sealed interface Event {
        data class Trashed(val ids: List<Long>, val count: Int) : Event
        data class Restored(val count: Int) : Event
        data class AddedToUpNext(val count: Int) : Event
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /** Books picked with long-press. Empty = not in selection mode. */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    private val query = MutableStateFlow("")
    val searchText: StateFlow<String> = query.asStateFlow()

    val scanState: StateFlow<ScanState> = library.scanState

    private data class LibraryData(
        val books: List<BookEntity>,
        val statuses: Map<Long, ReadingStatus>,
        val userTags: Map<Long, List<String>>,
    )

    private val booksWithStatus = combine(
        library.books,
        tracking.observeStatuses(),
        tracking.observeUserTags(),
    ) { books, statuses, userTags -> LibraryData(books, statuses, userTags) }

    private data class ViewPrefs(
        val sort: LibrarySort,
        val filter: LibraryFilter,
        val view: LibraryViewMode,
        val location: String?,
        val tag: String?,
    )

    private val viewPrefs = combine(
        preferences.librarySort,
        preferences.libraryFilter,
        preferences.libraryView,
        preferences.libraryLocation,
        preferences.libraryTag,
    ) { sort, filter, view, location, tag -> ViewPrefs(sort, filter, view, location, tag) }

    val uiState: StateFlow<LibraryUiState> = combine(
        booksWithStatus,
        query,
        viewPrefs,
    ) { (books, statuses, userTags), q, prefs ->
        val statusOf: (Long) -> ReadingStatus = { statuses[it] ?: ReadingStatus.PLAN }
        val userTagsOf: (Long) -> List<String> = { userTags[it].orEmpty() }
        val locations = LibraryQuery.locations(books)
        // A remembered location that no longer has books falls back to everywhere.
        val location = prefs.location?.takeIf { loc -> locations.any { it.first == loc } }
        val inLocation = LibraryQuery.inLocation(books, location)
        // Tag menu lists the tags found in the chosen location.
        val tags = LibraryQuery.tagCounts(inLocation, userTagsOf)
        val tag = prefs.tag?.takeIf { t -> tags.any { it.first.equals(t, ignoreCase = true) } }
        val here = LibraryQuery.withTag(inLocation, tag, userTagsOf)
        val sort = prefs.sort
        val filter = prefs.filter
        val view = prefs.view
        LibraryUiState(
            loaded = true,
            books = LibraryQuery.apply(here, q, sort, filter, statusOf, userTagsOf),
            totalCount = here.size,
            totalBytes = here.sumOf { it.sizeBytes },
            counts = LibraryQuery.counts(here, statusOf),
            statuses = statuses,
            location = location,
            locations = locations,
            tag = tag,
            tags = tags,
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

    /** How tiles look (size, full titles, which details show). */
    val display: StateFlow<LibraryDisplay> =
        preferences.libraryDisplay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryDisplay())

    fun setDisplay(display: LibraryDisplay) {
        viewModelScope.launch { preferences.setLibraryDisplay(display) }
    }

    fun setTag(tag: String?) {
        viewModelScope.launch { preferences.setLibraryTag(tag) }
    }

    fun setLocation(location: String?) {
        viewModelScope.launch { preferences.setLibraryLocation(location) }
    }

    fun toggleView(current: LibraryViewMode) {
        val next = if (current == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
        viewModelScope.launch { preferences.setLibraryView(next) }
    }

    fun scan() {
        library.startScan()
    }

    // ------------------------------------------------------------------ selection

    fun toggleSelected(id: Long) {
        _selected.update { if (id in it) it - id else it + id }
    }

    fun selectAll(ids: List<Long>) {
        _selected.value = ids.toSet()
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    // ------------------------------------------------------------------ actions (one or many books)

    fun moveToTrash(ids: Collection<Long>) {
        val list = ids.toList()
        clearSelection()
        viewModelScope.launch {
            val moved = trash.moveToTrash(list)
            if (moved > 0) _events.send(Event.Trashed(list, moved))
        }
    }

    fun undoTrash(ids: List<Long>) {
        viewModelScope.launch {
            val restored = trash.restore(ids)
            if (restored > 0) _events.send(Event.Restored(restored))
        }
    }

    fun setStatus(ids: Collection<Long>, status: ReadingStatus) {
        val list = ids.toList()
        clearSelection()
        viewModelScope.launch { list.forEach { tracking.setStatus(it, status) } }
    }

    fun addToUpNext(ids: Collection<Long>) {
        val list = ids.toList()
        clearSelection()
        viewModelScope.launch {
            upNext.add(list)
            _events.send(Event.AddedToUpNext(list.size))
        }
    }

    fun addTag(ids: Collection<Long>, name: String) {
        val list = ids.toList()
        clearSelection()
        viewModelScope.launch { list.forEach { tracking.addTag(it, name) } }
    }
}
