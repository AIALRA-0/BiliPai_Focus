package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

internal data class ZoomableImageScaleLimits(
    val doubleTapScale: Float,
    val maxScale: Float
)

private const val DEFAULT_DOUBLE_TAP_SCALE = 2.5f
private const val DEFAULT_MAX_SCALE = 5f
private const val EXTRA_DETAIL_SCALE = 2f
private const val EXTREME_ASPECT_RATIO = 3f

/** Untransformed viewport coordinates; parent Hero layers must never feed back into this rect. */
internal fun resolveZoomableImageLocalDisplayRect(
    imageSize: IntSize,
    containerSize: IntSize,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
): Rect? {
    if (imageSize.width <= 0 || imageSize.height <= 0 ||
        containerSize.width <= 0 || containerSize.height <= 0
    ) return null
    val fitScale = min(
        containerSize.width.toFloat() / imageSize.width,
        containerSize.height.toFloat() / imageSize.height,
    )
    val width = imageSize.width * fitScale * scale
    val height = imageSize.height * fitScale * scale
    val centerX = containerSize.width / 2f + offsetX
    val centerY = containerSize.height / 2f + offsetY
    return Rect(centerX - width / 2f, centerY - height / 2f, centerX + width / 2f, centerY + height / 2f)
}

/**
 * The image starts with ContentScale.Fit. For a very tall or wide image that can make the
 * narrow edge only a few pixels wide, so a fixed 5x zoom can still be smaller than the viewport.
 */
internal fun resolveZoomableImageScaleLimits(
    imageWidth: Int,
    imageHeight: Int,
    containerWidth: Int,
    containerHeight: Int
): ZoomableImageScaleLimits {
    if (imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0 || containerHeight <= 0) {
        return ZoomableImageScaleLimits(
            doubleTapScale = DEFAULT_DOUBLE_TAP_SCALE,
            maxScale = DEFAULT_MAX_SCALE
        )
    }
    if (!isExtremeAspectRatio(imageWidth, imageHeight)) {
        return ZoomableImageScaleLimits(
            doubleTapScale = DEFAULT_DOUBLE_TAP_SCALE,
            maxScale = DEFAULT_MAX_SCALE
        )
    }

    val fitScale = min(
        containerWidth.toFloat() / imageWidth,
        containerHeight.toFloat() / imageHeight
    )
    val fillViewportScale = max(
        containerWidth / (imageWidth * fitScale),
        containerHeight / (imageHeight * fitScale)
    )

    return ZoomableImageScaleLimits(
        doubleTapScale = max(DEFAULT_DOUBLE_TAP_SCALE, fillViewportScale),
        maxScale = max(DEFAULT_MAX_SCALE, fillViewportScale * EXTRA_DETAIL_SCALE)
    )
}

internal fun isExtremeAspectRatio(imageWidth: Int, imageHeight: Int): Boolean {
    if (imageWidth <= 0 || imageHeight <= 0) return false
    val ratio = max(
        imageWidth.toFloat() / imageHeight,
        imageHeight.toFloat() / imageWidth
    )
    return ratio > EXTREME_ASPECT_RATIO
}
