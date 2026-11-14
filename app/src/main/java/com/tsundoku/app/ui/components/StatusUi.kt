package com.tsundoku.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tsundoku.app.R
import com.tsundoku.app.data.db.ReadingStatus
import com.tsundoku.app.ui.theme.TsundokuTheme

@StringRes
fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.PLAN -> R.string.status_plan
    ReadingStatus.READING -> R.string.status_reading
    ReadingStatus.COMPLETED -> R.string.status_completed
    ReadingStatus.ON_HOLD -> R.string.status_on_hold
    ReadingStatus.DROPPED -> R.string.status_dropped
}

/** Soft background + readable ink color for a status (both themes). */
@Composable
fun statusColors(status: ReadingStatus): Pair<Color, Color> {
    val s = TsundokuTheme.statusColors
    return when (status) {
        ReadingStatus.PLAN -> s.planBg to s.planInk
        ReadingStatus.READING -> s.readingBg to s.readingInk
        ReadingStatus.COMPLETED -> s.completedBg to s.completedInk
        ReadingStatus.ON_HOLD -> s.onHoldBg to s.onHoldInk
        ReadingStatus.DROPPED -> s.droppedBg to s.droppedInk
    }
}

/** Dot + label, as under each cover in the grid. Color is never the only signal. */
@Composable
fun StatusDotLabel(status: ReadingStatus, modifier: Modifier = Modifier) {
    val (_, ink) = statusColors(status)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .background(ink, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(status.labelRes()),
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
