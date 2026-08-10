package com.gitupload.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePalette(
    val displayName: String,
    val isDark: Boolean,
    val primary: Color,
    val primaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceBorder: Color,
    val accentCyan: Color,
    val accentPurple: Color,
    val codeBg: Color,
    val textPrimary: Color,
    val textSecondary: Color
) {
    MATERIAL_EXPRESSIVE(
        displayName = "Material 3 Expressive",
        isDark = true,
        primary = Color(0xFF10B981),
        primaryContainer = Color(0xFF064E3B),
        background = Color(0xFF0B0F17),
        surface = Color(0xFF131B2A),
        surfaceVariant = Color(0xFF1E293B),
        surfaceBorder = Color(0xFF334155),
        accentCyan = Color(0xFF38BDF8),
        accentPurple = Color(0xFFA855F7),
        codeBg = Color(0xFF070A10),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8)
    ),
    GITHUB_DARK(
        displayName = "GitHub Dark",
        isDark = true,
        primary = Color(0xFF2EA043),
        primaryContainer = Color(0xFF1B4D27),
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        surfaceVariant = Color(0xFF21262D),
        surfaceBorder = Color(0xFF30363D),
        accentCyan = Color(0xFF38BDF8),
        accentPurple = Color(0xFFA855F7),
        codeBg = Color(0xFF010409),
        textPrimary = Color(0xFFF0F6FC),
        textSecondary = Color(0xFF8B949E)
    ),
    NORDIC_FROST(
        displayName = "Nordic Frost",
        isDark = true,
        primary = Color(0xFF38BDF8),
        primaryContainer = Color(0xFF075985),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF334155),
        surfaceBorder = Color(0xFF475569),
        accentCyan = Color(0xFF06B6D4),
        accentPurple = Color(0xFF818CF8),
        codeBg = Color(0xFF020617),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFCBD5E1)
    ),
    EMERALD_MINT(
        displayName = "Emerald Mint",
        isDark = true,
        primary = Color(0xFF10B981),
        primaryContainer = Color(0xFF047857),
        background = Color(0xFF064E3B),
        surface = Color(0xFF047857),
        surfaceVariant = Color(0xFF059669),
        surfaceBorder = Color(0xFF10B981),
        accentCyan = Color(0xFF34D399),
        accentPurple = Color(0xFF6EE7B7),
        codeBg = Color(0xFF022C22),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFFA7F3D0)
    ),
    CYBER_NEON(
        displayName = "Cyberpunk Neon",
        isDark = true,
        primary = Color(0xFFF43F5E),
        primaryContainer = Color(0xFF881337),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF334155),
        surfaceBorder = Color(0xFF475569),
        accentCyan = Color(0xFF06B6D4),
        accentPurple = Color(0xFFE11D48),
        codeBg = Color(0xFF020617),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8)
    ),
    DEEP_PURPLE(
        displayName = "Deep Space Purple",
        isDark = true,
        primary = Color(0xFFA855F7),
        primaryContainer = Color(0xFF581C87),
        background = Color(0xFF1E1B4B),
        surface = Color(0xFF2E1065),
        surfaceVariant = Color(0xFF3B0764),
        surfaceBorder = Color(0xFF4C1D95),
        accentCyan = Color(0xFF38BDF8),
        accentPurple = Color(0xFFEC4899),
        codeBg = Color(0xFF0F0728),
        textPrimary = Color(0xFFFAF5FF),
        textSecondary = Color(0xFFD8B4FE)
    ),
    OLED_BLACK(
        displayName = "Midnight OLED Black",
        isDark = true,
        primary = Color(0xFF6366F1),
        primaryContainer = Color(0xFF312E81),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF1E1E1E),
        surfaceBorder = Color(0xFF27272A),
        accentCyan = Color(0xFF38BDF8),
        accentPurple = Color(0xFF818CF8),
        codeBg = Color(0xFF050505),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA1A1AA)
    ),
    GITHUB_LIGHT(
        displayName = "GitHub Light",
        isDark = false,
        primary = Color(0xFF0969DA),
        primaryContainer = Color(0xFFDDF4FF),
        background = Color(0xFFF6F8FA),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF3F4F6),
        surfaceBorder = Color(0xFFD0D7DE),
        accentCyan = Color(0xFF0550AE),
        accentPurple = Color(0xFF8250DF),
        codeBg = Color(0xFFF0F3F6),
        textPrimary = Color(0xFF1F2328),
        textSecondary = Color(0xFF656D76)
    )
}

enum class ThemeMode(val displayName: String) {
    FOLLOW_SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

val LocalAppPalette = staticCompositionLocalOf { AppThemePalette.MATERIAL_EXPRESSIVE }
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.DARK }

object ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _currentPalette = MutableStateFlow(AppThemePalette.MATERIAL_EXPRESSIVE)
    val currentPalette: StateFlow<AppThemePalette> = _currentPalette.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        when (mode) {
            ThemeMode.LIGHT -> {
                if (_currentPalette.value.isDark) {
                    _currentPalette.value = AppThemePalette.GITHUB_LIGHT
                }
            }
            ThemeMode.AMOLED -> {
                _currentPalette.value = AppThemePalette.OLED_BLACK
            }
            ThemeMode.DARK -> {
                if (!_currentPalette.value.isDark) {
                    _currentPalette.value = AppThemePalette.MATERIAL_EXPRESSIVE
                }
            }
            ThemeMode.FOLLOW_SYSTEM -> {
                // Keep selected palette
            }
        }
    }

    fun setPalette(palette: AppThemePalette) {
        _currentPalette.value = palette
        if (palette == AppThemePalette.OLED_BLACK) {
            _themeMode.value = ThemeMode.AMOLED
        } else if (!palette.isDark && _themeMode.value != ThemeMode.LIGHT) {
            _themeMode.value = ThemeMode.LIGHT
        } else if (palette.isDark && _themeMode.value == ThemeMode.LIGHT) {
            _themeMode.value = ThemeMode.DARK
        }
    }
}
