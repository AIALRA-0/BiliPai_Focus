package com.android.purebilibili.core.ui.popup

/** 以实际测量的选项底边收紧视口；标题、多行说明和字体缩放都包含在边界中。 */
internal fun resolveWholeMenuViewportHeight(
    availableHeight: Int,
    itemBottoms: List<Int>,
    bottomPadding: Int,
): Int = itemBottoms.asSequence()
    .map { it + bottomPadding }
    .filter { it > 0 && it <= availableHeight }
    .maxOrNull() ?: availableHeight
