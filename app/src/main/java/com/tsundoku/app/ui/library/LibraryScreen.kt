package com.tsundoku.app.ui.library

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tsundoku.app.R
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.library.LibraryFilter
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.LibrarySort
import com.tsundoku.app.library.LibraryViewMode
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.library.isRunning

/** The Library tab: grid or list, search, sort and filters (design PDF page 3). */
@Composable
fun LibraryScreen(library: LibraryRepository, preferences: AppPreferences) {
    val vm: LibraryViewModel = viewModel { LibraryViewModel(library, preferences) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scan by vm.scanState.collectAsStateWithLifecycle()
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = searching) {
        searching = false
        vm.setQuery("")
    }

    Column(Modifier.fillMaxSize()) {
        if (searching) {
            SearchBar(
                text = searchText,
                onTextChange = vm::setQuery,
                onClose = {
                    searching = false
                    vm.setQuery("")
                },
            )
        } else {
            TitleBar(
                view = state.view,
                sort = state.sort,
                scanRunning = scan.isRunning,
                onSearch = { searching = true },
                onSort = vm::setSort,
                onToggleView = { vm.toggleView(state.view) },
                onScan = vm::scan,
            )
        }

        FilterRow(filter = state.filter, counts = state.counts, onFilter = vm::setFilter)
        SummaryRow(state = state, scan = scan)

        when {
            !state.loaded -> Unit
            state.totalCount == 0 && !scan.isRunning -> Message(
                stringResource(R.string.library_empty_title),
                stringResource(R.string.library_empty_body),
            )
            state.books.isEmpty() && state.query.isNotBlank() -> Message(
                stringResource(R.string.library_no_results_title),
                stringResource(R.string.library_no_results_body, state.query),
            )
            state.books.isEmpty() -> Message(null, stringResource(R.string.library_no_filter_results))
            state.view == LibraryViewMode.GRID -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.books, key = { it.id }) { book -> BookGridItem(book) }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.books, key = { it.id }) { book ->
                    BookListRow(book)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ top bars

@Composable
private fun TitleBar(
    view: LibraryViewMode,
    sort: LibrarySort,
    scanRunning: Boolean,
    onSearch: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onToggleView: () -> Unit,
    onScan: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tab_library),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.library_search))
        }
        SortMenu(current = sort, onSort = onSort)
        IconButton(onClick = onToggleView) {
            if (view == LibraryViewMode.GRID) {
                Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = stringResource(R.string.library_view_list))
            } else {
                Icon(Icons.Outlined.GridView, contentDescription = stringResource(R.string.library_view_grid))
            }
        }
        if (scanRunning) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = onScan) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.library_scan_now))
            }
        }
    }
}

@Composable
private fun SortMenu(current: LibrarySort, onSort: (LibrarySort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.library_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LibrarySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    onClick = {
                        onSort(option)
                        open = false
                    },
                    trailingIcon = {
                        if (option == current) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchBar(text: String, onTextChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.library_close_search))
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.library_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.library_clear_search))
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus),
        )
    }
}

// ------------------------------------------------------------------ chips + summary

@Composable
private fun FilterRow(filter: LibraryFilter, counts: Map<LibraryFilter, Int>, onFilter: (LibraryFilter) -> Unit) {
    val visible = LibraryFilter.entries.filter { it != LibraryFilter.BROKEN || (counts[it] ?: 0) > 0 || filter == it }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(visible) { option ->
            val selected = option == filter
            FilterChip(
                selected = selected,
                onClick = { onFilter(option) },
                label = { Text(stringResource(option.label(), counts[option] ?: 0)) },
                shape = MaterialTheme.shapes.large,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    }
}

@Composable
private fun SummaryRow(state: LibraryUiState, scan: ScanState) {
    val context = LocalContext.current
    val size = remember(state.totalBytes) { Formatter.formatShortFileSize(context, state.totalBytes) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(state.sort.label()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.library_summary, state.totalCount, size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (scan) {
            is ScanState.Reading -> if (scan.total > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { scan.processed / scan.total.toFloat() }, modifier = Modifier.fillMaxWidth())
            }
            ScanState.Discovering -> {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            is ScanState.Failed -> Text(
                stringResource(R.string.scan_failed, scan.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }
    }
}

@Composable
private fun Message(title: String?, body: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun LibrarySort.label(): Int = when (this) {
    LibrarySort.RECENT -> R.string.sort_recent
    LibrarySort.TITLE -> R.string.sort_title
    LibrarySort.AUTHOR -> R.string.sort_author
    LibrarySort.SERIES -> R.string.sort_series
    LibrarySort.SIZE -> R.string.sort_size
}

private fun LibraryFilter.label(): Int = when (this) {
    LibraryFilter.ALL -> R.string.filter_all
    LibraryFilter.IN_SERIES -> R.string.filter_series
    LibraryFilter.STANDALONE -> R.string.filter_standalone
    LibraryFilter.BROKEN -> R.string.filter_broken
}

