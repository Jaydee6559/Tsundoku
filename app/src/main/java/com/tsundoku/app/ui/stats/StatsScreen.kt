package com.tsundoku.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.AppContainer
import com.tsundoku.app.R
import com.tsundoku.app.library.StatsRules
import com.tsundoku.app.library.YearStats
import java.text.DateFormatSymbols

/** Reading stats and your year in reading. Everything is worked out on the phone. */
@Composable
fun StatsScreen(container: AppContainer, onBack: () -> Unit) {
    val books by container.library.books.collectAsStateWithLifecycle(initialValue = emptyList())
    val tracking by container.tracking.observeAllTracking().collectAsStateWithLifecycle(initialValue = emptyList())
    val userTags by container.tracking.observeUserTags().collectAsStateWithLifecycle(initialValue = emptyMap())

    val now = remember { System.currentTimeMillis() }
    val years = remember(tracking) { StatsRules.years(tracking, now) }
    var year by rememberSaveable { mutableIntStateOf(StatsRules.yearOf(now)) }
    val overall = remember(books, tracking) { StatsRules.overall(books, tracking) }
    val stats = remember(year, books, tracking, userTags) { StatsRules.year(year, books, tracking, { userTags[it].orEmpty() }) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // ---- library right now
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(overall.reading.toString(), stringResource(R.string.stats_reading_now), Modifier.weight(1f))
                Tile(overall.plan.toString(), stringResource(R.string.stats_in_pile), Modifier.weight(1f))
                Tile(overall.completed.toString(), stringResource(R.string.stats_completed), Modifier.weight(1f))
            }

            // ---- year picker
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                val index = years.indexOf(year)
                IconButton(onClick = { years.getOrNull(index + 1)?.let { year = it } }, enabled = index in 0 until years.lastIndex) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.stats_prev_year))
                }
                Text(
                    stringResource(R.string.stats_your_year, year),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { years.getOrNull(index - 1)?.let { year = it } }, enabled = index > 0) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.stats_next_year))
                }
            }

            YearCard(stats)
            Spacer(Modifier.height(16.dp))
            MonthChart(stats.perMonth)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(stats.started.toString(), stringResource(R.string.stats_started), Modifier.weight(1f))
                Tile(
                    stats.averageDaysToFinish?.toString() ?: "–",
                    stringResource(R.string.stats_avg_days),
                    Modifier.weight(1f),
                )
            }
            if (stats.finishedTitles.isNotEmpty()) {
                Text(
                    stringResource(R.string.stats_finished_list),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
                )
                stats.finishedTitles.take(MAX_TITLES).forEach {
                    Text("·  $it", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (stats.finishedTitles.size > MAX_TITLES) {
                    Text(
                        stringResource(R.string.stats_and_more, stats.finishedTitles.size - MAX_TITLES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.stats_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            )
        }
    }
}

private const val MAX_TITLES = 20

/** The "wrap-up" card: big number plus your top author and tags. */
@Composable
private fun YearCard(stats: YearStats) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stats.finished.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(R.string.stats_books_finished, stats.year),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            stats.topAuthor?.let {
                Text(
                    stringResource(R.string.stats_top_author, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (stats.topTags.isNotEmpty()) {
                Text(
                    stringResource(R.string.stats_top_tags, stats.topTags.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Simple bar per month (no chart library needed). */
@Composable
private fun MonthChart(perMonth: List<Int>) {
    val max = (perMonth.maxOrNull() ?: 0).coerceAtLeast(1)
    val months = remember { DateFormatSymbols.getInstance().shortMonths.take(12) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.stats_per_month), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                perMonth.forEachIndexed { i, count ->
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        if (count > 0) {
                            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((70f * count / max).dp.coerceAtLeast(2.dp))
                                .background(
                                    if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.extraSmall,
                                ),
                        )
                        Text(months[i].take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
