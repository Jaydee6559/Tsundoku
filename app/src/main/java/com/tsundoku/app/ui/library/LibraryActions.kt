package com.tsundoku.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.ui.components.labelRes
import com.tsundoku.app.ui.components.statusColors
import com.tsundoku.app.ui.theme.TsundokuTheme

/** Top bar shown while books are selected (long-press). */
@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onStatus: () -> Unit,
    onTag: () -> Unit,
    onUpNext: () -> Unit,
    onTrash: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = MaterialTheme.colorScheme.onPrimaryContainer
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.select_close), tint = ink) }
        Text(
            stringResource(R.string.select_count, count),
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSelectAll) { Icon(Icons.Outlined.SelectAll, stringResource(R.string.select_all), tint = ink) }
        IconButton(onClick = onStatus) {
            Icon(Icons.AutoMirrored.Outlined.LibraryBooks, stringResource(R.string.select_set_status), tint = ink)
        }
        IconButton(onClick = onTag) { Icon(Icons.Outlined.LocalOffer, stringResource(R.string.select_add_tag), tint = ink) }
        IconButton(onClick = onUpNext) {
            Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, stringResource(R.string.select_up_next), tint = ink)
        }
        IconButton(onClick = onTrash) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.select_delete), tint = ink) }
    }
}

/**
 * List row with swipe actions: swipe right = set status, swipe left = move to Trash.
 * The row always springs back; the action runs instead of the row disappearing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRow(
    enabled: Boolean,
    onSwipeStatus: () -> Unit,
    onSwipeTrash: () -> Unit,
    content: @Composable () -> Unit,
) {
    val status by rememberUpdatedState(onSwipeStatus)
    val trash by rememberUpdatedState(onSwipeTrash)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> status()
                SwipeToDismissBoxValue.EndToStart -> trash()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // spring back
        },
        positionalThreshold = { it * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val s = TsundokuTheme.statusColors
            val (bg, ink) = if (toRight) s.completedBg to s.completedInk else s.droppedBg to s.droppedInk
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toRight) {
                        Icon(Icons.AutoMirrored.Outlined.LibraryBooks, null, tint = ink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.swipe_status), color = ink, style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text(stringResource(R.string.swipe_trash), color = ink, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.DeleteOutline, null, tint = ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
    }
}

/** Pick a reading status (from a swipe or for several selected books). */
@Composable
fun StatusPickerDialog(onPick: (ReadingStatus) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.status_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReadingStatus.entries.forEach { status ->
                    val (_, ink) = statusColors(status)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(status) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(ink, CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text(stringResource(status.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun AddTagDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
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
