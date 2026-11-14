package com.tsundoku.app.ui.series

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tsundoku.app.R
import com.tsundoku.app.data.db.SeriesStatus
import com.tsundoku.app.library.SeriesSummary
import com.tsundoku.app.ui.components.CoverImage
import com.tsundoku.app.ui.theme.TsundokuTheme

@StringRes
fun SeriesStatus?.labelRes(): Int = when (this) {
    SeriesStatus.ONGOING -> R.string.series_status_ongoing
    SeriesStatus.COMPLETED -> R.string.series_status_completed
    SeriesStatus.HIATUS -> R.string.series_status_hiatus
    null -> R.string.series_status_unset
}

@Composable
fun seriesStatusColors(status: SeriesStatus?): Pair<Color, Color> {
    val s = TsundokuTheme.statusColors
    return when (status) {
        SeriesStatus.ONGOING -> s.readingBg to s.readingInk
        SeriesStatus.COMPLETED -> s.completedBg to s.completedInk
        SeriesStatus.HIATUS -> s.onHoldBg to s.onHoldInk
        null -> s.planBg to s.planInk
    }
}

/** Small rounded label (status, missing volumes, "New"). */
@Composable
fun Pill(text: String, bg: Color, ink: Color, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = bg, modifier = modifier) {
        Text(
            text,
            color = ink,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** "Missing Vol 3" or "Missing Vol 3 +2". */
@Composable
fun missingLabel(series: SeriesSummary): String? {
    val first = series.missingVolumes.firstOrNull() ?: return null
    val more = series.missingVolumes.size - 1
    return if (more == 0) {
        stringResource(R.string.series_missing_one, first.toString())
    } else {
        stringResource(R.string.series_missing_more, first.toString(), more)
    }
}

/** Up to three covers fanned out, like a little pile. */
@Composable
fun CoverStack(series: SeriesSummary, coverWidth: Dp = 30.dp, modifier: Modifier = Modifier) {
    val books = series.books.take(3)
    val step = coverWidth / 4
    val height = coverWidth * 1.5f
    Box(modifier.size(width = coverWidth + step * 2, height = height + step * 2)) {
        books.asReversed().forEachIndexed { i, book ->
            val shift = step * (books.size - 1 - i)
            CoverImage(
                thumbPath = book.coverThumbPath,
                title = book.title,
                modifier = Modifier
                    .offset(x = shift, y = step * 2 - shift)
                    .size(width = coverWidth, height = height),
            )
        }
    }
}
