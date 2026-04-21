package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset

internal fun resolveEffectiveLiquidGlassEnabled(
    requestedEnabled: Boolean,
    uiPreset: UiPreset
): Boolean {
    return requestedEnabled
}

internal fun resolveEffectiveHomeSettings(
    homeSettings: HomeSettings,
    uiPreset: UiPreset
): HomeSettings {
    val effectiveTopBarLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = homeSettings.isTopBarLiquidGlassEnabled,
        uiPreset = uiPreset
    )
    val effectiveBottomBarLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
        uiPreset = uiPreset
    )
    return if (
        effectiveTopBarLiquidGlassEnabled == homeSettings.isTopBarLiquidGlassEnabled &&
        effectiveBottomBarLiquidGlassEnabled == homeSettings.isBottomBarLiquidGlassEnabled
    ) {
        homeSettings
    } else {
        homeSettings.copy(
            isTopBarLiquidGlassEnabled = effectiveTopBarLiquidGlassEnabled,
            isBottomBarLiquidGlassEnabled = effectiveBottomBarLiquidGlassEnabled
        )
    }
}
