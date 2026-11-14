package com.tsundoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.storage.StoragePermission
import com.tsundoku.app.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.launch

/**
 * Decides what the user sees:
 *  - onboarding on first launch,
 *  - the permission step again if All files access was turned off later,
 *  - otherwise the main app.
 */
@Composable
fun AppRoot(preferences: AppPreferences, library: LibraryRepository) {
    val scope = rememberCoroutineScope()
    val onboardingDone by preferences.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
    val scanState by library.scanState.collectAsStateWithLifecycle()

    // Re-check every time the app comes back to the front (e.g. returning from Android settings).
    var permissionGranted by remember { mutableStateOf(StoragePermission.isGranted()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = StoragePermission.isGranted()
    }

    val done = onboardingDone
    when {
        // Settings still loading (a few milliseconds): plain background, no flicker.
        done == null -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )

        !done || !permissionGranted -> OnboardingFlow(
            onboardingDone = done,
            permissionGranted = permissionGranted,
            scanState = scanState,
            onStartScan = library::startScan,
            onFinished = { scope.launch { preferences.setOnboardingDone(true) } },
        )

        else -> {
            // Quick sync on every app start: only new/changed files are read, so it's cheap.
            LaunchedEffect(Unit) { library.startScan() }
            TsundokuApp(library = library)
        }
    }
}
