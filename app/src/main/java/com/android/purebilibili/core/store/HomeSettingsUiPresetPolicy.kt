package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset

internal fun resolveEffectiveLiquidGlassEnabled(
    requestedEnabled: Boolean,
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean = false
): Boolean {
    if (!requestedEnabled) return false
    return uiPreset == UiPreset.IOS || androidNativeLiquidGlassEnabled
}

internal fun resolveEffectiveHomeSettings(
    homeSettings: HomeSettings,
    uiPreset: UiPreset
): HomeSettings {
    val effectiveBottomBarLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
    )
    return if (effectiveBottomBarLiquidGlassEnabled == homeSettings.isBottomBarLiquidGlassEnabled) {
        homeSettings
    } else {
        homeSettings.copy(
            isBottomBarLiquidGlassEnabled = effectiveBottomBarLiquidGlassEnabled
        )
    }
}
