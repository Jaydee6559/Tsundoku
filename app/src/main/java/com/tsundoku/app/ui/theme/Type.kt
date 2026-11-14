package com.tsundoku.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Serif is used only for screen titles and book titles; everything else uses the
 * system sans (Roboto). The bundled Lora font replaces FontFamily.Serif in a later batch.
 */
val TitleFontFamily: FontFamily = FontFamily.Serif
private val BodyFontFamily: FontFamily = FontFamily.Default

private val base = Typography()

val TsundokuTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
    headlineLarge = base.headlineLarge.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
    headlineMedium = base.headlineMedium.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
    headlineSmall = base.headlineSmall.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
    titleLarge = base.titleLarge.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
    titleMedium = base.titleMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFontFamily, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = BodyFontFamily),
)
