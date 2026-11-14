package com.tsundoku.app.ui.series

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.R
import com.tsundoku.app.data.db.SeriesStatus
import com.tsundoku.app.library.SeriesRepository
import com.tsundoku.app.library.SeriesSummary
import com.tsundoku.app.ui.theme.TsundokuTheme

private enum class SeriesFilter { ALL, ONGOING, GAPS, NEW, COMPLETED }

/** The Series tab (design PDF page 5, left). */
@Composable
fun SeriesListScreen(seriesRepo: SeriesRepository, onOpenSeries: (String) -> Unit) {
    val series by seriesRepo.series.collectAsStateWithLifecycle(initialValue = null)
    var filterName by rememberSaveable { mutableStateOf(SeriesFilter.ALL.name) }
    val filter = SeriesFilter.valueOf(filterName)

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.tab_series),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp),
        )
        val all = series ?: return@Column
        if (all.isEmpty()) {
            EmptyMessage(stringResource(R.string.series_empty_title), stringResource(R.string.series_empty_body))
            return@Column
        }

        val counts = SeriesFilter.entries.associateWith { f -> all.count { matches(it, f) } }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(SeriesFilter.entries.filter { it == SeriesFilter.ALL || it == filter || (counts[it] ?: 0) > 0 }) { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { filterName = f.name },
                    label = { Text(stringResource(f.label(), counts[f] ?: 0)) },
                    shape = MaterialTheme.shapes.large,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        }

        val shown = all.filter { matches(it, filter) }
        if (shown.isEmpty()) {
            EmptyMessage(null, stringResource(R.string.series_empty_filter))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.key }) { s -> SeriesCard(s, onClick = { onOpenSeries(s.key) }) }
            }
        }
    }
}

@Composable
private fun SeriesCard(series: SeriesSummary, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverStack(series)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    series.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val read = stringResource(R.string.series_read_of, series.completedCount, series.volumeCount)
                Text(
                    listOfNotNull(series.author, read).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (series.status != null) {
                        val (bg, ink) = seriesStatusColors(series.status)
                        Pill(stringResource(series.status.labelRes()), bg, ink)
                    }
                    missingLabel(series)?.let {
                        val s = TsundokuTheme.statusColors
                        Pill(it, s.onHoldBg, s.onHoldInk)
                    }
                    if (series.hasNew) {
                        val s = TsundokuTheme.statusColors
                        Pill(stringResource(R.string.series_new), s.completedBg, s.completedInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(title: String?, body: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun matches(s: SeriesSummary, f: SeriesFilter): Boolean = when (f) {
    SeriesFilter.ALL -> true
    SeriesFilter.ONGOING -> s.status == SeriesStatus.ONGOING
    SeriesFilter.GAPS -> s.hasGaps
    SeriesFilter.NEW -> s.hasNew
    SeriesFilter.COMPLETED -> s.status == SeriesStatus.COMPLETED
}

private fun SeriesFilter.label(): Int = when (this) {
    SeriesFilter.ALL -> R.string.series_filter_all
    SeriesFilter.ONGOING -> R.string.series_filter_ongoing
    SeriesFilter.GAPS -> R.string.series_filter_gaps
    SeriesFilter.NEW -> R.string.series_filter_new
    SeriesFilter.COMPLETED -> R.string.series_filter_completed
}
