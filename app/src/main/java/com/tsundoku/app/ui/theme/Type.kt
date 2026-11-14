package com.tsundoku.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tsundoku.app.R

/**
 * Lora (SIL Open Font License, see docs/licenses/Lora-OFL.txt) for screen and book titles;
 * everything else uses the system sans (Roboto).
 */
val TitleFontFamily: FontFamily = FontFamily(Font(R.font.lora, FontWeight.Normal))
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
