package com.novel.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NovelDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = TextOnPrimary,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleLight,
    secondary = BlueSky,
    onSecondary = TextOnPrimary,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = PinkSoft,
    onTertiary = TextOnPrimary,
    tertiaryContainer = DarkCardElevated,
    onTertiaryContainer = PinkSoft,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    error = RedSoft,
    onError = TextOnPrimary,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
)

@Composable
fun NovelAITheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NovelDarkColorScheme,
        typography = NovelTypography,
        content = content
    )
}
