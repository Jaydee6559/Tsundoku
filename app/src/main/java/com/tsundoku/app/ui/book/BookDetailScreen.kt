package com.tsundoku.app.ui.book

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tsundoku.app.R
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.library.BookDetail
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrackingRules
import com.tsundoku.app.library.TrashRepository
import com.tsundoku.app.library.UpNextRepository
import kotlinx.coroutines.launch
import com.tsundoku.app.reader.ReaderLauncher
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.components.labelRes
import com.tsundoku.app.ui.components.statusColors
import com.tsundoku.app.ui.library.seriesLabel
import com.tsundoku.app.ui.theme.TsundokuTheme
import java.text.DateFormat
import java.util.Date

/** One book: Open in ReadEra, status, progress, rating, tags, notes (design PDF page 4). */
@Composable
fun BookDetailScreen(
    bookId: Long,
    tracking: TrackingRepository,
    trash: TrashRepository,
    upNext: UpNextRepository,
    onBack: () -> Unit,
) {
    val queued by upNext.ids.collectAsStateWithLifecycle(initialValue = emptySet())
    val upNextScope = rememberCoroutineScope()
    val vm: BookDetailViewModel = viewModel(key = "book-$bookId") { BookDetailViewModel(bookId, tracking) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Spacer(Modifier.weight(1f))
            if (state.detail?.book?.trashedAt == null && state.detail != null) {
                IconButton(onClick = {
                    trash.moveToTrashInBackground(listOf(bookId))
                    Toast.makeText(context, context.getString(R.string.trash_moved, 1), Toast.LENGTH_SHORT).show()
                    onBack()
                }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.detail_delete))
                }
            }
        }
        val detail = state.detail
        when {
            !state.loaded -> Unit
            detail == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> DetailContent(
                d = detail,
                vm = vm,
                inUpNext = bookId in queued,
                onToggleUpNext = {
                    upNextScope.launch { if (bookId in queued) upNext.remove(bookId) else upNext.add(listOf(bookId)) }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    d: BookDetail,
    vm: BookDetailViewModel,
    inUpNext: Boolean,
    onToggleUpNext: () -> Unit,
) {
    val context = LocalContext.current
    val book = d.book
    val t = d.tracking
    val total = d.totalChapters
    var showTotalDialog by rememberSaveable { mutableStateOf(false) }
    var showTagDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // ---- header
        Row {
            CoverImage(book.coverThumbPath, book.title, Modifier.size(width = 96.dp, height = 144.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                book.authors?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                seriesLabel(book)?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(6.dp))
                Stars(rating = t.rating, onRate = vm::setRating)
                if (book.spineCount > 0) {
                    Text(
                        stringResource(R.string.detail_chapters, book.spineCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                when (ReaderLauncher.open(context, book.path)) {
                    ReaderLauncher.Result.OPENED_READERA, ReaderLauncher.Result.OPENED_OTHER -> vm.markOpened()
                    ReaderLauncher.Result.FILE_MISSING ->
                        Toast.makeText(context, R.string.open_missing, Toast.LENGTH_LONG).show()
                    ReaderLauncher.Result.NO_READER ->
                        Toast.makeText(context, R.string.open_no_reader, Toast.LENGTH_LONG).show()
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.detail_open_readera), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onToggleUpNext,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (inUpNext) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (inUpNext) R.string.upnext_added else R.string.upnext_add))
        }

        // ---- status
        SectionLabel(stringResource(R.string.detail_status))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReadingStatus.entries.forEach { status ->
                val selected = status == t.status
                val (bg, ink) = statusColors(status)
                FilterChip(
                    selected = selected,
                    onClick = { vm.setStatus(status) },
                    label = { Text(stringResource(status.labelRes())) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                    shape = MaterialTheme.shapes.large,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = bg,
                        selectedLabelColor = ink,
                        selectedLeadingIconColor = ink,
                    ),
                    border = if (selected) {
                        BorderStroke(1.5.dp, ink)
                    } else {
                        FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                    },
                )
            }
        }

        // ---- progress
        SectionLabel(
            stringResource(R.string.detail_progress),
            trailing = if (total > 0) "${(TrackingRules.progress(t.currentChapter, total) * 100).toInt()}%" else null,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = { vm.setChapter(t.currentChapter - 1) }, enabled = t.currentChapter > 0) {
                Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.detail_prev_chapter))
            }
            Row(
                Modifier
                    .weight(1f)
                    .clickable { showTotalDialog = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    stringResource(R.string.detail_chapter, t.currentChapter),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (total > 0) stringResource(R.string.detail_of_total, total) else stringResource(R.string.detail_of_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            OutlinedIconButton(
                onClick = { vm.setChapter(t.currentChapter + 1) },
                enabled = total <= 0 || t.currentChapter < total,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.detail_next_chapter))
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { TrackingRules.progress(t.currentChapter, total) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- tags
        SectionLabel(stringResource(R.string.detail_tags), action = stringResource(R.string.detail_add_tag)) {
            showTagDialog = true
        }
        if (d.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                d.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeTag(tag.id) },
                        label = { Text(tag.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.detail_remove_tag, tag.name),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }
        }

        // ---- notes
        SectionLabel(stringResource(R.string.detail_notes))
        NotesField(initial = t.notes.orEmpty(), onChange = vm::onNotesChanged)

        // ---- file
        SectionLabel(stringResource(R.string.detail_file))
        val size = remember(book.sizeBytes) { Formatter.formatShortFileSize(context, book.sizeBytes) }
        val added = remember(book.addedAt) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(book.addedAt)) }
        Text(book.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.detail_file_info, size, added),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        book.parseError?.let { reason ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = TsundokuTheme.statusColors.droppedInk,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.detail_broken, reason),
                    style = MaterialTheme.typography.bodySmall,
                    color = TsundokuTheme.statusColors.droppedInk,
                )
            }
        }
    }

    if (showTotalDialog) {
        NumberDialog(
            title = stringResource(R.string.detail_total_title),
            body = stringResource(R.string.detail_total_body, book.spineCount),
            initial = t.totalChapters?.toString().orEmpty(),
            onDismiss = { showTotalDialog = false },
            onSave = {
                vm.setTotalChapters(it)
                showTotalDialog = false
            },
        )
    }
    if (showTagDialog) {
        TagDialog(
            onDismiss = { showTagDialog = false },
            onAdd = {
                vm.addTag(it)
                showTagDialog = false
            },
        )
    }
}

// ------------------------------------------------------------------ pieces

@Composable
private fun SectionLabel(text: String, trailing: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        action?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Stars(rating: Int, onRate: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            val filled = i <= rating
            Icon(
                if (filled) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(R.string.detail_star, i),
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onRate(i) }
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun NotesField(initial: String, onChange: (String) -> Unit) {
    // Local text so typing is instant; saving happens in the background (debounced).
    var text by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (text == null) text = initial }
    OutlinedTextField(
        value = text ?: initial,
        onValueChange = {
            text = it
            onChange(it)
        },
        placeholder = { Text(stringResource(R.string.detail_notes_hint)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp),
    )
}

@Composable
private fun NumberDialog(title: String, body: String, initial: String, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { new -> value = new.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.detail_total_edit)) },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value.toIntOrNull()) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun TagDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_tag_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(40) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.detail_tag_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(value) }, enabled = value.isNotBlank()) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
