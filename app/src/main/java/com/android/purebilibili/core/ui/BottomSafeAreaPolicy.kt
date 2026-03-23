package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun resolveBottomSafeAreaPadding(
    navigationBarsBottom: Dp,
    extraBottomPadding: Dp = 0.dp
): Dp {
    val safeNavigationBarsBottom = navigationBarsBottom.coerceAtLeast(0.dp)
    val safeExtraBottomPadding = extraBottomPadding.coerceAtLeast(0.dp)
    return safeNavigationBarsBottom + safeExtraBottomPadding
}
