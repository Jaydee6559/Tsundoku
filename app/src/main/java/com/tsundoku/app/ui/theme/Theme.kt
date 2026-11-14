package com.tsundoku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tsundoku.app.data.settings.ThemeMode

private val LightColors: ColorScheme = lightColorScheme(
    primary = PaperAccent,
    onPrimary = Color.White,
    primaryContainer = PaperAccentSoft,
    onPrimaryContainer = PaperAccentInk,
    secondary = PaperSage,
    onSecondary = Color.White,
    secondaryContainer = PaperSageSoft,
    onSecondaryContainer = PaperSageInk,
    tertiary = PaperSage,
    background = PaperBackground,
    onBackground = PaperText,
    surface = PaperBackground,
    onSurface = PaperText,
    surfaceVariant = PaperSurface2,
    onSurfaceVariant = PaperTextMuted,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurface2,
    surfaceContainerHighest = PaperSurface2,
    inverseSurface = PaperText,
    inverseOnSurface = PaperBackground,
    inversePrimary = PaperAccentSoft,
    outline = PaperOutline,
    outlineVariant = PaperBorder,
    error = PaperError,
    onError = Color.White,
    errorContainer = PaperErrorSoft,
    onErrorContainer = PaperError,
    scrim = Color(0xFF3B342C),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = InkAccent,
    onPrimary = InkBackground,
    primaryContainer = InkAccentSoft,
    onPrimaryContainer = InkAccentInk,
    secondary = InkSage,
    onSecondary = InkBackground,
    secondaryContainer = InkSageSoft,
    onSecondaryContainer = InkSageInk,
    tertiary = InkSage,
    background = InkBackground,
    onBackground = InkText,
    surface = InkBackground,
    onSurface = InkText,
    surfaceVariant = InkSurface2,
    onSurfaceVariant = InkTextMuted,
    surfaceContainerLowest = InkBackground,
    surfaceContainerLow = InkSurface,
    surfaceContainer = InkSurface,
    surfaceContainerHigh = InkSurface2,
    surfaceContainerHighest = InkSurface2,
    inverseSurface = InkText,
    inverseOnSurface = InkBackground,
    inversePrimary = InkAccentSoft,
    outline = InkOutline,
    outlineVariant = InkBorder,
    error = InkError,
    onError = InkBackground,
    errorContainer = InkErrorSoft,
    onErrorContainer = InkError,
    scrim = Color.Black,
)

/** AMOLED option (wired to Settings in a later batch): pure black backgrounds. */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
)

private val TsundokuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // book covers
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),      // cards
    large = RoundedCornerShape(22.dp),       // buttons, sheets
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

@Composable
fun TsundokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        darkTheme && amoledBlack -> DarkColors.amoled()
        darkTheme -> DarkColors
        else -> LightColors
    }
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = TsundokuTypography,
            shapes = TsundokuShapes,
            content = content,
        )
    }
}

/** Access with `TsundokuTheme.statusColors` inside any composable. */
object TsundokuTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current
}

/** Resolves the Settings choice: System follows the phone, Light/Dark force it. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
