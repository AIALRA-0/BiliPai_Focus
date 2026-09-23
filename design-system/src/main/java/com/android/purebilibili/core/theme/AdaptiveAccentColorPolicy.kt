package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class AdaptiveAccentColors(
    val backgroundColor: Color,
    val contentColor: Color
)

/**
 * Resolve a readable accent fill + label pair.
 *
 * On dark surfaces, filled selection chrome (tabs / chips / liquid indicators)
 * must not keep heavy dark labels on bright primary just because black wins the
 * pure contrast contest — prefer a light label when it still meets UI contrast,
 * darkening the fill slightly if needed so orange/coral brand pills stay
 * "light-on-accent" rather than black-on-orange.
 */
internal fun resolveAdaptiveAccentColors(
    accentBackground: Color,
    accentContent: Color,
    containerBackground: Color,
    containerContent: Color,
    surface: Color,
    minimumContrast: Float = ACCESSIBLE_TEXT_MIN_CONTRAST
): AdaptiveAccentColors {
    val accentContrast = calculateContrastRatio(accentContent, accentBackground)
    val shouldAvoidPureBrightAccent = surface.luminance() < 0.35f &&
        accentBackground.luminance() > 0.88f
    val useContainerColors = shouldAvoidPureBrightAccent || accentContrast < minimumContrast

    val base = if (useContainerColors) {
        AdaptiveAccentColors(
            backgroundColor = containerBackground,
            contentColor = containerContent,
        )
    } else {
        AdaptiveAccentColors(
            backgroundColor = accentBackground,
            contentColor = accentContent,
        )
    }

    return preferLightFilledSelectionContent(
        colors = base,
        surface = surface,
        // Chip / tab labels are UI-scale; 3:1 is the WCAG UI threshold.
        lightLabelMinContrast = ACCESSIBLE_UI_MIN_CONTRAST,
    )
}

/**
 * Dark-surface filled selection: avoid black labels on bright brand fills.
 * Prefer white when usable; otherwise darken the fill until white is readable.
 */
internal fun preferLightFilledSelectionContent(
    colors: AdaptiveAccentColors,
    surface: Color,
    lightLabelMinContrast: Float = ACCESSIBLE_UI_MIN_CONTRAST,
): AdaptiveAccentColors {
    val surfaceIsDark = surface.luminance() < 0.35f
    if (!surfaceIsDark) return colors

    val contentIsDark = colors.contentColor.luminance() < 0.45f
    if (!contentIsDark) return colors

    val white = Color.White
    val opaqueBackground = if (colors.backgroundColor.alpha < 1f) {
        opaqueCompositeOver(colors.backgroundColor, surface)
    } else {
        colors.backgroundColor.copy(alpha = 1f)
    }

    if (calculateContrastRatio(white, opaqueBackground) >= lightLabelMinContrast) {
        return AdaptiveAccentColors(
            backgroundColor = opaqueBackground,
            contentColor = white,
        )
    }

    // Progressively darken brand fill so white label stays readable.
    var darkened = opaqueBackground
    repeat(8) {
        darkened = Color(
            red = darkened.red * 0.88f,
            green = darkened.green * 0.88f,
            blue = darkened.blue * 0.88f,
            alpha = 1f,
        )
        if (calculateContrastRatio(white, darkened) >= lightLabelMinContrast) {
            return AdaptiveAccentColors(
                backgroundColor = darkened,
                contentColor = white,
            )
        }
    }

    // Could not force light label safely — keep the contrast-winning dark pair.
    return colors.copy(
        backgroundColor = opaqueBackground,
        contentColor = colors.contentColor.copy(alpha = 1f),
    )
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

/**
 * Filled selection chrome (type tabs, filter chips, soft pills).
 *
 * Always uses the theme **primaryContainer** pair so selected pills stay soft/tonal
 * in both light and dark (screenshot soft-capsule style), never neon solid primary.
 * All colors come from [ColorScheme] — no hard-coded hex fills.
 */
fun resolveFilledSelectionAccentColors(
    colorScheme: ColorScheme,
): AdaptiveAccentColors {
    return preferLightFilledSelectionContent(
        colors = AdaptiveAccentColors(
            backgroundColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
        ),
        surface = colorScheme.surface,
    )
}
