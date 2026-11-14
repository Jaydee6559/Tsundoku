package com.tsundoku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The book-spine illustration used on the welcome screen (and the app icon's idea). */
@Composable
fun BookPile() {
    val spines = listOf(
        Color(0xFFA0673E) to 70,
        Color(0xFF9C6B7A) to 84,
        Color(0xFF7C8B6F) to 62,
        Color(0xFF8A7A9C) to 92,
        Color(0xFFB8935A) to 76,
        Color(0xFF6F7F96) to 68,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
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
