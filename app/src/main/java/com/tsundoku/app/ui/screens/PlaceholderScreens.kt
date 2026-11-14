package com.tsundoku.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.ui.theme.TsundokuTheme

/** Temporary Home: shows the brand and the theme until the real Home lands (Batch 11). */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BookPile()
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.app_name_jp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        StatusPreviewRow()
    }
}

/** Temporary screen for tabs that are built in later batches. */
@Composable
fun PlaceholderScreen(@StringRes title: Int, @StringRes body: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The book-spine illustration from the welcome screen design. */
@Composable
private fun BookPile() {
    val spines = listOf(
        Color(0xFFA0673E) to 70,
        Color(0xFF9C6B7A) to 84,
        Color(0xFF7C8B6F) to 62,
        Color(0xFF8A7A9C) to 92,
        Color(0xFFB8935A) to 76,
        Color(0xFF6F7F96) to 68,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        spines.forEachIndexed { i, (color, h) ->
            Box(
                Modifier
                    .width(if (i == 3) 22.dp else 16.dp)
                    .height(h.dp)
                    .background(color, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}

/** Small row of status chips, so the theme's status colors can be checked on a real phone. */
@Composable
private fun StatusPreviewRow() {
    val s = TsundokuTheme.statusColors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip("Reading", s.readingBg, s.readingInk)
        StatusChip("Completed", s.completedBg, s.completedInk)
        StatusChip("Plan", s.planBg, s.planInk)
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip("On hold", s.onHoldBg, s.onHoldInk)
        StatusChip("Dropped", s.droppedBg, s.droppedInk)
    }
}

@Composable
private fun StatusChip(label: String, bg: Color, ink: Color) {
    Box(
        Modifier
            .background(bg, MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    TsundokuTheme { HomeScreen() }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreviewDark() {
    TsundokuTheme(darkTheme = true) { HomeScreen() }
}
