package com.novel.assistant.ui.theme

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val title: String,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
) {
    WarmDark(
        title = "Café Đêm",
        background = Color(0xFF1C1A19),
        surface = Color(0xFF252321),
        textPrimary = Color(0xFFE4DFD9),
        textSecondary = Color(0xFFA8A39D),
        isDark = true
    ),
    WarmCream(
        title = "Giấy Nến",
        background = Color(0xFFF9F6F0),
        surface = Color(0xFFEFE9E0),
        textPrimary = Color(0xFF33302E),
        textSecondary = Color(0xFF66605C),
        isDark = false
    ),
    Sepia(
        title = "Sách Cổ",
        background = Color(0xFFEAE0C8),
        surface = Color(0xFFDFD4B8),
        textPrimary = Color(0xFF4A3B2C),
        textSecondary = Color(0xFF7A6855),
        isDark = false
    ),
    NightBlue(
        title = "Biển Đêm",
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        textPrimary = Color(0xFFE2E8F0),
        textSecondary = Color(0xFF94A3B8),
        isDark = true
    )
}
