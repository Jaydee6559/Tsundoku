package com.tsundoku.app.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.tsundoku.app.ui.theme.TitleFontFamily
import com.tsundoku.app.R
import com.tsundoku.app.library.EpubDiscovery
import com.tsundoku.app.library.FolderCount
import com.tsundoku.app.library.ScanState
import com.tsundoku.app.ui.theme.TsundokuTheme

/** Step 2 of onboarding: the first scan, with a live count (design PDF page 2, right). */
@Composable
fun FirstScanScreen(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    // Start the scan as soon as this screen appears (only if nothing is running yet).
    LaunchedEffect(Unit) {
        if (scanState is ScanState.Idle || scanState is ScanState.Failed) onStartScan()
    }

    val found: Int
    val progress: Float
    val label: String
    val folders: List<FolderCount>
    when (scanState) {
        is ScanState.Reading -> {
            found = scanState.found
            progress = if (scanState.total == 0) 1f else scanState.processed / scanState.total.toFloat()
            label = stringResource(R.string.scan_reading)
            folders = scanState.folders
        }
        is ScanState.Done -> {
            found = scanState.found
            progress = 1f
            label = stringResource(R.string.scan_done)
            folders = scanState.folders
        }
        is ScanState.Failed -> {
            found = 0
            progress = 0f
            label = stringResource(R.string.scan_failed, scanState.message)
            folders = emptyList()
        }
        ScanState.Discovering, ScanState.Idle -> {
            found = 0
            progress = 0f
            label = stringResource(R.string.scan_looking)
            folders = emptyList()
        }
    }
    val animatedProgress by animateFloatAsState(progress, label = "scanProgress")

    OnboardingScaffold(
        top = { StepHeader(label = stringResource(R.string.onb_step_2), showBack = false, onBack = {}) },
        bottom = { PrimaryButton(text = stringResource(R.string.onb_open_library), onClick = onOpenLibrary) },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.scan_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.scan_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = found.toString(),
                    fontFamily = TitleFontFamily,
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.scan_epubs_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            if (scanState is ScanState.Discovering) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp))
            } else {
                LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (scanState is ScanState.Reading || scanState is ScanState.Done) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (folders.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.scan_found_in),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                FolderCard(folders.take(MAX_FOLDERS))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.scan_skipped, EpubDiscovery.skippedForDisplay.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MAX_FOLDERS = 6

@Composable
private fun FolderCard(folders: List<FolderCount>) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            folders.forEachIndexed { i, folder ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        folder.count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FirstScanPreview() = TsundokuTheme {
    FirstScanScreen(
        scanState = ScanState.Reading(
            found = 312, processed = 224, total = 312,
            folders = listOf(FolderCount("Download", 142), FolderCount("Telegram", 98), FolderCount("Documents", 51)),
        ),
        onStartScan = {},
        onOpenLibrary = {},
    )
}
