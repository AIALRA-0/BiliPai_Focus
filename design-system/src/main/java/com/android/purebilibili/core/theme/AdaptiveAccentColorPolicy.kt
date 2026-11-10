package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class AdaptiveAccentColors(
    val backgroundColor: Color,
    val contentColor: Color
)

/** Preserve Material's paired color roles; small labels require 4.5:1 in both themes. */
internal fun resolveAdaptiveAccentColors(
    accentBackground: Color,
    accentContent: Color,
    containerBackground: Color,
    containerContent: Color,
    surface: Color,
    minimumContrast: Float = ACCESSIBLE_TEXT_MIN_CONTRAST
): AdaptiveAccentColors {
    val background = opaqueCompositeOver(accentBackground, surface)
    val content = opaqueCompositeOver(accentContent, background)
    val useAccent = calculateContrastRatio(content, background) >= minimumContrast
    val resolved = resolveAccessibleContainerColors(
        containerColor = if (useAccent) accentBackground else containerBackground,
        contentColor = if (useAccent) accentContent else containerContent,
        backgroundColor = surface,
        fallbackContentColors = listOf(Color.Black, Color.White),
        minimumContrast = minimumContrast,
    )
    return AdaptiveAccentColors(resolved.containerColor, resolved.contentColor)
}

fun resolveAdaptivePrimaryAccentColors(
    colorScheme: ColorScheme,
    minimumContrast: Float = ACCESSIBLE_TEXT_MIN_CONTRAST
): AdaptiveAccentColors = resolveAdaptiveAccentColors(
    accentBackground = colorScheme.primary,
    accentContent = colorScheme.onPrimary,
    containerBackground = colorScheme.primaryContainer,
    containerContent = colorScheme.onPrimaryContainer,
    surface = colorScheme.surface,
    minimumContrast = minimumContrast
)

fun resolveAdaptiveTertiaryAccentColors(
    colorScheme: ColorScheme,
    minimumContrast: Float = ACCESSIBLE_TEXT_MIN_CONTRAST
): AdaptiveAccentColors = resolveAdaptiveAccentColors(
    accentBackground = colorScheme.tertiary,
    accentContent = colorScheme.onTertiary,
    containerBackground = colorScheme.tertiaryContainer,
    containerContent = colorScheme.onTertiaryContainer,
    surface = colorScheme.surface,
    minimumContrast = minimumContrast
)

/** Tonal selection uses the same container/on-container pair in light and dark themes. */
fun resolveFilledSelectionAccentColors(colorScheme: ColorScheme): AdaptiveAccentColors {
    val resolved = resolveAccessibleContainerColors(
        containerColor = colorScheme.secondaryContainer,
        contentColor = colorScheme.onSecondaryContainer,
        backgroundColor = colorScheme.surface,
        fallbackContentColors = listOf(colorScheme.onSurface, Color.Black, Color.White),
    )
    return AdaptiveAccentColors(resolved.containerColor, resolved.contentColor)
}
