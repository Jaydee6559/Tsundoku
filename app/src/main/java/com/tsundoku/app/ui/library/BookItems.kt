package com.tsundoku.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.data.settings.LibraryDisplay
import com.tsundoku.app.data.settings.TileSize
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.ui.components.StatusDotLabel
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.theme.TsundokuTheme

/** One cover in the grid: cover, title, then author or series (design PDF page 3, middle). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookGridItem(
    book: BookEntity,
    status: ReadingStatus,
    selected: Boolean,
    display: LibraryDisplay,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = gridText(display.tileSize)
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            CoverImage(
                thumbPath = book.coverThumbPath,
                title = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            if (selected) SelectedMark(Modifier.align(Alignment.TopEnd).padding(5.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            book.title,
            fontSize = text.title,
            lineHeight = text.titleLine,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (display.fullTitles) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        val line = seriesLabel(book) ?: book.authors
        if (display.showSubtitle && line != null) {
            Text(
                line,
                fontSize = text.sub,
                lineHeight = text.subLine,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (display.fullTitles) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (book.parseError != null) {
            BrokenLabel()
        } else if (display.showStatus) {
            StatusDotLabel(status, Modifier.padding(top = 2.dp))
        }
    }
}

/** One row in the list view. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListRow(
    book: BookEntity,
    status: ReadingStatus,
    selected: Boolean,
    display: LibraryDisplay,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (coverW, coverH) = listCover(display.tileSize)
    Row(
        modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            CoverImage(
                thumbPath = book.coverThumbPath,
                title = book.title,
                modifier = Modifier.size(width = coverW, height = coverH),
            )
            if (selected) SelectedMark(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                book.title,
                style = if (display.tileSize == TileSize.SMALL) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (display.fullTitles) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(book.authors, seriesLabel(book)).joinToString(" · ")
            if (display.showSubtitle && subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (book.parseError != null) {
                BrokenLabel()
            } else if (display.showStatus) {
                StatusDotLabel(status, Modifier.padding(top = 3.dp))
            }
        }
        if (display.showLocation) {
            Spacer(Modifier.width(8.dp))
            LocationChip(book.folder)
        }
    }
}

/** Grid column width for each tile size (the grid fits as many columns as it can). */
fun gridTileWidth(size: TileSize) = when (size) {
    TileSize.SMALL -> 96.dp
    TileSize.MEDIUM -> 128.dp
    TileSize.LARGE -> 170.dp
}

private data class GridText(val title: TextUnit, val titleLine: TextUnit, val sub: TextUnit, val subLine: TextUnit)

private fun gridText(size: TileSize) = when (size) {
    TileSize.SMALL -> GridText(12.sp, 15.sp, 11.sp, 14.sp)
    TileSize.MEDIUM -> GridText(14.sp, 18.sp, 12.sp, 16.sp)
    TileSize.LARGE -> GridText(16.sp, 20.sp, 13.sp, 17.sp)
}

private fun listCover(size: TileSize): Pair<Dp, Dp> = when (size) {
    TileSize.SMALL -> 36.dp to 54.dp
    TileSize.MEDIUM -> 50.dp to 75.dp
    TileSize.LARGE -> 66.dp to 99.dp
}

/** Check mark on a selected book. */
@Composable
private fun SelectedMark(modifier: Modifier = Modifier) {
    Icon(
        Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .size(24.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(2.dp),
    )
}

@Composable
private fun BrokenLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = TsundokuTheme.statusColors.droppedInk,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.library_broken),
            fontSize = 11.sp,
            color = TsundokuTheme.statusColors.droppedInk,
            maxLines = 1,
        )
    }
}

/** "Lantern Saga · Vol 2", or just the series name, or null. */
@Composable
fun seriesLabel(book: BookEntity): String? {
    val name = book.seriesName ?: return null
    val index = book.seriesIndex ?: return name
    val number = if (index % 1.0 == 0.0) index.toLong().toString() else index.toString()
    return "$name · ${stringResource(R.string.library_vol, number)}"
}

@Composable
private fun LocationChip(folder: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            LibraryQuery.locationOf(folder),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
