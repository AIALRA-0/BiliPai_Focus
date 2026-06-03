package com.android.purebilibili.feature.screenshot

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val DEFAULT_MIN_SELECTION_SIZE_PX = 24f

enum class AppScreenshotCaptureMode(val value: Int, val label: String, val description: String) {
    FULL_WINDOW(
        value = 0,
        label = "全屏",
        description = "直接保存当前 BiliPai 窗口"
    ),
    SELECT_REGION(
        value = 1,
        label = "手选区域",
        description = "先冻结预览，再拖拽选择保存区域"
    );

    companion object {
        fun fromValue(value: Int): AppScreenshotCaptureMode =
            entries.find { it.value == value } ?: FULL_WINDOW
    }
}

data class AppScreenshotPoint(
    val x: Float,
    val y: Float
)

data class AppScreenshotSelectionRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class AppScreenshotCropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

fun normalizeAppScreenshotSelectionRect(
    start: AppScreenshotPoint,
    end: AppScreenshotPoint,
    viewportWidth: Float,
    viewportHeight: Float
): AppScreenshotSelectionRect {
    val boundedLeft = min(start.x, end.x).coerceIn(0f, viewportWidth)
    val boundedRight = max(start.x, end.x).coerceIn(0f, viewportWidth)
    val boundedTop = min(start.y, end.y).coerceIn(0f, viewportHeight)
    val boundedBottom = max(start.y, end.y).coerceIn(0f, viewportHeight)
    return AppScreenshotSelectionRect(
        left = boundedLeft,
        top = boundedTop,
        right = boundedRight,
        bottom = boundedBottom
    )
}

fun isAppScreenshotSelectionLargeEnough(
    rect: AppScreenshotSelectionRect,
    minimumSizePx: Float = DEFAULT_MIN_SELECTION_SIZE_PX
): Boolean {
    return rect.width >= minimumSizePx && rect.height >= minimumSizePx
}

fun mapAppScreenshotSelectionRectToBitmap(
    rect: AppScreenshotSelectionRect,
    previewWidth: Float,
    previewHeight: Float,
    bitmapWidth: Int,
    bitmapHeight: Int,
    minimumPreviewSelectionSizePx: Float = DEFAULT_MIN_SELECTION_SIZE_PX
): AppScreenshotCropRect? {
    if (previewWidth <= 0f || previewHeight <= 0f || bitmapWidth <= 0 || bitmapHeight <= 0) {
        return null
    }
    if (!isAppScreenshotSelectionLargeEnough(rect, minimumPreviewSelectionSizePx)) {
        return null
    }

    val scaleX = bitmapWidth / previewWidth
    val scaleY = bitmapHeight / previewHeight
    val left = (rect.left * scaleX).roundToInt().coerceIn(0, bitmapWidth - 1)
    val top = (rect.top * scaleY).roundToInt().coerceIn(0, bitmapHeight - 1)
    val right = (rect.right * scaleX).roundToInt().coerceIn(left + 1, bitmapWidth)
    val bottom = (rect.bottom * scaleY).roundToInt().coerceIn(top + 1, bitmapHeight)
    val width = right - left
    val height = bottom - top
    if (width <= 1 || height <= 1) return null

    return AppScreenshotCropRect(
        left = left,
        top = top,
        width = width,
        height = height
    )
}
