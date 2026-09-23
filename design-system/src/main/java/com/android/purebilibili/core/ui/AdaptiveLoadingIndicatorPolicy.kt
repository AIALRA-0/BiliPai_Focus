package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AppUiStyle

/**
 * Visual recipe for indeterminate loading chrome across UI presets.
 *
 * - [MATERIAL3_LOADING_INDICATOR]: official M3 morphing [androidx.compose.material3.LoadingIndicator]
 *   with [ColorScheme.primary] (dynamic color when enabled).
 * - [MATERIAL3_CIRCULAR]: official M3 [androidx.compose.material3.CircularProgressIndicator]
 *   for compact / inline slots where the morphing indicator is too large.
 * - [MIUIX_INFINITE]: Miuix orbiting-dot [top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator]
 *   for page-level loading.
 * - [MIUIX_CIRCULAR]: Miuix [top.yukonga.miuix.kmp.basic.CircularProgressIndicator]
 *   for compact / inline slots.
 */
enum class AdaptiveLoadingVisual {
    MATERIAL3_LOADING_INDICATOR,
    MATERIAL3_CIRCULAR,
    MIUIX_INFINITE,
    MIUIX_CIRCULAR,
}

/**
 * Density of the loading slot.
 *
 * [PAGE] is full-screen / empty-state content loading.
 * [COMPACT] is list footers, buttons, and other tight slots (typically ≤ 32.dp).
 */
enum class AdaptiveLoadingDensity {
    PAGE,
    COMPACT,
}

fun resolveAdaptiveLoadingVisual(
    uiStyle: AppUiStyle,
    density: AdaptiveLoadingDensity = AdaptiveLoadingDensity.PAGE,
): AdaptiveLoadingVisual {
    val renderer = resolvePresetPrimitiveRenderer(uiStyle)
    return resolveAdaptiveLoadingVisual(renderer = renderer, density = density)
}

fun resolveAdaptiveLoadingVisual(
    renderer: PresetPrimitiveRenderer,
    density: AdaptiveLoadingDensity = AdaptiveLoadingDensity.PAGE,
): AdaptiveLoadingVisual = when (renderer) {
    PresetPrimitiveRenderer.MATERIAL3 -> when (density) {
        AdaptiveLoadingDensity.PAGE -> AdaptiveLoadingVisual.MATERIAL3_LOADING_INDICATOR
        AdaptiveLoadingDensity.COMPACT -> AdaptiveLoadingVisual.MATERIAL3_CIRCULAR
    }
    PresetPrimitiveRenderer.MIUIX_BRIDGED -> when (density) {
        AdaptiveLoadingDensity.PAGE -> AdaptiveLoadingVisual.MIUIX_INFINITE
        AdaptiveLoadingDensity.COMPACT -> AdaptiveLoadingVisual.MIUIX_CIRCULAR
    }
}

/**
 * Heuristic: treat sizes at or below this as compact slots (list footers, chips, etc.).
 */
const val AdaptiveLoadingCompactSizeThresholdDp = 32f

fun resolveAdaptiveLoadingDensity(sizeDp: Float?): AdaptiveLoadingDensity {
    if (sizeDp == null) return AdaptiveLoadingDensity.PAGE
    return if (sizeDp <= AdaptiveLoadingCompactSizeThresholdDp) {
        AdaptiveLoadingDensity.COMPACT
    } else {
        AdaptiveLoadingDensity.PAGE
    }
}
