package com.android.purebilibili.feature.video.ui.overlay

data class VideoProgressBarLayoutPolicy(
    val baseHeightWithoutChapterDp: Int,
    val baseHeightWithChapterDp: Int,
    val draggingContainerHeightDp: Int,
    val previewBottomPaddingDp: Int,
    val chapterBottomPaddingDp: Int,
    val chapterStartPaddingDp: Int,
    val chapterIconSizeDp: Int,
    val chapterSpacingDp: Int,
    val chapterFontSp: Int,
    val touchContainerHeightDp: Int,
    val trackHeightDp: Float,
    val thumbIdleSizeDp: Int,
    val thumbDraggingSizeDp: Int,
    val thumbIdleOffsetDp: Int,
    val thumbDraggingOffsetDp: Int
)

fun resolveVideoProgressBarLayoutPolicy(
    widthDp: Int
): VideoProgressBarLayoutPolicy {
    if (widthDp >= 1600) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 24,
            baseHeightWithChapterDp = 44,
            draggingContainerHeightDp = 140,
            previewBottomPaddingDp = 30,
            chapterBottomPaddingDp = 6,
            chapterStartPaddingDp = 20,
            chapterIconSizeDp = 16,
            chapterSpacingDp = 8,
            chapterFontSp = 14,
            touchContainerHeightDp = 20,
            trackHeightDp = 2f,
            thumbIdleSizeDp = 14,
            thumbDraggingSizeDp = 20,
            thumbIdleOffsetDp = 8,
            thumbDraggingOffsetDp = 10
        )
    }

    if (widthDp >= 840) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 18,
            baseHeightWithChapterDp = 36,
            draggingContainerHeightDp = 120,
            previewBottomPaddingDp = 26,
            chapterBottomPaddingDp = 5,
            chapterStartPaddingDp = 16,
            chapterIconSizeDp = 14,
            chapterSpacingDp = 6,
            chapterFontSp = 12,
            touchContainerHeightDp = 16,
            trackHeightDp = 2f,
            thumbIdleSizeDp = 12,
            thumbDraggingSizeDp = 16,
            thumbIdleOffsetDp = 7,
            thumbDraggingOffsetDp = 9
        )
    }

    if (widthDp >= 600) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 15,
            baseHeightWithChapterDp = 32,
            draggingContainerHeightDp = 110,
            previewBottomPaddingDp = 25,
            chapterBottomPaddingDp = 4,
            chapterStartPaddingDp = 14,
            chapterIconSizeDp = 13,
            chapterSpacingDp = 5,
            chapterFontSp = 11,
            touchContainerHeightDp = 14,
            trackHeightDp = 2f,
            thumbIdleSizeDp = 11,
            thumbDraggingSizeDp = 15,
            thumbIdleOffsetDp = 6,
            thumbDraggingOffsetDp = 8
        )
    }

    return VideoProgressBarLayoutPolicy(
        baseHeightWithoutChapterDp = 12,
        baseHeightWithChapterDp = 28,
        draggingContainerHeightDp = 100,
        previewBottomPaddingDp = 24,
        chapterBottomPaddingDp = 4,
        chapterStartPaddingDp = 12,
        chapterIconSizeDp = 12,
        chapterSpacingDp = 4,
        chapterFontSp = 10,
        touchContainerHeightDp = 12,
        trackHeightDp = 2f,
        thumbIdleSizeDp = 10,
        thumbDraggingSizeDp = 14,
        thumbIdleOffsetDp = 6,
        thumbDraggingOffsetDp = 8
    )
}

/** Reserve image height plus its gap; chapter chrome must not consume the image's space. */
internal fun resolveVideoProgressPreviewAreaHeightDp(
    layoutPolicy: VideoProgressBarLayoutPolicy,
    hasChapter: Boolean,
    previewImageHeightDp: Int?,
): Int {
    val baseHeight = if (hasChapter) layoutPolicy.baseHeightWithChapterDp
        else layoutPolicy.baseHeightWithoutChapterDp
    val existingHeight = (layoutPolicy.draggingContainerHeightDp - baseHeight).coerceAtLeast(52)
    val requiredHeight = previewImageHeightDp?.let { it + layoutPolicy.previewBottomPaddingDp } ?: 0
    return maxOf(existingHeight, requiredHeight)
}
