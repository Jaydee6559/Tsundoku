package com.tsundoku.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tsundoku.app.storage.StoragePermission

enum class OnboardingStep { WELCOME, PERMISSION, READY }

/**
 * First-launch flow: Welcome -> All files access -> Ready.
 *
 * @param onboardingDone true when the user finished onboarding before (then only the permission
 *   step is shown, e.g. if they turned the permission off later).
 * @param permissionGranted current All files access state (re-checked by the caller on every resume).
 * @param onFinished called when the user taps "Open library".
 */
@Composable
fun OnboardingFlow(
    onboardingDone: Boolean,
    permissionGranted: Boolean,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable {
        mutableStateOf(if (onboardingDone) OnboardingStep.PERMISSION else OnboardingStep.WELCOME)
    }
    // True after the user went to settings once, so we can explain if it's still off.
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    // Move on automatically as soon as the permission is on.
    LaunchedEffect(permissionGranted, step) {
        if (permissionGranted && step == OnboardingStep.PERMISSION) {
            step = OnboardingStep.READY
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding",
        ) { current ->
            when (current) {
                OnboardingStep.WELCOME -> WelcomeScreen(
                    onGetStarted = { step = OnboardingStep.PERMISSION },
                )
                OnboardingStep.PERMISSION -> PermissionScreen(
                    showBack = !onboardingDone,
                    askedOnce = askedOnce,
                    onBack = { step = OnboardingStep.WELCOME },
                    onAllow = {
                        askedOnce = true
                        StoragePermission.openSettings(context)
                    },
                )
                OnboardingStep.READY -> ReadyScreen(onOpenLibrary = onFinished)
            }
        }
    }
}
