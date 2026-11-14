package com.tsundoku.app.ui.settings

import android.provider.DocumentsContract
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.AppContainer
import com.tsundoku.app.R
import com.tsundoku.app.data.settings.ThemeMode
import com.tsundoku.app.library.FolderRules
import kotlinx.coroutines.launch

/** Settings (design PDF page 7). */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenTrash: () -> Unit,
    onOpenExcluded: () -> Unit,
    onOpenRestore: () -> Unit,
    onOpenStatus: () -> Unit,
) {
    val prefs = container.preferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trashed by container.trash.trashed.collectAsStateWithLifecycle(initialValue = emptyList())
    val excluded by prefs.excludedFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val shareFolder by prefs.shareFolder.collectAsStateWithLifecycle(initialValue = "")
    val theme by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val amoled by prefs.amoled.collectAsStateWithLifecycle(initialValue = false)
    val autoBackup by prefs.autoBackup.collectAsStateWithLifecycle(initialValue = true)
    val lastBackup by prefs.lastBackupAt.collectAsStateWithLifecycle(initialValue = 0L)

    val pickShareFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val relative = FolderRules.relativeFromTreeDocId(DocumentsContract.getTreeDocumentId(uri))
        if (relative.isNullOrEmpty()) {
            Toast.makeText(context, R.string.pick_folder_unsupported, Toast.LENGTH_LONG).show()
        } else {
            scope.launch { prefs.setShareFolder(relative) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        Section(stringResource(R.string.settings_section_library)) {
            SettingRow(Icons.Outlined.Refresh, stringResource(R.string.settings_scan), stringResource(R.string.settings_scan_sub), onClick = container.library::startScan)
            Divider()
            SettingRow(Icons.Outlined.FolderOff, stringResource(R.string.settings_excluded), stringResource(R.string.settings_excluded_sub, excluded.size), onClick = onOpenExcluded, chevron = true)
            Divider()
            SettingRow(Icons.Outlined.FolderOpen, stringResource(R.string.settings_share_folder), shareFolder, onClick = { pickShareFolder.launch(null) }, chevron = true)
            Divider()
            SettingRow(Icons.Outlined.Insights, stringResource(R.string.settings_status), stringResource(R.string.settings_status_sub), onClick = onOpenStatus, chevron = true)
        }

        Section(stringResource(R.string.settings_section_appearance)) {
            // Label on its own line, choices underneath: on a phone-width screen the chips
            // squeezed "Theme" into a column one letter wide when they shared a line.
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    Modifier.padding(start = 34.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                        .forEach { (mode, label) ->
                            FilterChip(
                                selected = theme == mode,
                                onClick = { scope.launch { prefs.setThemeMode(mode) } },
                                label = { Text(stringResource(label), maxLines = 1) },
                                shape = MaterialTheme.shapes.large,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                    selectedLabelColor = MaterialTheme.colorScheme.background,
                                ),
                            )
                        }
                }
            }
            Divider()
            SwitchRow(Icons.Outlined.Visibility, stringResource(R.string.settings_amoled), stringResource(R.string.settings_amoled_sub), amoled) {
                scope.launch { prefs.setAmoled(it) }
            }
        }

        Section(stringResource(R.string.settings_section_backup)) {
            SwitchRow(Icons.Outlined.Schedule, stringResource(R.string.settings_auto_backup), stringResource(R.string.settings_auto_backup_sub), autoBackup) {
                scope.launch { prefs.setAutoBackup(it) }
            }
            Divider()
            val last = if (lastBackup > 0) DateUtils.getRelativeTimeSpanString(lastBackup).toString() else stringResource(R.string.settings_never)
            SettingRow(Icons.Outlined.Backup, stringResource(R.string.settings_backup_now), stringResource(R.string.settings_last_backup, last), onClick = {
                scope.launch {
                    val file = container.backup.backupNow()
                    Toast.makeText(context, if (file != null) R.string.backup_saved else R.string.backup_failed, Toast.LENGTH_SHORT).show()
                }
            })
            Divider()
            SettingRow(Icons.Outlined.Restore, stringResource(R.string.settings_restore), stringResource(R.string.settings_restore_sub), onClick = onOpenRestore, chevron = true)
        }

        Section(stringResource(R.string.settings_section_more)) {
            SettingRow(Icons.Outlined.DeleteOutline, stringResource(R.string.settings_trash), stringResource(R.string.settings_trash_sub, trashed.size), onClick = onOpenTrash, chevron = true)
        }

        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        Text(
            stringResource(R.string.settings_about, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
        )
    }
}

// ------------------------------------------------------------------ shared pieces (used by sub-screens too)

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column { content() }
    }
}

@Composable
internal fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    chevron: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (chevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
