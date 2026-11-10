package com.android.purebilibili.feature.video.ui.components

/** The narrow half-opened pane cannot leave enough room for title and stats beside a cover. */
internal const val RELATED_VIDEO_HORIZONTAL_MIN_WIDTH_DP = 320

internal enum class RelatedVideoCardLayout {
    HORIZONTAL,
    STACKED,
}

internal fun resolveRelatedVideoCardLayout(availableWidthDp: Int): RelatedVideoCardLayout =
    if (availableWidthDp < RELATED_VIDEO_HORIZONTAL_MIN_WIDTH_DP) {
        RelatedVideoCardLayout.STACKED
    } else {
        RelatedVideoCardLayout.HORIZONTAL
    }

internal fun resolveRelatedVideoListCardLayout(
    viewportWidthPx: Int,
    horizontalContentPaddingPx: Int,
    cardHorizontalPaddingPx: Int,
    density: Float,
): RelatedVideoCardLayout {
    val cardWidthPx = (viewportWidthPx - horizontalContentPaddingPx - 2 * cardHorizontalPaddingPx)
        .coerceAtLeast(0)
    return resolveRelatedVideoCardLayout((cardWidthPx / density).toInt())
}
