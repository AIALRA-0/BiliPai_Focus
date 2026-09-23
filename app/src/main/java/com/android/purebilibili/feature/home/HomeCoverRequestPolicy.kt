package com.android.purebilibili.feature.home

import com.android.purebilibili.core.util.FormatUtils
import kotlin.math.ceil

internal data class HomeCoverRequestSpec(
    val widthPx: Int,
    val heightPx: Int,
) {
    val cacheKeySuffix: String = "${widthPx}x$heightPx"

    fun resolveUrl(url: String?): String =
        FormatUtils.buildSizedImageUrl(url, width = widthPx, height = heightPx)
}

internal fun resolveHomeCoverRequestSpec(
    cardWidthDp: Float,
    density: Float,
    useLowQualityCover: Boolean,
): HomeCoverRequestSpec {
    if (useLowQualityCover) {
        return HomeCoverRequestSpec(widthPx = 240, heightPx = 150)
    }

    val sampledWidthPx = ceil(cardWidthDp.coerceAtLeast(0f) * density.coerceAtLeast(0f) * 1.25f)
        .toInt()
    val widthPx = HOME_COVER_WIDTH_TIERS.firstOrNull { it >= sampledWidthPx }
        ?: HOME_COVER_WIDTH_TIERS.last()
    return HomeCoverRequestSpec(
        widthPx = widthPx,
        heightPx = widthPx * 10 / 16,
    )
}

private val HOME_COVER_WIDTH_TIERS = intArrayOf(480, 640, 960, 1280)
