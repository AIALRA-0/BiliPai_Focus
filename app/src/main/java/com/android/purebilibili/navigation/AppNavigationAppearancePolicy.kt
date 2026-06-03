package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

internal data class AppNavigationAppearance(
    val cardTransitionEnabled: Boolean,
    val videoTransitionRealtimeBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val bottomBarLabelMode: Int,
    val bottomBarFloating: Boolean
)

internal fun resolveAppNavigationAppearance(
    homeSettings: HomeSettings,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): AppNavigationAppearance {
    return AppNavigationAppearance(
        cardTransitionEnabled = homeSettings.cardTransitionEnabled,
        videoTransitionRealtimeBlurEnabled = homeSettings.videoTransitionRealtimeBlurEnabled,
        bottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled,
        bottomBarLabelMode = homeSettings.bottomBarLabelMode,
        bottomBarFloating = homeSettings.isBottomBarFloating
    )
}
