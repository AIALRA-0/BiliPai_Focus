package com.android.purebilibili.feature.live

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
internal data class LiveChromePalette(
    val isDark: Boolean,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val searchField: Color,
    val bubble: Color,
    val bubbleStrong: Color,
    val border: Color,
    val scrim: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val onAccent: Color
)

@Composable
internal fun rememberLiveChromePalette(): LiveChromePalette {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.45f
    return remember(colorScheme, isDark) {
        if (isDark) {
            LiveChromePalette(
                isDark = true,
                accent = Color(0xFFE4C06F),
                accentStrong = Color(0xFFF0D18A),
                accentSoft = Color(0x33E4C06F),
                backgroundTop = Color(0xFF120F0C),
                backgroundBottom = Color(0xFF211B14),
                surface = Color(0xE628221A),
                surfaceElevated = Color(0xF2332C22),
                surfaceMuted = Color(0xCC2C261D),
                searchField = Color(0xFF2D271E),
                bubble = Color(0xC9181C23),
                bubbleStrong = Color(0xDD232A33),
                border = Color(0x26FFF2D9),
                scrim = Color(0xBF04070B),
                primaryText = Color(0xFFF6EFE2),
                secondaryText = Color(0xFFD2C4AB),
                tertiaryText = Color(0xFF9C907D),
                onAccent = Color(0xFF1F180D)
            )
        } else {
            LiveChromePalette(
                isDark = false,
                accent = Color(0xFF8C6B27),
                accentStrong = Color(0xFFA57A25),
                accentSoft = Color(0x1A8C6B27),
                backgroundTop = Color(0xFFF8F2E8),
                backgroundBottom = Color(0xFFF2EBDF),
                surface = Color(0xF9FFF9F2),
                surfaceElevated = Color(0xFFF7F0E5),
                surfaceMuted = Color(0xFFF0E7DA),
                searchField = Color(0xFFEDE3D4),
                bubble = Color(0xF8FFFFFF),
                bubbleStrong = Color(0xFFF8F1E4),
                border = Color(0x1F5E4630),
                scrim = Color(0x802F2417),
                primaryText = Color(0xFF241A11),
                secondaryText = Color(0xFF655646),
                tertiaryText = Color(0xFF9A8C7B),
                onAccent = Color.White
            )
        }
    }
}

internal fun LiveChromePalette.backgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(backgroundTop, backgroundBottom)
    )
}
