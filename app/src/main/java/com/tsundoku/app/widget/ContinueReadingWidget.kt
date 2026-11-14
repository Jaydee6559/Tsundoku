package com.tsundoku.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tsundoku.app.MainActivity
import com.tsundoku.app.R
import com.tsundoku.app.TsundokuApplication
import com.tsundoku.app.library.HomeRules
import com.tsundoku.app.library.TrackingRules
import kotlinx.coroutines.flow.first

/** What the widget shows (a snapshot taken when it updates). */
private data class WidgetData(
    val title: String?,
    val progressText: String?,
    val progress: Float?,
    val cover: Bitmap?,
)

/**
 * Home-screen widget: the book you're reading, its progress, and one tap into Tsundoku.
 * Updates when your reading data changes (while the app runs) and about once an hour.
 */
class ContinueReadingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { load(context) }.getOrDefault(WidgetData(null, null, null, null))
        provideContent { Content(context, data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val container = (context.applicationContext as TsundokuApplication).container
        val books = container.library.books.first()
        val tracking = container.tracking.observeAllTracking().first()
        val (book, t) = HomeRules.continueReading(books, tracking) ?: return WidgetData(null, null, null, null)
        val total = TrackingRules.totalChapters(t, book.spineCount)
        val cover = book.coverThumbPath?.let { path ->
            runCatching {
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 2 })
            }.getOrNull()
        }
        return WidgetData(
            title = book.title,
            progressText = if (total > 0) {
                context.getString(R.string.widget_progress, t.currentChapter, total, (TrackingRules.progress(t.currentChapter, total) * 100).toInt())
            } else {
                context.getString(R.string.home_chapter, t.currentChapter)
            },
            progress = if (total > 0) TrackingRules.progress(t.currentChapter, total) else null,
            cover = cover,
        )
    }

    @Composable
    private fun Content(context: Context, data: WidgetData) {
        // Paper and ink colors for light and dark home screens.
        val background = ColorProvider(day = Color(0xFFFBF7F0), night = Color(0xFF2A2622))
        val text = ColorProvider(day = Color(0xFF3B342C), night = Color(0xFFE8DFD3))
        val muted = ColorProvider(day = Color(0xFF7A6F62), night = Color(0xFFA89C8E))
        val accent = ColorProvider(day = Color(0xFFA0673E), night = Color(0xFFD19A6C))
        val track = ColorProvider(day = Color(0xFFEFE6D8), night = Color(0xFF332E29))

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.cover?.let {
                Image(
                    provider = ImageProvider(it),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(width = 52.dp, height = 78.dp).cornerRadius(4.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
            }
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = context.getString(if (data.title != null) R.string.widget_continue else R.string.app_name),
                    style = TextStyle(color = muted, fontSize = 11.sp),
                )
                Text(
                    text = data.title ?: context.getString(R.string.widget_nothing),
                    style = TextStyle(color = text, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    maxLines = 2,
                )
                data.progressText?.let {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(text = it, style = TextStyle(color = muted, fontSize = 11.sp))
                }
                data.progress?.let {
                    Spacer(GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = it,
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = accent,
                        backgroundColor = track,
                    )
                }
            }
        }
    }

    companion object {
        /** Redraws every Tsundoku widget on the home screen (safe to call often). */
        suspend fun refreshAll(context: Context) {
            runCatching { ContinueReadingWidget().updateAll(context) }
        }
    }
}

class ContinueReadingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContinueReadingWidget()
}
