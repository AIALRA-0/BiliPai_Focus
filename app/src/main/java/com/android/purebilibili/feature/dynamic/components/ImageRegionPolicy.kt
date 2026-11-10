package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

internal data class ImageRegionRequest(val bounds: IntRect, val sampleSize: Int)

/** 只解码视口附近的原图区域；按网格合并拖动请求，每块最长边不超过 2048 像素。 */
internal fun resolveImageRegionRequest(
    imageSize: IntSize,
    viewport: IntSize,
    displayRect: Rect?,
): ImageRegionRequest? {
    if (displayRect == null || !displayRect.width.isFinite() || !displayRect.height.isFinite() ||
        !displayRect.left.isFinite() || !displayRect.top.isFinite() ||
        displayRect.width <= 0f || displayRect.height <= 0f ||
        imageSize.width <= 0 || imageSize.height <= 0 || viewport.width <= 0 || viewport.height <= 0
    ) return null
    val pixelsPerSourcePixel = displayRect.width / imageSize.width
    var sample = 1
    while (sample < (1 shl 28) && sample * 2 * pixelsPerSourcePixel <= 1f) sample *= 2
    val grid = sample * 128f
    fun low(value: Float, limit: Int) = (floor(value / grid) * grid).toInt().coerceIn(0, limit)
    fun high(value: Float, limit: Int) = (ceil(value / grid) * grid).toInt().coerceIn(0, limit)
    val left = low(-displayRect.left / pixelsPerSourcePixel, imageSize.width)
    val top = low(-displayRect.top / pixelsPerSourcePixel, imageSize.height)
    val right = high((viewport.width - displayRect.left) / pixelsPerSourcePixel, imageSize.width)
    val bottom = high((viewport.height - displayRect.top) / pixelsPerSourcePixel, imageSize.height)
    if (right <= left || bottom <= top) return null
    while (ceil(max(right - left, bottom - top).toDouble() / sample) > 2048) sample *= 2
    return ImageRegionRequest(IntRect(left, top, right, bottom), sample)
}
