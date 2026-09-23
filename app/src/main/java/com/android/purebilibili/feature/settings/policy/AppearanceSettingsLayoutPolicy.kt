package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val APPEARANCE_EXTRA_BOTTOM_PADDING_EXPANDABLE = 96.dp
private val APPEARANCE_EXTRA_BOTTOM_PADDING_COMPACT = 24.dp

internal fun resolveAppearanceBottomPadding(
    navigationBarsBottom: Dp,
    expandableSectionEnabled: Boolean
): Dp {
    val extraPadding = if (expandableSectionEnabled) {
        APPEARANCE_EXTRA_BOTTOM_PADDING_EXPANDABLE
    } else {
        APPEARANCE_EXTRA_BOTTOM_PADDING_COMPACT
    }
    return navigationBarsBottom + extraPadding
}
