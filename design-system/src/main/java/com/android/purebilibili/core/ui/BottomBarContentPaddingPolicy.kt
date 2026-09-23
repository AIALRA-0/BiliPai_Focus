package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle

data class BottomBarContentPaddingSpec(
    val floatingBodyHeight: Dp,
    val dockedBodyHeight: Dp,
    val floatingInset: Dp,
    val contentGap: Dp,
)

fun resolveBottomBarContentPaddingSpec(
    bottomBarLabelMode: Int,
    isTablet: Boolean,
    uiStyle: AppUiStyle,
    hasUiSkinDecoration: Boolean,
): BottomBarContentPaddingSpec {
    // These are the actual shell extents used by the renderers. Label mode
    // changes item content, but it does not change the navigation shell's
    // occupied height.
    return resolveBottomBarContentPaddingSpec(
        compactDockedBar = uiStyle == AppUiStyle.MIUIX,
        hasUiSkinDecoration = hasUiSkinDecoration,
    )
}

private fun resolveBottomBarContentPaddingSpec(
    compactDockedBar: Boolean,
    hasUiSkinDecoration: Boolean,
): BottomBarContentPaddingSpec {
    val floatingBodyHeight = if (hasUiSkinDecoration) 88.dp else 64.dp
    val dockedBodyHeight = if (compactDockedBar) {
        if (hasUiSkinDecoration) 88.dp else 64.dp
    } else {
        80.dp
    }
    return BottomBarContentPaddingSpec(
        floatingBodyHeight = floatingBodyHeight,
        dockedBodyHeight = dockedBodyHeight,
        floatingInset = 12.dp,
        contentGap = AppSpacingTokens.Medium,
    )
}

fun resolveBottomBarContentPadding(
    navigationBarsBottom: Dp,
    reserveBottomBar: Boolean,
    isBottomBarFloating: Boolean,
    bottomBarLabelMode: Int,
    isTablet: Boolean,
    uiStyle: AppUiStyle,
    hasUiSkinDecoration: Boolean,
    extraContentPadding: Dp = AppSpacingTokens.Small,
): Dp {
    val spec = resolveBottomBarContentPaddingSpec(
        bottomBarLabelMode = bottomBarLabelMode,
        isTablet = isTablet,
        uiStyle = uiStyle,
        hasUiSkinDecoration = hasUiSkinDecoration,
    )
    return calculateBottomBarContentPadding(
        navigationBarsBottom = navigationBarsBottom,
        reserveBottomBar = reserveBottomBar,
        isBottomBarFloating = isBottomBarFloating,
        spec = spec,
        extraContentPadding = extraContentPadding,
    )
}

@Composable
fun rememberAppBottomBarContentPadding(
    navigationBarsBottom: Dp,
    reserveBottomBar: Boolean,
    isBottomBarFloating: Boolean,
    hasUiSkinDecoration: Boolean,
    extraContentPadding: Dp = AppSpacingTokens.Small,
): Dp {
    val uiStyle = LocalAppUiStyle.current
    val spec = resolveBottomBarContentPaddingSpec(
        compactDockedBar = uiStyle == AppUiStyle.MIUIX,
        hasUiSkinDecoration = hasUiSkinDecoration,
    )
    return calculateBottomBarContentPadding(
        navigationBarsBottom = navigationBarsBottom,
        reserveBottomBar = reserveBottomBar,
        isBottomBarFloating = isBottomBarFloating,
        spec = spec,
        extraContentPadding = extraContentPadding,
    )
}

private fun calculateBottomBarContentPadding(
    navigationBarsBottom: Dp,
    reserveBottomBar: Boolean,
    isBottomBarFloating: Boolean,
    spec: BottomBarContentPaddingSpec,
    extraContentPadding: Dp,
): Dp {
    val safeNavigationBarsBottom = navigationBarsBottom.coerceAtLeast(0.dp)
    val safeExtraContentPadding = extraContentPadding.coerceAtLeast(0.dp)
    if (!reserveBottomBar) {
        return safeNavigationBarsBottom + safeExtraContentPadding
    }

    val barExtent = if (isBottomBarFloating) {
        spec.floatingBodyHeight + spec.floatingInset
    } else {
        spec.dockedBodyHeight
    }
    return safeNavigationBarsBottom + barExtent + spec.contentGap + safeExtraContentPadding
}
