package com.tsundoku.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.data.settings.LibraryDisplay
import com.tsundoku.app.data.settings.TileSize
import com.tsundoku.app.library.LibraryViewMode

/** Library → Display options: grid or list, tile size, full titles, which details to show. */
@Composable
fun DisplayOptionsDialog(
    view: LibraryViewMode,
    display: LibraryDisplay,
    onView: (LibraryViewMode) -> Unit,
    onDisplay: (LibraryDisplay) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.display_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Label(stringResource(R.string.display_view))
                ChoiceRow(
                    options = listOf(LibraryViewMode.GRID to R.string.display_grid, LibraryViewMode.LIST to R.string.display_list),
                    selected = view,
                    onPick = onView,
                )
                Label(stringResource(R.string.display_size))
                ChoiceRow(
                    options = listOf(
                        TileSize.SMALL to R.string.display_small,
                        TileSize.MEDIUM to R.string.display_medium,
                        TileSize.LARGE to R.string.display_large,
                    ),
                    selected = display.tileSize,
                    onPick = { onDisplay(display.copy(tileSize = it)) },
                )
                Toggle(
                    title = stringResource(R.string.display_covers_only),
                    subtitle = stringResource(R.string.display_covers_only_sub),
                    checked = display.coversOnly,
                ) { onDisplay(display.copy(coversOnly = it)) }
                Toggle(
                    title = stringResource(R.string.display_full_titles),
                    subtitle = stringResource(R.string.display_full_titles_sub),
                    checked = display.fullTitles,
                ) { onDisplay(display.copy(fullTitles = it)) }
                Toggle(stringResource(R.string.display_subtitle), null, display.showSubtitle) {
                    onDisplay(display.copy(showSubtitle = it))
                }
                Toggle(stringResource(R.string.display_status), null, display.showStatus) {
                    onDisplay(display.copy(showStatus = it))
                }
                Toggle(stringResource(R.string.display_location), null, display.showLocation) {
                    onDisplay(display.copy(showLocation = it))
                }
                Toggle(stringResource(R.string.display_epub_tags), stringResource(R.string.display_epub_tags_sub), display.showEpubTags) {
                    onDisplay(display.copy(showEpubTags = it))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.display_done)) } },
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, Int>>, selected: T, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onPick(value) },
                label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
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
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
