package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset

internal data class LiquidGlassSelectionContentColors(
    val visibleColor: Color,
    val exportColor: Color
)

internal fun resolveLiquidGlassSelectionContentColors(
    unselectedColor: Color,
    selectedColor: Color,
    themeWeight: Float,
    glassEnabled: Boolean,
    indicatorProgress: Float,
    indicatorBackdropEnabled: Boolean = true
): LiquidGlassSelectionContentColors {
    return LiquidGlassSelectionContentColors(
        visibleColor = resolveBottomBarGlassVisibleContentColor(
            unselectedColor = unselectedColor,
            selectedColor = selectedColor,
            themeWeight = themeWeight,
            glassEnabled = glassEnabled,
            indicatorProgress = indicatorProgress,
            indicatorBackdropEnabled = indicatorBackdropEnabled
        ),
        exportColor = resolveBottomBarGlassExportContentColor(
            unselectedColor = unselectedColor,
            selectedColor = selectedColor,
            themeWeight = themeWeight,
            glassEnabled = glassEnabled
        )
    )
}

internal fun resolveBottomBarGlassVisibleContentColor(
    unselectedColor: Color,
    selectedColor: Color,
    themeWeight: Float,
    glassEnabled: Boolean,
    indicatorProgress: Float,
    indicatorBackdropEnabled: Boolean = true
): Color {
    if (glassEnabled && indicatorBackdropEnabled && indicatorProgress > 0.001f) {
        return unselectedColor
    }
    return lerpColor(
        start = unselectedColor,
        stop = selectedColor,
        fraction = themeWeight.coerceIn(0f, 1f)
    )
}

internal fun resolveBottomBarGlassExportContentColor(
    unselectedColor: Color,
    selectedColor: Color,
    themeWeight: Float,
    glassEnabled: Boolean
): Color {
    if (glassEnabled) return unselectedColor
    return lerpColor(
        start = unselectedColor,
        stop = selectedColor,
        fraction = themeWeight.coerceIn(0f, 1f)
    )
}

internal fun shouldRenderBottomBarForegroundAboveIndicator(
    preset: BottomBarLiquidGlassPreset
): Boolean {
    return when (preset) {
        BottomBarLiquidGlassPreset.BILIPAI_TUNED,
        BottomBarLiquidGlassPreset.IOS26_REFINED -> false
    }
}
