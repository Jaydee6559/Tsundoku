package com.tsundoku.app.ui.settings

import android.provider.DocumentsContract
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.AppContainer
import com.tsundoku.app.R
import com.tsundoku.app.library.FolderRules
import kotlinx.coroutines.launch
import java.io.File

@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) { content() }
    }
}

// ------------------------------------------------------------------ excluded folders

@Composable
fun ExcludedFoldersScreen(container: AppContainer, onBack: () -> Unit) {
    val prefs = container.preferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val excluded by prefs.excludedFolders.collectAsStateWithLifecycle(initialValue = emptySet())

    // After any change, rescan so the library matches right away.
    val change: (suspend () -> Unit) -> Unit = { block ->
        scope.launch {
            block()
            container.library.startScan()
            container.watcher.refresh()
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val relative = FolderRules.relativeFromTreeDocId(DocumentsContract.getTreeDocumentId(uri))
        if (relative.isNullOrEmpty()) {
            Toast.makeText(context, R.string.pick_folder_unsupported, Toast.LENGTH_LONG).show()
        } else {
            change { prefs.addExcludedFolder(relative) }
        }
    }

    SubScreen(stringResource(R.string.excluded_title), onBack) {
        Text(stringResource(R.string.excluded_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (excluded.isEmpty()) {
            Text(stringResource(R.string.excluded_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            Section(stringResource(R.string.excluded_title)) {
                excluded.sorted().forEachIndexed { i, folder ->
                    if (i > 0) Divider()
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(folder, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { change { prefs.removeExcludedFolder(folder) } }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.excluded_remove, folder))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { pick.launch(null) }) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.excluded_add))
        }
    }
}

// ------------------------------------------------------------------ restore

@Composable
fun RestoreScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backups = remember { container.backup.listBackups() }
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    val report: (suspend () -> com.tsundoku.app.backup.BackupManager.RestoreResult) -> Unit = { block ->
        scope.launch {
            val message = runCatching { block() }.fold(
                onSuccess = { context.getString(R.string.restore_done, it.restored, it.notFound) },
                onFailure = { context.getString(R.string.restore_failed, it.message ?: it.javaClass.simpleName) },
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirm = { report { container.backup.restore(uri) } }
    }

    SubScreen(stringResource(R.string.restore_title), onBack) {
        Text(stringResource(R.string.restore_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (backups.isEmpty()) {
            Text(stringResource(R.string.restore_none), style = MaterialTheme.typography.bodyMedium)
        } else {
            Section(container.backup.backupDir.name) {
                backups.forEachIndexed { i, file ->
                    if (i > 0) Divider()
                    BackupRow(file) { confirm = { report { container.backup.restore(file) } } }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
            Text(stringResource(R.string.restore_from_file))
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    action()
                }) { Text(stringResource(R.string.restore_action)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun BackupRow(file: File, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(DateUtils.getRelativeTimeSpanString(file.lastModified()).toString(), style = MaterialTheme.typography.bodyLarge)
            Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ scanner status

@Composable
fun ScannerStatusScreen(container: AppContainer, onBack: () -> Unit) {
    val library = container.library
    val books by library.libraryCount.collectAsStateWithLifecycle(initialValue = 0)
    val broken by library.brokenCount.collectAsStateWithLifecycle(initialValue = 0)
    val missing by library.missingCount.collectAsStateWithLifecycle(initialValue = 0)
    val watched by container.watcher.watchedCount.collectAsStateWithLifecycle()
    val last by library.lastFullScan.collectAsStateWithLifecycle()

    SubScreen(stringResource(R.string.status_title), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(books.toString(), stringResource(R.string.status_books), Modifier.weight(1f))
            StatTile(broken.toString(), stringResource(R.string.status_broken), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(missing.toString(), stringResource(R.string.status_missing), Modifier.weight(1f))
            StatTile(watched.toString(), stringResource(R.string.status_watched), Modifier.weight(1f))
        }
        Section(stringResource(R.string.status_last_scan)) {
            Column(Modifier.padding(14.dp)) {
                val scan = last
                if (scan == null) {
                    Text(stringResource(R.string.status_last_scan_none), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val (done, at) = scan
                    Text(
                        stringResource(
                            R.string.status_last_scan_value,
                            DateUtils.getRelativeTimeSpanString(at).toString(),
                            done.found,
                            done.durationMs / 1000.0,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.status_last_changes, done.added, done.updated, done.moved, done.missing, done.broken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.status_background),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
