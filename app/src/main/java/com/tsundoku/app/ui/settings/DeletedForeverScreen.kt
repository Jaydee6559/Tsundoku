package com.tsundoku.app.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.AppContainer
import com.tsundoku.app.R
import com.tsundoku.app.data.db.DeletedBookEntity
import com.tsundoku.app.library.LibraryQuery
import com.tsundoku.app.library.folderOf
import com.tsundoku.app.ui.theme.TsundokuTheme
import kotlinx.coroutines.launch

/** Books deleted forever, and where identical copies kept reappearing. */
@Composable
fun DeletedForeverScreen(container: AppContainer, onBack: () -> Unit) {
    val deleted by container.trash.deletedForever.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.deleted_title), style = MaterialTheme.typography.headlineSmall)
        }
        if (deleted.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.deleted_none), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            item {
                Text(
                    stringResource(R.string.deleted_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(deleted, key = { it.fingerprint }) { entry ->
                DeletedRow(entry, onAllow = { scope.launch { container.trash.allowAgain(entry.fingerprint) } })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun DeletedRow(entry: DeletedBookEntity, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.deleted_when, DateUtils.getRelativeTimeSpanString(entry.deletedAt).toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val seen = entry.lastSeenPath
            if (entry.timesRemoved > 0 && seen != null) {
                val root = android.os.Environment.getExternalStorageDirectory().absolutePath
                Text(
                    stringResource(R.string.deleted_removed_again, entry.timesRemoved, LibraryQuery.locationOf(folderOf(seen, root))),
                    style = MaterialTheme.typography.bodySmall,
                    color = TsundokuTheme.statusColors.onHoldInk,
                )
                Text(
                    seen,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(onClick = onAllow, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
            Text(stringResource(R.string.deleted_allow), style = MaterialTheme.typography.labelMedium)
        }
    }
}
