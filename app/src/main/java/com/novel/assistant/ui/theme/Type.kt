package com.novel.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Novel content uses Serif for webnovel reading feel
val NovelFontFamily = FontFamily.Serif
// UI uses default sans-serif (clean, modern)
val UiFontFamily = FontFamily.Default

val NovelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal, // Lighter for calm vibe
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    // Novel reading body - Muted contrast, generous line height for immersion
    bodyLarge = TextStyle(
        fontFamily = NovelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 32.sp,  // ~1.88x for optimal reading breathing room
        letterSpacing = 0.5.sp, // Tweak letter spacing slightly
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextHint
    ),
)
