package com.tsundoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tsundoku.app.diagnostics.CrashLog
import com.tsundoku.app.data.settings.ThemeMode
import com.tsundoku.app.ui.theme.isDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.notifications.AppVisibility
import com.tsundoku.app.ui.AppRoot
import com.tsundoku.app.ui.components.CrashReportDialog
import com.tsundoku.app.ui.theme.TsundokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TsundokuApplication).container
        val lastCrash = CrashLog.read(this)
        setContent {
            val themeMode by container.preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val amoled by container.preferences.amoled.collectAsStateWithLifecycle(initialValue = false)
            TsundokuTheme(darkTheme = themeMode.isDark(), amoledBlack = amoled) {
                AppRoot(
                    preferences = container.preferences,
                    library = container.library,
                    tracking = container.tracking,
                    trash = container.trash,
                    series = container.series,
                    upNext = container.upNext,
                    watcher = container.watcher,
                    container = container,
                )

                var crashReport by rememberSaveable { mutableStateOf(lastCrash) }
                crashReport?.let { report ->
                    CrashReportDialog(report = report, onDismiss = {
                        CrashLog.clear(this@MainActivity)
                        crashReport = null
                    })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isVisible = true
        (application as TsundokuApplication).container.notifier.reset() // you're looking at the new books now
    }

    override fun onStop() {
        AppVisibility.isVisible = false
        super.onStop()
    }
}
