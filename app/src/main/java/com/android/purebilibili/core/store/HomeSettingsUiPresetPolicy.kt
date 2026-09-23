package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.AppUiStyle

/**
 * Global liquid-glass reuse master switch ("安卓液态玻璃").
 * When enabled, every reusable chrome surface shares the bottom-bar liquid material.
 */
internal fun resolveGlobalLiquidGlassReuseEnabled(
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = androidNativeLiquidGlassEnabled

internal enum class LiquidGlassReuseSurface {
    HOME_BOTTOM_BAR,
    COMMENT_BOTTOM_BAR,
    HOME_TOP_DOCK,
    HOME_SEARCH,
}

internal fun resolveLiquidGlassReuseParticipates(
    surface: LiquidGlassReuseSurface,
    androidNativeLiquidGlassEnabled: Boolean,
): Boolean = resolveGlobalLiquidGlassReuseEnabled(androidNativeLiquidGlassEnabled)

/** Effective enablement for reusable chrome. The global switch is the sole entry point. */
@Suppress("UNUSED_PARAMETER")
internal fun resolveSharedLiquidGlassChromeEnabled(
    individualEnabled: Boolean,
    uiStyle: AppUiStyle,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = resolveGlobalLiquidGlassReuseEnabled(androidNativeLiquidGlassEnabled)

internal fun resolveEffectiveHomeSettings(
    homeSettings: HomeSettings,
): HomeSettings = homeSettings
