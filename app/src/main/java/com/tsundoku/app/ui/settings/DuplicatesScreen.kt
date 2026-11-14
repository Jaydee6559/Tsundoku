package com.tsundoku.app.ui.settings

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.AppContainer
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.library.DuplicateGroup
import com.tsundoku.app.library.DuplicateRules
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.ui.components.CoverImage
import kotlinx.coroutines.launch

/** Finds the same novel stored more than once (e.g. a Download original and its ReadEra copy). */
@Composable
fun DuplicatesScreen(container: AppContainer, onBack: () -> Unit, onOpenBook: (Long) -> Unit) {
    val books by container.library.books.collectAsStateWithLifecycle(initialValue = null)
    val groups = remember(books) { books?.let { DuplicateRules.find(it) } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.dup_title), style = MaterialTheme.typography.headlineSmall)
        }
        val list = groups ?: return@Column
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dup_none), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            item {
                Text(
                    stringResource(R.string.dup_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(list, key = { g -> g.books.joinToString("-") { it.id.toString() } }) { group ->
                GroupCard(
                    group = group,
                    onOpenBook = onOpenBook,
                    onKeep = { keep ->
                        val others = group.books.filter { it.id != keep.id }.map { it.id }
                        scope.launch {
                            val moved = container.trash.moveToTrash(others)
                            Toast.makeText(context, context.getString(R.string.trash_moved, moved), Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupCard(group: DuplicateGroup, onOpenBook: (Long) -> Unit, onKeep: (BookEntity) -> Unit) {
    Section(stringResource(if (group.exact) R.string.dup_exact else R.string.dup_similar)) {
        group.books.forEachIndexed { i, book ->
            if (i > 0) Divider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 34.dp, height = 50.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val size = Formatter.formatShortFileSize(LocalContext.current, book.sizeBytes)
                    Text(
                        listOf(LibraryQuery.locationOf(book.folder), size).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        book.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    OutlinedButton(onClick = { onKeep(book) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text(stringResource(R.string.dup_keep), style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        stringResource(R.string.dup_open),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clickable { onOpenBook(book.id) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
