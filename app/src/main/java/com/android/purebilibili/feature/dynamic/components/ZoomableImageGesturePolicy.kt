package com.android.purebilibili.feature.dynamic.components

import kotlin.math.abs

internal enum class ZoomableImageGestureMode {
    UNDECIDED,
    HORIZONTAL_PAGER,
    IMAGE_INTERACTION,
    VERTICAL_DISMISS
}

/**
 * 单指、未缩放时优先按轴锁定：竖直 → 下滑退出，水平 → 交给 Pager。
 * 不把微小 pinch 噪声当成缩放，避免竖滑被吞掉。
 */
internal fun resolveZoomableImageGestureMode(
    isMultiTouch: Boolean,
    scale: Float,
    panX: Float,
    panY: Float,
    verticalBias: Float = 1.08f
): ZoomableImageGestureMode {
    if (isMultiTouch || !shouldEnableImagePreviewVerticalDismiss(scale)) {
        return ZoomableImageGestureMode.IMAGE_INTERACTION
    }
    val absX = abs(panX)
    val absY = abs(panY)
    return when {
        absY > absX * verticalBias -> ZoomableImageGestureMode.VERTICAL_DISMISS
        absX > absY * verticalBias -> ZoomableImageGestureMode.HORIZONTAL_PAGER
        else -> ZoomableImageGestureMode.IMAGE_INTERACTION
    }
}
