package com.tsundoku.app.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.TsundokuApplication
import com.tsundoku.app.storage.StoragePermission
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.theme.TsundokuTheme
import kotlinx.coroutines.launch

/**
 * "Share → Tsundoku" / "Open with → Tsundoku" for EPUB files (design PDF page 6, left).
 * Shows what the book is before saving it; nothing is saved until you tap "Add to library".
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val uri = sharedUri(intent)
        val container = (application as TsundokuApplication).container
        val importer = ShareImporter(this, container.bookDao, container.library, container.upNext)

        setContent {
            TsundokuTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShareScreen(
                        uri = uri,
                        importer = importer,
                        onDone = { message ->
                            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun sharedUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        Intent.ACTION_VIEW -> intent.data
        else -> null
    }
}

private sealed interface ShareUi {
    data object Loading : ShareUi
    data class Ready(val book: PreparedBook) : ShareUi
    data class Error(val message: String) : ShareUi
    data object Saving : ShareUi
}

@Composable
private fun ShareScreen(uri: Uri?, importer: ShareImporter, onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<ShareUi>(ShareUi.Loading) }
    var addToUpNext by remember { mutableStateOf(true) }
    val needPermission = stringResource(R.string.share_need_permission)
    val notEpub = stringResource(R.string.share_not_epub)
    val tooBig = stringResource(R.string.share_too_big)
    val unreadable = stringResource(R.string.share_failed, "")

    LaunchedEffect(uri) {
        ui = when {
            !StoragePermission.isGranted() -> ShareUi.Error(needPermission)
            uri == null -> ShareUi.Error(notEpub)
            else -> when (val r = importer.prepare(uri)) {
                is ShareImporter.Prepare.Ready -> ShareUi.Ready(r.book)
                is ShareImporter.Prepare.Failed -> ShareUi.Error(
                    when (r.reason) {
                        ShareImporter.FailReason.NOT_EPUB -> notEpub
                        ShareImporter.FailReason.TOO_BIG -> tooBig
                        ShareImporter.FailReason.UNREADABLE -> unreadable.trimEnd(':', ' ')
                    },
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.share_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        when (val state = ui) {
            ShareUi.Loading, ShareUi.Saving -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.share_reading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is ShareUi.Error -> {
                Text(state.message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onDone(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
            is ShareUi.Ready -> {
                val book = state.book
                Column(Modifier.weight(1f)) {
                    Row {
                        val cover = book.cover
                        if (cover != null) {
                            Image(
                                bitmap = cover.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 72.dp, height = 108.dp)
                                    .clip(MaterialTheme.shapes.extraSmall),
                            )
                        } else {
                            CoverImage(null, book.title, Modifier.size(width = 72.dp, height = 108.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(
                                book.author ?: stringResource(R.string.share_unknown_author),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                book.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    if (book.alreadyInLibrary) {
                        val s = TsundokuTheme.statusColors
                        Surface(shape = MaterialTheme.shapes.small, color = s.onHoldBg, modifier = Modifier.padding(top = 14.dp)) {
                            Text(stringResource(R.string.share_duplicate), color = s.onHoldInk, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.share_save_to),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                    )
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(importer.saveFolderLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.share_add_up_next), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = addToUpNext, onCheckedChange = { addToUpNext = it })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            importer.discard(book)
                            onDone(null)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_cancel)) }
                    val savedTemplate = stringResource(R.string.share_saved, book.title)
                    Button(
                        onClick = {
                            ui = ShareUi.Saving
                            scope.launch {
                                val path = importer.import(book, addToUpNext)
                                onDone(if (path != null) savedTemplate else unreadable.trimEnd(':', ' '))
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                    ) { Text(stringResource(R.string.share_add)) }
                }
            }
        }
    }
}
