package com.tsundoku.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.R
import com.tsundoku.app.library.UpNextRepository
import com.tsundoku.app.ui.components.CoverImage
import kotlinx.coroutines.launch

/** Reorder or remove books in Up next. Arrows instead of dragging: precise and screen-reader friendly. */
@Composable
fun UpNextScreen(upNext: UpNextRepository, onBack: () -> Unit, onOpenBook: (Long) -> Unit) {
    val queue by upNext.queue.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.upnext_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.upnext_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            itemsIndexed(queue, key = { _, book -> book.id }) { index, book ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                    )
                    CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 36.dp, height = 52.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        book.authors?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    IconButton(onClick = { scope.launch { upNext.move(book.id, -1) } }, enabled = index > 0) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.upnext_move_up))
                    }
                    IconButton(onClick = { scope.launch { upNext.move(book.id, +1) } }, enabled = index < queue.lastIndex) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.upnext_move_down))
                    }
                    IconButton(onClick = { scope.launch { upNext.remove(book.id) } }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.upnext_remove))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
