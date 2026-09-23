package com.android.purebilibili.feature.settings

internal data class TopBarVisualEffectState(
    val headerBlurEnabled: Boolean,
    val progressiveTopBlurEnabled: Boolean
)

internal data class BottomBarVisualEffectState(
    val bottomBarBlurEnabled: Boolean,
    val liquidGlassEnabled: Boolean
)

internal fun resolveTopBarBlurToggleState(
    enableHeaderBlur: Boolean,
    currentProgressiveTopBlurEnabled: Boolean = false
): TopBarVisualEffectState {
    return TopBarVisualEffectState(
        headerBlurEnabled = enableHeaderBlur,
        progressiveTopBlurEnabled = if (enableHeaderBlur) false else currentProgressiveTopBlurEnabled
    )
}

internal fun resolveProgressiveTopBlurToggleState(
    enableProgressiveTopBlur: Boolean,
    currentHeaderBlurEnabled: Boolean = false
): TopBarVisualEffectState {
    return TopBarVisualEffectState(
        headerBlurEnabled = if (enableProgressiveTopBlur) false else currentHeaderBlurEnabled,
        progressiveTopBlurEnabled = enableProgressiveTopBlur
    )
}

internal fun resolveBottomBarBlurToggleState(
    enableBottomBarBlur: Boolean,
    currentLiquidGlassEnabled: Boolean
): BottomBarVisualEffectState {
    // The two effects are mutually exclusive only when enabling one of them.
    // Turning an effect off should preserve the user's current choice for the other one.
    val nextLiquidGlassEnabled = if (enableBottomBarBlur) {
        false
    } else {
        currentLiquidGlassEnabled
    }
    return BottomBarVisualEffectState(
        bottomBarBlurEnabled = enableBottomBarBlur,
        liquidGlassEnabled = nextLiquidGlassEnabled
    )
}

internal fun resolveLiquidGlassToggleState(
    enableLiquidGlass: Boolean,
    currentBottomBarBlurEnabled: Boolean
): BottomBarVisualEffectState {
    return BottomBarVisualEffectState(
        bottomBarBlurEnabled = if (enableLiquidGlass) false else currentBottomBarBlurEnabled,
        liquidGlassEnabled = enableLiquidGlass
    )
}
