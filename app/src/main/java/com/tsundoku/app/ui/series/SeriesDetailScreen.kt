package com.tsundoku.app.ui.series

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.db.SeriesStatus
import com.tsundoku.app.library.SeriesEntry
import com.tsundoku.app.library.SeriesGrouper
import com.tsundoku.app.library.SeriesRepository
import com.tsundoku.app.library.SeriesSummary
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.components.StatusDotLabel
import com.tsundoku.app.ui.theme.TsundokuTheme
import kotlinx.coroutines.launch

/** One series: volumes in order, gaps shown as placeholders (design PDF page 5, middle). */
@Composable
fun SeriesDetailScreen(
    seriesKey: String,
    seriesRepo: SeriesRepository,
    tracking: TrackingRepository,
    onBack: () -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    val flow = remember(seriesKey) { seriesRepo.observeSeries(seriesKey) }
    val series by flow.collectAsStateWithLifecycle(initialValue = null)
    val statuses by tracking.observeStatuses().collectAsStateWithLifecycle(initialValue = emptyMap())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
        }
        val s = series
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.series_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            item {
                Header(s, onStatus = { status -> scope.launch { seriesRepo.setStatus(s.key, status) } })
            }
            items(SeriesGrouper.entries(s), key = { entry ->
                when (entry) {
                    is SeriesEntry.Book -> "b${entry.book.id}"
                    is SeriesEntry.Missing -> "m${entry.volume}"
                }
            }) { entry ->
                when (entry) {
                    is SeriesEntry.Book -> VolumeRow(
                        book = entry.book,
                        status = statuses[entry.book.id] ?: ReadingStatus.PLAN,
                        isNew = entry.book.id in s.newBookIds,
                        onClick = { onOpenBook(entry.book.id) },
                    )
                    is SeriesEntry.Missing -> MissingRow(entry.volume)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun Header(s: SeriesSummary, onStatus: (SeriesStatus?) -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(s.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            listOfNotNull(s.author, stringResource(R.string.series_volumes, s.volumeCount)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusMenu(current = s.status, onStatus = onStatus)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    stringResource(R.string.series_read_of, s.completedCount, s.volumeCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (s.volumeCount == 0) 0f else s.completedCount / s.volumeCount.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatusMenu(current: SeriesStatus?, onStatus: (SeriesStatus?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val (bg, ink) = seriesStatusColors(current)
    Box {
        Surface(shape = MaterialTheme.shapes.large, color = bg, modifier = Modifier.clickable { open = true }) {
            Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(current.labelRes()), color = ink, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = ink)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (SeriesStatus.entries + listOf<SeriesStatus?>(null)).forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(if (option == null) R.string.series_status_clear else option.labelRes())) },
                    onClick = {
                        onStatus(option)
                        open = false
                    },
                    trailingIcon = {
                        if (option == current) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                )
            }
        }
    }
}

@Composable
private fun VolumeRow(book: BookEntity, status: ReadingStatus, isNew: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 40.dp, height = 58.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                book.seriesIndex?.let { volumeLabel(it) } ?: book.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                book.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusDotLabel(status, Modifier.padding(top = 2.dp))
        }
        if (isNew) {
            val c = TsundokuTheme.statusColors
            Pill(stringResource(R.string.series_new), c.completedBg, c.completedInk)
        }
    }
}

@Composable
private fun MissingRow(volume: Int) {
    val ink = TsundokuTheme.statusColors.onHoldInk
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 40.dp, height = 58.dp)
                .border(BorderStroke(1.5.dp, ink), MaterialTheme.shapes.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text("?", color = ink, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.series_missing_title, volume), style = MaterialTheme.typography.titleSmall, color = ink)
            Text(stringResource(R.string.series_missing_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun volumeLabel(index: Double): String {
    val number = if (index % 1.0 == 0.0) index.toLong().toString() else index.toString()
    return stringResource(R.string.library_vol, number)
}

