package com.android.purebilibili.feature.dynamic.components

internal enum class DrawGridScaleMode {
    FIT,
    CROP
}

/** 列表/转发预览与 B 站一致：最多九宫格；详情页传 null 不截断。 */
internal const val DYNAMIC_FEED_PREVIEW_MAX_IMAGES = 9

private const val PILI_PLUS_DYNAMIC_MAX_IMAGE_RATIO = 22f / 9f
private const val PILI_PLUS_SINGLE_IMAGE_WIDE_THRESHOLD = 1.5f
private const val PILI_PLUS_DYNAMIC_LONG_IMAGE_THRESHOLD = 2.2f

// 单图长图判定（对齐 BiliPai image_grid_view 的长图徽标）：
// 高宽比超过阈值且宽度足够大时显示"长图"徽标。
internal fun shouldShowDrawGridLongImageBadge(
    width: Int,
    height: Int
): Boolean {
    if (width <= 0 || height <= 0) return false
    if (width < 100) return false
    return height.toFloat() / width.toFloat() > PILI_PLUS_DYNAMIC_LONG_IMAGE_THRESHOLD
}

internal fun resolveSingleImageAspectRatio(
    width: Int,
    height: Int
): Float {
    if (width <= 0 || height <= 0) return 1f
    return (width.toFloat() / height.toFloat()).coerceIn(
        1f / PILI_PLUS_DYNAMIC_MAX_IMAGE_RATIO,
        PILI_PLUS_DYNAMIC_MAX_IMAGE_RATIO
    )
}

internal fun resolveSingleImageWidthFraction(
    width: Int,
    height: Int
): Float {
    if (width <= 0 || height <= 0) return 2f / 3f

    val ratioWh = width.toFloat() / height.toFloat()
    val ratioHw = height.toFloat() / width.toFloat()
    return when {
        ratioWh > PILI_PLUS_SINGLE_IMAGE_WIDE_THRESHOLD -> 1f
        ratioWh >= 1f || (height > width && ratioHw < PILI_PLUS_SINGLE_IMAGE_WIDE_THRESHOLD) -> 2f / 3f
        else -> 0.5f
    }
}

internal fun resolveDrawGridScaleMode(totalImages: Int): DrawGridScaleMode {
    return if (totalImages == 1) DrawGridScaleMode.FIT else DrawGridScaleMode.CROP
}

internal fun resolveDrawGridDisplayCount(
    totalImages: Int,
    maxDisplayImages: Int?
): Int {
    if (totalImages <= 0) return 0
    val maxImages = maxDisplayImages ?: return totalImages
    return totalImages.coerceAtMost(maxImages.coerceAtLeast(1))
}

internal fun resolveDrawGridColumnCount(displayCount: Int): Int {
    return when {
        displayCount <= 1 -> 1
        displayCount <= 4 -> 2
        else -> 3
    }
}

internal fun shouldDrawGridShowMoreBadge(
    index: Int,
    displayCount: Int,
    totalCount: Int
): Boolean = index == displayCount - 1 && totalCount > displayCount

internal fun resolveDrawGridSpacingDp(): Int = 5

internal fun resolveDrawGridCornerRadiusDp(): Int = 10
