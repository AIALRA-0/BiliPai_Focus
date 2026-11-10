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

/** Interactive adjustment is used by image cropping; screenshot selection keeps its original gesture. */
internal enum class RegionDragTarget { NEW, MOVE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

internal fun resolveRegionDragTarget(
    rect: AppScreenshotSelectionRect?, point: AppScreenshotPoint, hitRadius: Float,
): RegionDragTarget {
    if (rect == null) return RegionDragTarget.NEW
    val x = point.x
    val y = point.y
    if (x < rect.left - hitRadius || x > rect.right + hitRadius || y < rect.top - hitRadius || y > rect.bottom + hitRadius) return RegionDragTarget.NEW
    val nearLeft = kotlin.math.abs(x - rect.left) <= hitRadius
    val nearRight = kotlin.math.abs(x - rect.right) <= hitRadius
    val nearTop = kotlin.math.abs(y - rect.top) <= hitRadius
    val nearBottom = kotlin.math.abs(y - rect.bottom) <= hitRadius
    val left = nearLeft && (!nearRight || kotlin.math.abs(x - rect.left) <= kotlin.math.abs(x - rect.right))
    val right = nearRight && !left
    val top = nearTop && (!nearBottom || kotlin.math.abs(y - rect.top) <= kotlin.math.abs(y - rect.bottom))
    val bottom = nearBottom && !top
    return when {
        left && top -> RegionDragTarget.TOP_LEFT
        right && top -> RegionDragTarget.TOP_RIGHT
        left && bottom -> RegionDragTarget.BOTTOM_LEFT
        right && bottom -> RegionDragTarget.BOTTOM_RIGHT
        left -> RegionDragTarget.LEFT
        right -> RegionDragTarget.RIGHT
        top -> RegionDragTarget.TOP
        bottom -> RegionDragTarget.BOTTOM
        x in rect.left..rect.right && y in rect.top..rect.bottom -> RegionDragTarget.MOVE
        else -> RegionDragTarget.NEW
    }
}

internal fun adjustRegionSelection(
    rect: AppScreenshotSelectionRect,
    target: RegionDragTarget,
    delta: AppScreenshotPoint,
    viewportWidth: Float,
    viewportHeight: Float,
    minimumSize: Float = 24f,
): AppScreenshotSelectionRect {
    if (viewportWidth <= 0f || viewportHeight <= 0f) return rect
    val minWidth = minimumSize.coerceIn(0f, viewportWidth)
    val minHeight = minimumSize.coerceIn(0f, viewportHeight)
    if (target == RegionDragTarget.MOVE) {
        val dx = delta.x.coerceIn(-rect.left, viewportWidth - rect.right)
        val dy = delta.y.coerceIn(-rect.top, viewportHeight - rect.bottom)
        return AppScreenshotSelectionRect(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
    }
    val left = if ((target == RegionDragTarget.LEFT || target == RegionDragTarget.TOP_LEFT || target == RegionDragTarget.BOTTOM_LEFT))
        (rect.left + delta.x).coerceIn(0f, (rect.right - minWidth).coerceAtLeast(0f)) else rect.left
    val right = if ((target == RegionDragTarget.RIGHT || target == RegionDragTarget.TOP_RIGHT || target == RegionDragTarget.BOTTOM_RIGHT))
        (rect.right + delta.x).coerceIn((rect.left + minWidth).coerceAtMost(viewportWidth), viewportWidth) else rect.right
    val top = if ((target == RegionDragTarget.TOP || target == RegionDragTarget.TOP_LEFT || target == RegionDragTarget.TOP_RIGHT))
        (rect.top + delta.y).coerceIn(0f, (rect.bottom - minHeight).coerceAtLeast(0f)) else rect.top
    val bottom = if ((target == RegionDragTarget.BOTTOM || target == RegionDragTarget.BOTTOM_LEFT || target == RegionDragTarget.BOTTOM_RIGHT))
        (rect.bottom + delta.y).coerceIn((rect.top + minHeight).coerceAtMost(viewportHeight), viewportHeight) else rect.bottom
    return AppScreenshotSelectionRect(left, top, right, bottom)
}
