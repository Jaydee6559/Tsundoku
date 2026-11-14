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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.data.db.ReadingStatus
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val line = seriesLabel(book) ?: book.authors
        if (line != null) {
            Text(
                line,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (book.parseError != null) BrokenLabel() else StatusDotLabel(status, Modifier.padding(top = 2.dp))
    }
}

/** One row in the list view. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListRow(
    book: BookEntity,
    status: ReadingStatus,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                modifier = Modifier.size(width = 42.dp, height = 62.dp),
            )
            if (selected) SelectedMark(Modifier.align(Alignment.Center))
        }
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
            if (book.parseError != null) BrokenLabel() else StatusDotLabel(status, Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.width(8.dp))
        LocationChip(book.folder)
    }
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
            folder.substringBefore('/').ifEmpty { "Storage" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
