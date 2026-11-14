package com.tsundoku.app.ui.library

import android.text.format.Formatter
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.library.isRunning
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.theme.TsundokuTheme

/**
 * Basic library list (Batch 5). The full grid, search, sorting and filters arrive in Batch 6.
 */
@Composable
fun LibraryScreen(library: LibraryRepository) {
    val books by library.books.collectAsStateWithLifecycle(initialValue = emptyList())
    val scan by library.scanState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val totalSize = remember(books) { Formatter.formatShortFileSize(context, books.sumOf { it.sizeBytes }) }

    Column(Modifier.fillMaxSize()) {
        // ---- title row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tab_library),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (scan.isRunning) {
                CircularProgressIndicator(Modifier.size(22.dp).padding(2.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(13.dp))
            } else {
                IconButton(onClick = library::startScan) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.library_scan_now))
                }
            }
        }

        // ---- summary + progress
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.library_summary, books.size, totalSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val s = scan
            when {
                s is ScanState.Reading && s.total > 0 -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { s.processed / s.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                s is ScanState.Discovering -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                s is ScanState.Done && (s.added + s.updated + s.moved + s.missing) > 0 -> Text(
                    stringResource(R.string.library_last_scan, s.added, s.updated, s.moved, s.missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                s is ScanState.Failed -> Text(
                    stringResource(R.string.scan_failed, s.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (books.isEmpty() && !scan.isRunning) {
            EmptyLibrary()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(books, key = { it.id }) { book ->
                    BookRow(book)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BookRow(book: BookEntity) {
    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(
            thumbPath = book.coverThumbPath,
            title = book.title,
            modifier = Modifier.size(width = 42.dp, height = 62.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(book.authors, seriesLabel(book)).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (book.parseError != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = TsundokuTheme.statusColors.droppedInk,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.library_broken),
                        style = MaterialTheme.typography.labelSmall,
                        color = TsundokuTheme.statusColors.droppedInk,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        LocationChip(book.folder)
    }
}

@Composable
private fun seriesLabel(book: BookEntity): String? {
    val name = book.seriesName ?: return null
    val index = book.seriesIndex ?: return name
    val number = if (index % 1.0 == 0.0) index.toLong().toString() else index.toString()
    return "$name · ${stringResource(R.string.library_vol, number)}"
}

@Composable
private fun LocationChip(folder: String) {
    val label = folder.substringBefore('/').ifEmpty { "Storage" }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.library_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.library_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
