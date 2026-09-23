package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.theme.AppUiStyle

internal enum class HomeSelectionIndicatorStyle {
    CAPSULE,
    MD3_UNDERLINE,
}

internal fun resolveHomeSelectionIndicatorStyle(
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean,
    forceMaterialUnderline: Boolean = false,
): HomeSelectionIndicatorStyle = when {
    forceMaterialUnderline -> HomeSelectionIndicatorStyle.MD3_UNDERLINE
    liquidGlassEnabled -> HomeSelectionIndicatorStyle.CAPSULE
    uiStyle == AppUiStyle.MIUIX -> HomeSelectionIndicatorStyle.CAPSULE
    else -> HomeSelectionIndicatorStyle.MD3_UNDERLINE
}
