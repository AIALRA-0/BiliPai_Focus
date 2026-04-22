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
        resolveLiveChromePalette(
            isDark = isDark,
            primary = colorScheme.primary,
            onPrimary = colorScheme.onPrimary,
            background = colorScheme.background,
            surface = colorScheme.surface,
            surfaceContainerLow = colorScheme.surfaceContainerLow,
            surfaceContainer = colorScheme.surfaceContainer,
            surfaceContainerHigh = colorScheme.surfaceContainerHigh,
            surfaceContainerHighest = colorScheme.surfaceContainerHighest,
            surfaceVariant = colorScheme.surfaceVariant,
            onBackground = colorScheme.onBackground,
            onSurface = colorScheme.onSurface,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            outline = colorScheme.outline,
            outlineVariant = colorScheme.outlineVariant
        )
    }
}

internal fun resolveLiveChromePalette(
    isDark: Boolean,
    primary: Color,
    onPrimary: Color,
    background: Color,
    surface: Color,
    surfaceContainerLow: Color,
    surfaceContainer: Color,
    surfaceContainerHigh: Color,
    surfaceContainerHighest: Color,
    surfaceVariant: Color,
    onBackground: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    outline: Color,
    outlineVariant: Color
): LiveChromePalette {
    return LiveChromePalette(
        isDark = isDark,
        accent = primary,
        accentStrong = primary,
        accentSoft = primary.copy(alpha = if (isDark) 0.24f else 0.16f),
        backgroundTop = background,
        backgroundBottom = if (isDark) surface else surfaceContainerLow,
        surface = surface.copy(alpha = if (isDark) 0.92f else 0.98f),
        surfaceElevated = if (isDark) surfaceContainerHigh.copy(alpha = 0.96f) else surface,
        surfaceMuted = if (isDark) surfaceContainer.copy(alpha = 0.92f) else surfaceContainerHighest,
        searchField = if (isDark) surfaceContainerHigh else surfaceContainerHighest,
        bubble = if (isDark) surfaceContainerHigh.copy(alpha = 0.82f) else surface.copy(alpha = 0.96f),
        bubbleStrong = if (isDark) surfaceContainerHighest.copy(alpha = 0.88f) else surfaceVariant,
        border = outlineVariant.copy(alpha = if (isDark) 0.42f else 0.55f),
        scrim = Color.Black.copy(alpha = if (isDark) 0.78f else 0.52f),
        primaryText = onBackground,
        secondaryText = onSurfaceVariant,
        tertiaryText = outline,
        onAccent = onPrimary
    )
}

internal fun LiveChromePalette.backgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(backgroundTop, backgroundBottom)
    )
}
