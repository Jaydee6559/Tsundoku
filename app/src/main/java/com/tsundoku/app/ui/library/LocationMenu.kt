package com.tsundoku.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.library.LibraryQuery

/**
 * "All locations ▾" button: shows only books from one place, e.g. Download or the copies
 * ReadEra made in its own folder (so they can be found and cleaned up).
 */
@Composable
fun LocationMenu(
    current: String?,
    locations: List<Pair<String, Int>>,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selected = current != null
    Box(modifier) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceContainer,
            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.clickable { open = true },
        ) {
            val ink = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
            Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(current ?: stringResource(R.string.location_all), color = ink, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.location_menu), tint = ink)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val total = locations.sumOf { it.second }
            LocationItem(stringResource(R.string.location_all), total, null, current == null) {
                onPick(null)
                open = false
            }
            locations.forEach { (name, count) ->
                val hint = if (name == LibraryQuery.READERA_LOCATION) stringResource(R.string.location_readera_hint) else null
                LocationItem(name, count, hint, current == name) {
                    onPick(name)
                    open = false
                }
            }
        }
    }
}

@Composable
private fun LocationItem(name: String, count: Int, hint: String?, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(R.string.location_item, name, count))
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        onClick = onClick,
        trailingIcon = {
            if (checked) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}
