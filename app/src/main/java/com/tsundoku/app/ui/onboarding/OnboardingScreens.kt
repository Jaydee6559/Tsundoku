package com.tsundoku.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tsundoku.app.R
import com.tsundoku.app.ui.screens.BookPile
import com.tsundoku.app.ui.theme.TsundokuTheme

// Layouts follow docs/design/Tsundoku-UI-Design.pdf, page 2 (Onboarding).

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    OnboardingScaffold(
        bottom = {
            PrimaryButton(text = stringResource(R.string.onb_get_started), onClick = onGetStarted)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BookPile()
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.app_name_jp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.onb_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PermissionScreen(
    showBack: Boolean,
    askedOnce: Boolean,
    onBack: () -> Unit,
    onAllow: () -> Unit,
) {
    OnboardingScaffold(
        top = { StepHeader(label = stringResource(R.string.onb_step_1), showBack = showBack, onBack = onBack) },
        bottom = {
            PrimaryButton(
                text = stringResource(if (askedOnce) R.string.onb_allow_again else R.string.onb_allow),
                onClick = onAllow,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onb_allow_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            IconBadge(Icons.Outlined.FolderOpen)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onb_perm_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onb_perm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            TrustPoint(Icons.Outlined.Lock, R.string.onb_trust_offline_title, R.string.onb_trust_offline_body)
            TrustPoint(Icons.Outlined.Visibility, R.string.onb_trust_readonly_title, R.string.onb_trust_readonly_body)
            TrustPoint(Icons.Outlined.DeleteOutline, R.string.onb_trust_trash_title, R.string.onb_trust_trash_body)

            if (askedOnce) {
                Spacer(Modifier.height(8.dp))
                val s = TsundokuTheme.statusColors
                Text(
                    text = stringResource(R.string.onb_not_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = s.onHoldInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(s.onHoldBg, MaterialTheme.shapes.small)
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
fun ReadyScreen(onOpenLibrary: () -> Unit) {
    OnboardingScaffold(
        top = { StepHeader(label = stringResource(R.string.onb_step_2), showBack = false, onBack = {}) },
        bottom = { PrimaryButton(text = stringResource(R.string.onb_open_library), onClick = onOpenLibrary) },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            IconBadge(Icons.Outlined.CheckCircle, sage = true)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onb_ready_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onb_ready_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onb_granted),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun OnboardingScaffold(
    top: @Composable () -> Unit = {},
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        top()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { content() }
        Spacer(Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) { bottom() }
    }
}

@Composable
private fun StepHeader(label: String, showBack: Boolean, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.onb_back))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun IconBadge(icon: ImageVector, sage: Boolean = false) {
    val bg = if (sage) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (sage) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        Modifier
            .size(64.dp)
            .background(bg, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun TrustPoint(icon: ImageVector, title: Int, body: Int) {
    Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- previews

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() = TsundokuTheme { WelcomeScreen(onGetStarted = {}) }

@Preview(showBackground = true)
@Composable
private fun PermissionPreview() = TsundokuTheme {
    PermissionScreen(showBack = true, askedOnce = true, onBack = {}, onAllow = {})
}

@Preview(showBackground = true)
@Composable
private fun ReadyPreview() = TsundokuTheme { ReadyScreen(onOpenLibrary = {}) }
