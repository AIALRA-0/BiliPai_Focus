package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

const val ACCESSIBLE_TEXT_MIN_CONTRAST = 4.5f
const val ACCESSIBLE_UI_MIN_CONTRAST = 3.0f

private const val PRIMARY_TEXT_MIN_CONTRAST = ACCESSIBLE_TEXT_MIN_CONTRAST
private const val SECONDARY_TEXT_MIN_CONTRAST = ACCESSIBLE_TEXT_MIN_CONTRAST

data class AccessibleContainerColors(
    val containerColor: Color,
    val contentColor: Color,
)

/** Flattens [foreground] over [background] so contrast checks use the color users see. */
fun opaqueCompositeOver(
    foreground: Color,
    background: Color,
): Color {
    val foregroundAlpha = foreground.alpha
    val backgroundAlpha = background.alpha
    val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)
    if (outputAlpha == 0f) return Color.Transparent
    return Color(
        red = (foreground.red * foregroundAlpha + background.red * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        green = (foreground.green * foregroundAlpha + background.green * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        blue = (foreground.blue * foregroundAlpha + background.blue * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        alpha = 1f,
    )
}

/**
 * Resolves an opaque visual equivalent for a translucent semantic container and a readable
 * foreground for it. The first fallback that reaches [minimumContrast] wins; if none do, the
 * highest-contrast candidate is used.
 */
fun resolveAccessibleContainerColors(
    containerColor: Color,
    contentColor: Color,
    backgroundColor: Color,
    fallbackContentColors: List<Color>,
    minimumContrast: Float = ACCESSIBLE_TEXT_MIN_CONTRAST,
): AccessibleContainerColors {
    val resolvedContainer = opaqueCompositeOver(containerColor, backgroundColor)
    val candidates = (listOf(contentColor) + fallbackContentColors).distinct()
    val resolvedContent = candidates.firstOrNull {
        calculateContrastRatio(opaqueCompositeOver(it, resolvedContainer), resolvedContainer) >= minimumContrast
    } ?: candidates.maxByOrNull {
        calculateContrastRatio(opaqueCompositeOver(it, resolvedContainer), resolvedContainer)
    } ?: contentColor
    return AccessibleContainerColors(
        containerColor = resolvedContainer,
        contentColor = opaqueCompositeOver(resolvedContent, resolvedContainer),
    )
}

fun calculateContrastRatio(
    foreground: Color,
    background: Color
): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

fun resolveReadableTextColor(
    candidate: Color,
    background: Color,
    fallback: Color,
    minimumContrast: Float
): Color {
    return if (calculateContrastRatio(candidate, background) >= minimumContrast) {
        candidate
    } else {
        fallback
    }
}

internal fun resolveReadableThemeTextColor(
    candidate: Color,
    background: Color,
    fallbacks: List<Color>,
    minimumContrast: Float
): Color {
    val candidates = listOf(candidate) + fallbacks + listOf(Color.Black, Color.White)
    return candidates.map { opaqueCompositeOver(it, background) }.firstOrNull {
        calculateContrastRatio(it, background) >= minimumContrast
    } ?: candidate
}

/**
 * 浅色与深色模式通用的正文对比度兜底：候选文字与其底色不足 [ACCESSIBLE_TEXT_MIN_CONTRAST]
 * 时，依序回退到可读的语义角色，避免自定义种子色/覆盖色产生不可读文本。
 */
fun enforceDynamicTextContrast(
    scheme: ColorScheme
): ColorScheme {
    val accentFallbacks = listOf(
        scheme.onSurface,
        scheme.onBackground,
        scheme.inverseOnSurface,
        scheme.scrim
    )
    val surfaceFallbacks = listOf(
        scheme.onBackground,
        scheme.onSurface,
        scheme.inverseOnSurface,
        scheme.scrim
    )
    val surfaceVariantFallbacks = listOf(
        scheme.onSurface,
        scheme.onBackground,
        scheme.inverseOnSurface,
        scheme.scrim
    )

    return scheme.copy(
        onBackground = resolveReadableThemeTextColor(
            candidate = scheme.onBackground,
            background = scheme.background,
            fallbacks = surfaceFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onSurface = resolveReadableThemeTextColor(
            candidate = scheme.onSurface,
            background = scheme.surface,
            fallbacks = surfaceFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onSurfaceVariant = resolveReadableThemeTextColor(
            candidate = scheme.onSurfaceVariant,
            background = scheme.surfaceVariant,
            fallbacks = surfaceVariantFallbacks,
            minimumContrast = SECONDARY_TEXT_MIN_CONTRAST
        ),
        onPrimary = resolveReadableThemeTextColor(
            candidate = scheme.onPrimary,
            background = scheme.primary,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onPrimaryContainer = resolveReadableThemeTextColor(
            candidate = scheme.onPrimaryContainer,
            background = scheme.primaryContainer,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onSecondary = resolveReadableThemeTextColor(
            candidate = scheme.onSecondary,
            background = scheme.secondary,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onSecondaryContainer = resolveReadableThemeTextColor(
            candidate = scheme.onSecondaryContainer,
            background = scheme.secondaryContainer,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onTertiary = resolveReadableThemeTextColor(
            candidate = scheme.onTertiary,
            background = scheme.tertiary,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onTertiaryContainer = resolveReadableThemeTextColor(
            candidate = scheme.onTertiaryContainer,
            background = scheme.tertiaryContainer,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onError = resolveReadableThemeTextColor(
            candidate = scheme.onError,
            background = scheme.error,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        ),
        onErrorContainer = resolveReadableThemeTextColor(
            candidate = scheme.onErrorContainer,
            background = scheme.errorContainer,
            fallbacks = accentFallbacks,
            minimumContrast = PRIMARY_TEXT_MIN_CONTRAST
        )
    )
}

/** Keep a readable native glyph unchanged; repair only a failed glyph/background pair.
 * The caller supplies the actual control fill and the surface underneath transparency. */
fun resolveControlGlyphColor(candidate: Color, container: Color, surface: Color): Color {
    if (container == Color.Unspecified || candidate == Color.Unspecified) return candidate
    val background = opaqueCompositeOver(container, surface)
    if (calculateContrastRatio(opaqueCompositeOver(candidate, background), background) >= ACCESSIBLE_UI_MIN_CONTRAST) {
        return candidate
    }
    return resolveAccessibleContainerColors(
        container, candidate, surface, listOf(Color.Black, Color.White), ACCESSIBLE_UI_MIN_CONTRAST,
    ).contentColor
}
