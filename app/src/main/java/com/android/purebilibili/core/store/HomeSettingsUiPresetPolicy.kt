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
    val effectiveLiquidGlassEnabled = resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = homeSettings.isLiquidGlassEnabled,
        uiPreset = uiPreset
    )
    return if (effectiveLiquidGlassEnabled == homeSettings.isLiquidGlassEnabled) {
        homeSettings
    } else {
        homeSettings.copy(isLiquidGlassEnabled = effectiveLiquidGlassEnabled)
    }
}
