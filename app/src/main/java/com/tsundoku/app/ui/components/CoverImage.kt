package com.tsundoku.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tsundoku.app.ui.theme.TitleFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory cache so scrolling back doesn't re-read thumbnails from disk.
 * Limited by MEMORY (1/8 of the app's allowance), not by count: hundreds of covers
 * held at once could otherwise run the app out of memory.
 */
private val thumbCache = object : LruCache<String, ImageBitmap>(
    (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
}

/** Muted cover colors (Paper and ink) for books without a cover image. */
private val PLACEHOLDER_COLORS = listOf(
    Color(0xFFA0673E), Color(0xFF7C8B6F), Color(0xFF6F7F96), Color(0xFF9C6B7A), Color(0xFFB8935A),
    Color(0xFF5E6B5C), Color(0xFF8A7A9C), Color(0xFFA4775B), Color(0xFF7E6A55), Color(0xFF6B8584),
)

/**
 * A book cover: the saved thumbnail if there is one, otherwise a colored "book" with its title.
 * Thumbnails load off the main thread, so lists stay smooth.
 */
@Composable
fun CoverImage(thumbPath: String?, title: String, modifier: Modifier = Modifier) {
    val bitmap by produceState(initialValue = thumbPath?.let { thumbCache.get(it) }, key1 = thumbPath) {
        if (thumbPath != null && value == null) {
            value = withContext(Dispatchers.IO) {
                // Catches everything, including OutOfMemoryError: a bad image shows the placeholder instead.
                try {
                    BitmapFactory.decodeFile(thumbPath)?.asImageBitmap()
                } catch (t: Throwable) {
                    null
                }
            }?.also { thumbCache.put(thumbPath, it) }
        }
    }

    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier
            .clip(shape)
            .background(PLACEHOLDER_COLORS[(title.hashCode() and Int.MAX_VALUE) % PLACEHOLDER_COLORS.size]),
        contentAlignment = Alignment.BottomStart,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = TitleFontFamily,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}
