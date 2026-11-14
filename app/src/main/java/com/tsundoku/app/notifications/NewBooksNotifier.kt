package com.tsundoku.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tsundoku.app.MainActivity
import com.tsundoku.app.R
import java.util.concurrent.atomic.AtomicBoolean

/** Whether the app is on screen right now (set by MainActivity). */
object AppVisibility {
    private val visible = AtomicBoolean(false)
    var isVisible: Boolean
        get() = visible.get()
        set(value) = visible.set(value)
}

/**
 * One notification for new books, never one per book: "80 novels added".
 * Counts keep adding up until you open the app, then reset.
 * Only shown while the app is in the background (on screen, the Library updates by itself).
 */
class NewBooksNotifier(private val context: Context) {

    private var pendingCount = 0
    private val recentTitles = ArrayDeque<String>()

    @Synchronized
    fun onBooksAdded(count: Int, titles: List<String>) {
        if (count <= 0 || AppVisibility.isVisible) return
        if (!canNotify()) return
        pendingCount += count
        titles.forEach { title ->
            recentTitles.addFirst(title)
            while (recentTitles.size > MAX_LINES) recentTitles.removeLast()
        }
        show()
    }

    /** Called when the app opens: the user has seen the new books. */
    @Synchronized
    fun reset() {
        pendingCount = 0
        recentTitles.clear()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    @SuppressLint("MissingPermission") // checked in canNotify() before every call
    private fun show() {
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = context.resources.getQuantityString(R.plurals.notify_books_added, pendingCount, pendingCount)
        val style = NotificationCompat.InboxStyle().also { s -> recentTitles.forEach { s.addLine(it) } }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(recentTitles.joinToString(", "))
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notify_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.notify_channel_desc) },
        )
    }

    private companion object {
        const val CHANNEL_ID = "new_books"
        const val NOTIFICATION_ID = 1001
        const val MAX_LINES = 5
    }
}
