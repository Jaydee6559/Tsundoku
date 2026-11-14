package com.tsundoku.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrackingRules
import com.tsundoku.app.library.UpNextRepository
import com.tsundoku.app.reader.ReaderLauncher
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.library.seriesLabel

/** Home: continue reading, Up next, recently added (design PDF page 3, left). */
@Composable
fun HomeScreen(
    library: LibraryRepository,
    tracking: TrackingRepository,
    upNext: UpNextRepository,
    onOpenBook: (Long) -> Unit,
    onEditUpNext: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val vm: HomeViewModel = viewModel { HomeViewModel(library, tracking, upNext) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val openInReader: (BookEntity) -> Unit = { book ->
        when (ReaderLauncher.open(context, book.path)) {
            ReaderLauncher.Result.OPENED_READERA, ReaderLauncher.Result.OPENED_OTHER -> vm.markOpened(book.id)
            ReaderLauncher.Result.FILE_MISSING -> Toast.makeText(context, R.string.open_missing, Toast.LENGTH_LONG).show()
            ReaderLauncher.Result.NO_READER -> Toast.makeText(context, R.string.open_no_reader, Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                stringResource(R.string.home_counts, state.counts.reading, state.counts.plan, state.counts.completed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }

        item {
            val card = state.continueReading
            if (card != null) {
                ContinueCardView(card, onOpenBook = { onOpenBook(card.book.id) }, onRead = { openInReader(card.book) })
            } else if (state.loaded) {
                NothingInProgress(
                    next = state.upNext.firstOrNull(),
                    onStart = { book ->
                        vm.startNext(book.id)
                        openInReader(book)
                    },
                )
            }
        }

        item {
            SectionTitle(stringResource(R.string.home_up_next), action = stringResource(R.string.home_edit), onAction = onEditUpNext)
            if (state.upNext.isEmpty()) {
                Text(
                    stringResource(R.string.home_up_next_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.upNext, key = { it.id }) { book ->
                        Column(Modifier.width(64.dp).clickable { onOpenBook(book.id) }) {
                            CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 64.dp, height = 96.dp))
                            Text(
                                book.title,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.home_recent), action = stringResource(R.string.home_see_all), onAction = onOpenLibrary) }
        items(state.recent, key = { it.id }) { book ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBook(book.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 32.dp, height = 46.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(LibraryQuery.locationOf(book.folder), book.authors).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ContinueCardView(card: ContinueCard, onOpenBook: () -> Unit, onRead: () -> Unit) {
    val book = card.book
    val t = card.tracking
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenBook),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 66.dp, height = 98.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_continue),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    seriesLabel(book)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (card.totalChapters > 0) {
                            stringResource(R.string.home_chapter_of, t.currentChapter, card.totalChapters)
                        } else {
                            stringResource(R.string.home_chapter, t.currentChapter)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (card.totalChapters > 0) {
                        LinearProgressIndicator(
                            progress = { TrackingRules.progress(t.currentChapter, card.totalChapters) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRead, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_open_readera))
            }
        }
    }
}

@Composable
private fun NothingInProgress(next: BookEntity?, onStart: (BookEntity) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_nothing_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.home_nothing_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (next != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { onStart(next) }, shape = MaterialTheme.shapes.large) {
                    Text(stringResource(R.string.home_start_next, next.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(
            action,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
        )
    }
}
