package com.vfa.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Two families, the way the VFA_App_Real design uses them: a serif for the big
 * editorial headlines, a sans for everything the user has to read carefully.
 * Both resolve to the device's system faces, so nothing has to be downloaded and
 * the app stays legible with the user's own font settings.
 */
val TitleFont = FontFamily.Serif
val BodyFont = FontFamily.SansSerif

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = TitleFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = TitleFont, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 33.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, letterSpacing = 0.1.sp
    )
)
