package com.gitupload.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun GitUploadTheme(
    content: @Composable () -> Unit
) {
    val palette by ThemeManager.currentPalette.collectAsState()
    val mode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val effectiveDark = when (mode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val activePalette = if (mode == ThemeMode.FOLLOW_SYSTEM) {
        if (effectiveDark && !palette.isDark) AppThemePalette.GITHUB_DARK
        else if (!effectiveDark && palette.isDark) AppThemePalette.GITHUB_LIGHT
        else palette
    } else palette

    val colorScheme = if (activePalette.isDark) {
        darkColorScheme(
            primary = activePalette.primary,
            onPrimary = Color.White,
            primaryContainer = activePalette.primaryContainer,
            onPrimaryContainer = activePalette.textPrimary,
            secondary = activePalette.accentCyan,
            onSecondary = Color.Black,
            secondaryContainer = activePalette.surfaceVariant,
            onSecondaryContainer = activePalette.textPrimary,
            tertiary = activePalette.accentPurple,
            onTertiary = Color.White,
            tertiaryContainer = activePalette.surfaceVariant,
            onTertiaryContainer = activePalette.textPrimary,
            background = activePalette.background,
            onBackground = activePalette.textPrimary,
            surface = activePalette.surface,
            onSurface = activePalette.textPrimary,
            surfaceVariant = activePalette.surfaceVariant,
            onSurfaceVariant = activePalette.textSecondary,
            surfaceContainerLowest = activePalette.background,
            surfaceContainerLow = activePalette.surface,
            surfaceContainer = activePalette.surface,
            surfaceContainerHigh = activePalette.surfaceVariant,
            surfaceContainerHighest = activePalette.surfaceBorder.copy(alpha = 0.5f),
            outline = activePalette.surfaceBorder,
            outlineVariant = activePalette.surfaceBorder.copy(alpha = 0.6f)
        )
    } else {
        lightColorScheme(
            primary = activePalette.primary,
            onPrimary = Color.White,
            primaryContainer = activePalette.primaryContainer,
            onPrimaryContainer = activePalette.textPrimary,
            secondary = activePalette.accentCyan,
            onSecondary = Color.White,
            secondaryContainer = activePalette.surfaceVariant,
            onSecondaryContainer = activePalette.textPrimary,
            tertiary = activePalette.accentPurple,
            onTertiary = Color.White,
            tertiaryContainer = activePalette.surfaceVariant,
            onTertiaryContainer = activePalette.textPrimary,
            background = activePalette.background,
            onBackground = activePalette.textPrimary,
            surface = activePalette.surface,
            onSurface = activePalette.textPrimary,
            surfaceVariant = activePalette.surfaceVariant,
            onSurfaceVariant = activePalette.textSecondary,
            surfaceContainerLowest = activePalette.surface,
            surfaceContainerLow = activePalette.background,
            surfaceContainer = activePalette.surface,
            surfaceContainerHigh = activePalette.surfaceVariant,
            surfaceContainerHighest = activePalette.surfaceBorder.copy(alpha = 0.3f),
            outline = activePalette.surfaceBorder,
            outlineVariant = activePalette.surfaceBorder.copy(alpha = 0.6f)
        )
    }

    CompositionLocalProvider(
        LocalAppPalette provides activePalette,
        LocalThemeMode provides mode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}


