package com.tsundoku.app.ui.trash

import android.text.format.Formatter
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.R
import com.tsundoku.app.data.db.BookEntity
import com.tsundoku.app.library.TrashRepository
import com.tsundoku.app.library.TrashRules
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.theme.TsundokuTheme
import kotlinx.coroutines.launch

/** Trash: restore books, or delete them for good (design PDF page 5, right). */
@Composable
fun TrashScreen(trash: TrashRepository, onBack: () -> Unit) {
    val books by trash.trashed.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var confirmOneId by rememberSaveable { mutableStateOf<Long?>(null) }
    val now = remember { System.currentTimeMillis() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(
                stringResource(R.string.trash_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (books.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) {
                    Text(stringResource(R.string.trash_empty_action), color = TsundokuTheme.statusColors.droppedInk)
                }
            }
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.trash_banner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.trash_empty_list), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val size = remember(books) { Formatter.formatShortFileSize(context, books.sumOf { it.sizeBytes }) }
            Text(
                stringResource(R.string.trash_summary, books.size, size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(books, key = { it.id }) { book ->
                    TrashRow(
                        book = book,
                        now = now,
                        onRestore = { scope.launch { trash.restore(listOf(book.id)) } },
                        onDelete = { confirmOneId = book.id },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = stringResource(R.string.trash_confirm_empty_title),
            body = stringResource(R.string.trash_confirm_empty_body, books.size),
            confirm = stringResource(R.string.trash_empty_action),
            onConfirm = {
                confirmEmpty = false
                scope.launch { trash.emptyTrash() }
            },
            onDismiss = { confirmEmpty = false },
        )
    }
    confirmOneId?.let { id ->
        val book = books.firstOrNull { it.id == id }
        if (book != null) {
            ConfirmDialog(
                title = stringResource(R.string.trash_confirm_one_title),
                body = stringResource(R.string.trash_confirm_one_body, book.title),
                confirm = stringResource(R.string.trash_delete_forever),
                onConfirm = {
                    confirmOneId = null
                    scope.launch { trash.deleteForever(listOf(id)) }
                },
                onDismiss = { confirmOneId = null },
            )
        }
    }
}

@Composable
private fun TrashRow(book: BookEntity, now: Long, onRestore: () -> Unit, onDelete: () -> Unit) {
    val daysLeft = TrashRules.daysLeft(book.trashedAt ?: now, now)
    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 40.dp, height = 58.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (daysLeft == 0) stringResource(R.string.trash_last_day) else stringResource(R.string.trash_days_left, daysLeft),
                style = MaterialTheme.typography.bodySmall,
                color = if (daysLeft <= 3) TsundokuTheme.statusColors.droppedInk else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.trash_restore), style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.DeleteForever,
                contentDescription = stringResource(R.string.trash_delete_forever),
                tint = TsundokuTheme.statusColors.droppedInk,
            )
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = TsundokuTheme.statusColors.droppedInk) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

