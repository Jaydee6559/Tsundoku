package com.tsundoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tsundoku.app.ui.AppRoot
import com.tsundoku.app.ui.theme.TsundokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TsundokuApplication).container
        setContent {
            TsundokuTheme {
                AppRoot(preferences = container.preferences, library = container.library)
            }
        }
    }
}
