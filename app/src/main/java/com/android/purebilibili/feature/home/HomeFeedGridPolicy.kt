package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardWidthPreset

internal fun resolveHomeFeedGridColumns(
    contentWidthDp: Int,
    displayMode: Int,
    fixedColumnCount: Int,
    cardWidthPreset: HomeFeedCardWidthPreset
): Int {
    val isSingleColumnMode = displayMode == 1
    if (!isSingleColumnMode && fixedColumnCount > 0) {
        return fixedColumnCount
    }

    val minColumnWidthDp = if (isSingleColumnMode) {
        280
    } else {
        cardWidthPreset.minCardWidthDp ?: 180
    }
    val maxColumns = if (isSingleColumnMode) 2 else 6
    val columns = contentWidthDp / minColumnWidthDp
    val minColumns = if (!isSingleColumnMode && contentWidthDp >= 300) 2 else 1
    return columns.coerceIn(minColumns, maxColumns)
}
