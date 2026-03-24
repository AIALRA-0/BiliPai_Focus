package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding

internal fun shouldConsumeSettingsBack(showBlockedList: Boolean): Boolean = showBlockedList

internal fun resolveSettingsBottomBarReservedPadding(
    bottomBarVisible: Boolean,
    isBottomBarFloating: Boolean,
    bottomBarLabelMode: Int,
    isTablet: Boolean
): Dp {
    if (!bottomBarVisible) return 0.dp

    val floatingBodyHeight = when (bottomBarLabelMode) {
        0 -> if (isTablet) 76.dp else 70.dp
        2 -> if (isTablet) 56.dp else 54.dp
        else -> if (isTablet) 68.dp else 62.dp
    }
    val dockedBodyHeight = when (bottomBarLabelMode) {
        0 -> 72.dp
        2 -> if (isTablet) 52.dp else 56.dp
        else -> 64.dp
    }
    val floatingInset = if (isBottomBarFloating) {
        if (isTablet) 20.dp else 16.dp
    } else {
        0.dp
    }

    return if (isBottomBarFloating) {
        floatingBodyHeight + floatingInset + 12.dp
    } else {
        dockedBodyHeight + 12.dp
    }
}

internal fun resolveSettingsContentBottomPadding(
    navigationBarsBottom: Dp,
    bottomBarVisible: Boolean,
    isBottomBarFloating: Boolean,
    bottomBarLabelMode: Int,
    isTablet: Boolean,
    extraBottomPadding: Dp = 28.dp
): Dp {
    return resolveBottomSafeAreaPadding(
        navigationBarsBottom = navigationBarsBottom,
        extraBottomPadding = extraBottomPadding + resolveSettingsBottomBarReservedPadding(
            bottomBarVisible = bottomBarVisible,
            isBottomBarFloating = isBottomBarFloating,
            bottomBarLabelMode = bottomBarLabelMode,
            isTablet = isTablet
        )
    )
}
