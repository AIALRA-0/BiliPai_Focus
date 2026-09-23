package com.android.purebilibili.core.ui.adaptive

import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.WindowWidthSizeClass

data class DeviceUiProfile(
    val widthSizeClass: WindowWidthSizeClass,
    val isTablet: Boolean,
    val motionTier: MotionTier,
    val foldPosture: AdaptiveFoldPosture = AdaptiveFoldPosture.None,
)

fun resolveDeviceUiProfile(
    widthSizeClass: WindowWidthSizeClass,
    foldPosture: AppFoldPosture = AppFoldPosture.None,
): DeviceUiProfile {
    val spec = resolveDeviceUiProfileSpec(
        widthClass = widthSizeClass.toAdaptiveWidthClass(),
        foldPosture = foldPosture.toAdaptiveFoldPosture(),
    )
    return DeviceUiProfile(
        widthSizeClass = widthSizeClass,
        isTablet = spec.isTablet,
        motionTier = spec.motionTier,
        foldPosture = spec.foldPosture,
    )
}

internal fun WindowWidthSizeClass.toAdaptiveWidthClass(): AdaptiveWidthClass = when (this) {
    WindowWidthSizeClass.Compact -> AdaptiveWidthClass.Compact
    WindowWidthSizeClass.Medium -> AdaptiveWidthClass.Medium
    WindowWidthSizeClass.Expanded -> AdaptiveWidthClass.Expanded
    WindowWidthSizeClass.Large -> AdaptiveWidthClass.Large
    WindowWidthSizeClass.ExtraLarge -> AdaptiveWidthClass.ExtraLarge
}

internal fun AppFoldPosture.toAdaptiveFoldPosture(): AdaptiveFoldPosture = when (this) {
    AppFoldPosture.None -> AdaptiveFoldPosture.None
    AppFoldPosture.Flat -> AdaptiveFoldPosture.Flat
    AppFoldPosture.Book -> AdaptiveFoldPosture.Book
    AppFoldPosture.Tabletop -> AdaptiveFoldPosture.Tabletop
}
