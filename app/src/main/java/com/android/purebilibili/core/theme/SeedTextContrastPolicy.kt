package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs

/**
 * Extended palettes keep every accent and container color. Only inadequate foreground
 * roles are retuned within their own color family. This runs after pure-black and
 * wallpaper edits, before bridging native components or animating the theme.
 * It is independent of glass; Standard and native Miuix neutral defaults keep their
 * existing policies. Shared text/icon roles use the stricter small-text threshold.
 */
internal fun enforceSeedTextContrast(scheme: ColorScheme): ColorScheme {
    var changed = false
    fun foreground(candidate: Color, family: Color, backgrounds: List<Color>): Color {
        val resolved = resolveSeedReadableForeground(candidate, family, backgrounds)
        if (resolved != candidate) changed = true
        return resolved
    }
    fun pairedForeground(candidate: Color, background: Color): Color =
        foreground(candidate, background, listOf(background))

    val surfaces = listOf(
        scheme.surface, scheme.background, scheme.surfaceVariant,
        scheme.surfaceDim, scheme.surfaceBright, scheme.surfaceContainerLowest,
        scheme.surfaceContainerLow, scheme.surfaceContainer,
        scheme.surfaceContainerHigh, scheme.surfaceContainerHighest,
    ).distinct()
    val primaryFixed = listOf(scheme.primaryFixed, scheme.primaryFixedDim)
    val secondaryFixed = listOf(scheme.secondaryFixed, scheme.secondaryFixedDim)
    val tertiaryFixed = listOf(scheme.tertiaryFixed, scheme.tertiaryFixedDim)
    val result = scheme.copy(
        onPrimary = pairedForeground(scheme.onPrimary, scheme.primary),
        onPrimaryContainer = pairedForeground(scheme.onPrimaryContainer, scheme.primaryContainer),
        onSecondary = pairedForeground(scheme.onSecondary, scheme.secondary),
        onSecondaryContainer = pairedForeground(scheme.onSecondaryContainer, scheme.secondaryContainer),
        onTertiary = pairedForeground(scheme.onTertiary, scheme.tertiary),
        onTertiaryContainer = pairedForeground(scheme.onTertiaryContainer, scheme.tertiaryContainer),
        onError = pairedForeground(scheme.onError, scheme.error),
        onErrorContainer = pairedForeground(scheme.onErrorContainer, scheme.errorContainer),
        onBackground = pairedForeground(scheme.onBackground, scheme.background),
        onSurface = foreground(scheme.onSurface, scheme.onSurface, surfaces),
        onSurfaceVariant = foreground(scheme.onSurfaceVariant, scheme.onSurfaceVariant, surfaces),
        inverseOnSurface = pairedForeground(scheme.inverseOnSurface, scheme.inverseSurface),
        onPrimaryFixed = foreground(scheme.onPrimaryFixed, scheme.primary, primaryFixed),
        onPrimaryFixedVariant = foreground(scheme.onPrimaryFixedVariant, scheme.primary, primaryFixed),
        onSecondaryFixed = foreground(scheme.onSecondaryFixed, scheme.secondary, secondaryFixed),
        onSecondaryFixedVariant = foreground(scheme.onSecondaryFixedVariant, scheme.secondary, secondaryFixed),
        onTertiaryFixed = foreground(scheme.onTertiaryFixed, scheme.tertiary, tertiaryFixed),
        onTertiaryFixedVariant = foreground(scheme.onTertiaryFixedVariant, scheme.tertiary, tertiaryFixed),
    )
    // Keep identity when already readable, including when resolving an endpoint twice.
    return if (changed) result else scheme
}

/** The closest passing tone toward black/white; hue and chroma come from the role's family. */
internal fun resolveSeedReadableForeground(
    candidate: Color,
    family: Color,
    backgrounds: List<Color>,
): Color {
    require(backgrounds.isNotEmpty())
    fun minimumRatio(color: Color): Float = backgrounds.minOf { background ->
        calculateContrastRatio(opaqueCompositeOver(color, background), background)
    }
    if (minimumRatio(candidate) >= ACCESSIBLE_TEXT_MIN_CONTRAST) return candidate

    val sourceTone = Hct.fromInt(candidate.toArgb()).tone
    val palette = Hct.fromInt(family.toArgb())
    val extremes = listOf(0.0 to Color.Black, 100.0 to Color.White)
        .sortedBy { (tone, _) -> abs(tone - sourceTone) }
    val target = extremes.firstOrNull { (_, color) -> minimumRatio(color) >= ACCESSIBLE_TEXT_MIN_CONTRAST }
        // Current paired surfaces/fixed tones share a brightness range. For arbitrary
        // incompatible backgrounds, return the best foreground without changing them.
        ?: return extremes.maxBy { (_, color) -> minimumRatio(color) }.second
    var failing = 0.0
    var passing = 1.0
    var best = target.second
    repeat(16) {
        val fraction = (failing + passing) / 2.0
        val tone = sourceTone + (target.first - sourceTone) * fraction
        val color = Color(Hct.from(palette.hue, palette.chroma, tone).toInt())
        if (minimumRatio(color) >= ACCESSIBLE_TEXT_MIN_CONTRAST) {
            passing = fraction
            best = color
        } else {
            failing = fraction
        }
    }
    return best
}
