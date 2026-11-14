package com.tsundoku.app.ui.theme

import androidx.compose.ui.graphics.Color

// "Paper and ink" palette. Source: docs/design/Tsundoku-UI-Design.pdf (page 1).

// ---------- Light ----------
internal val PaperBackground = Color(0xFFF5EFE6)
internal val PaperSurface = Color(0xFFFBF7F0)
internal val PaperSurface2 = Color(0xFFEFE6D8)
internal val PaperBorder = Color(0xFFE4D9C8)
internal val PaperOutline = Color(0xFFB5A895)
internal val PaperText = Color(0xFF3B342C)
internal val PaperTextMuted = Color(0xFF7A6F62)
internal val PaperAccent = Color(0xFFA0673E)
internal val PaperAccentSoft = Color(0xFFEEDCCB)
internal val PaperAccentInk = Color(0xFF7A4A26)
internal val PaperSage = Color(0xFF7C8B6F)
internal val PaperSageSoft = Color(0xFFE2E6D9)
internal val PaperSageInk = Color(0xFF4F5D42)
internal val PaperError = Color(0xFF8A3F35)
internal val PaperErrorSoft = Color(0xFFF0D6D0)

// ---------- Dark ----------
internal val InkBackground = Color(0xFF1E1B18)
internal val InkSurface = Color(0xFF2A2622)
internal val InkSurface2 = Color(0xFF332E29)
internal val InkBorder = Color(0xFF3B352F)
internal val InkOutline = Color(0xFF6B6157)
internal val InkText = Color(0xFFE8DFD3)
internal val InkTextMuted = Color(0xFFA89C8E)
internal val InkAccent = Color(0xFFD19A6C)
internal val InkAccentSoft = Color(0xFF4A3526)
internal val InkAccentInk = Color(0xFFEBC3A0)
internal val InkSage = Color(0xFFA3B193)
internal val InkSageSoft = Color(0xFF353A2E)
internal val InkSageInk = Color(0xFFC2CDB3)
internal val InkError = Color(0xFFE7B1A8)
internal val InkErrorSoft = Color(0xFF4A2C28)

/** Colors for reading statuses. Each status is a soft background + a readable ink color. */
data class StatusColors(
    val readingBg: Color, val readingInk: Color,
    val completedBg: Color, val completedInk: Color,
    val planBg: Color, val planInk: Color,
    val onHoldBg: Color, val onHoldInk: Color,
    val droppedBg: Color, val droppedInk: Color,
)

internal val LightStatusColors = StatusColors(
    readingBg = PaperAccentSoft, readingInk = PaperAccentInk,
    completedBg = PaperSageSoft, completedInk = PaperSageInk,
    planBg = Color(0xFFE8E1D6), planInk = Color(0xFF6E655A),
    onHoldBg = Color(0xFFF1E3C4), onHoldInk = Color(0xFF7A5A22),
    droppedBg = PaperErrorSoft, droppedInk = PaperError,
)

internal val DarkStatusColors = StatusColors(
    readingBg = InkAccentSoft, readingInk = InkAccentInk,
    completedBg = InkSageSoft, completedInk = InkSageInk,
    planBg = Color(0xFF39332D), planInk = Color(0xFFBFB3A4),
    onHoldBg = Color(0xFF45391F), onHoldInk = Color(0xFFE3C78F),
    droppedBg = InkErrorSoft, droppedInk = InkError,
)
