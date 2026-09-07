package com.shankar.beep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DisplaySerif = FontFamily.Serif
val UiSans = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.2).sp,
        color = Ivory
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        color = Ivory
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        color = Ivory
    ),
    titleLarge = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
        color = Ivory
    ),
    titleMedium = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
        color = Ivory
    ),
    bodyLarge = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.15.sp,
        color = IvoryMuted
    ),
    bodyMedium = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp,
        color = IvoryMuted
    ),
    labelLarge = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        color = Ivory
    ),
    labelSmall = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
        color = Stone
    )
)
