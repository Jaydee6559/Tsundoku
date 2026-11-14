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
            TsundokuTheme {
                AppRoot(preferences = container.preferences, library = container.library)

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
}
