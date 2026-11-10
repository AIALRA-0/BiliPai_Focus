package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.engine.DanmakuItem

/** Only recent comments are eligible: never reveal a later scene's comments early. */
internal fun selectHotDanmaku(
    items: List<DanmakuItem>,
    positionMs: Long,
    maxItems: Int = 2,
): List<DanmakuItem> = items.asSequence()
    .filter { it.danmakuId > 0L && !it.text.isNullOrBlank() && it.likeCount >= 10L }
    .filter { it.showAtTime in (positionMs.coerceAtLeast(0L) - 15_000L).coerceAtLeast(0L)..positionMs.coerceAtLeast(0L) }
    .sortedWith(compareByDescending<DanmakuItem> { it.likeCount }.thenBy { it.danmakuId })
    .distinctBy { it.danmakuId }
    .distinctBy { it.text }
    .take(maxItems.coerceAtLeast(0))
    .toList()

/** Fit up to three complete entries, reserving an equal gap between entries and at both edges. */
internal fun selectFittingHotDanmaku(
    items: List<DanmakuItem>,
    availableWidthPx: Float,
    minimumGapPx: Float,
    measureWidthPx: (DanmakuItem) -> Float,
): List<DanmakuItem> {
    var first: DanmakuItem? = null
    var second: DanmakuItem? = null
    var third: DanmakuItem? = null
    var firstWidth = Float.POSITIVE_INFINITY
    var secondWidth = Float.POSITIVE_INFINITY
    var thirdWidth = Float.POSITIVE_INFINITY
    for (item in items) {
        val width = measureWidthPx(item)
        if (!width.isFinite() || width <= 0f || width > availableWidthPx) continue
        when {
            width < firstWidth -> {
                third = second
                thirdWidth = secondWidth
                second = first
                secondWidth = firstWidth
                first = item
                firstWidth = width
            }
            width < secondWidth -> {
                third = second
                thirdWidth = secondWidth
                second = item
                secondWidth = width
            }
            width < thirdWidth -> {
                third = item
                thirdWidth = width
            }
        }
    }
    val shortest = first ?: return emptyList()
    val next = second
    val last = third
    return when {
        next != null && last != null &&
            firstWidth + secondWidth + thirdWidth + 4 * minimumGapPx <= availableWidthPx ->
            listOf(shortest, next, last)
        next != null && firstWidth + secondWidth + 3 * minimumGapPx <= availableWidthPx ->
            listOf(shortest, next)
        else -> listOf(shortest)
    }
}
