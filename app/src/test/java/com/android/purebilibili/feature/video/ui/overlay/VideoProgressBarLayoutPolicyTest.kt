package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoProgressBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseProgressBarLayout() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 393
        )

        assertEquals(20, policy.baseHeightWithoutChapterDp)
        assertEquals(32, policy.baseHeightWithChapterDp)
        assertEquals(100, policy.draggingContainerHeightDp)
        assertEquals(2f, policy.trackHeightDp)
        assertEquals(10, policy.chapterFontSp)
        assertEquals(10, policy.thumbIdleSizeDp)
        assertEquals(14, policy.thumbDraggingSizeDp)
    }

    @Test
    fun mediumTablet_improvesSeekPrecisionAndHitArea() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 720
        )

        assertEquals(23, policy.baseHeightWithoutChapterDp)
        assertEquals(36, policy.baseHeightWithChapterDp)
        assertEquals(110, policy.draggingContainerHeightDp)
        assertEquals(2f, policy.trackHeightDp)
        assertEquals(11, policy.chapterFontSp)
        assertEquals(11, policy.thumbIdleSizeDp)
        assertEquals(15, policy.thumbDraggingSizeDp)
    }

    @Test
    fun tablet_expandsPreviewAndChapterReadability() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 1024
        )

        assertEquals(26, policy.baseHeightWithoutChapterDp)
        assertEquals(40, policy.baseHeightWithChapterDp)
        assertEquals(120, policy.draggingContainerHeightDp)
        assertEquals(2f, policy.trackHeightDp)
        assertEquals(12, policy.chapterFontSp)
        assertEquals(12, policy.thumbIdleSizeDp)
        assertEquals(16, policy.thumbDraggingSizeDp)
    }

    @Test
    fun ultraWide_usesLargestSeekTrackAndThumbs() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(32, policy.baseHeightWithoutChapterDp)
        assertEquals(48, policy.baseHeightWithChapterDp)
        assertEquals(140, policy.draggingContainerHeightDp)
        assertEquals(2f, policy.trackHeightDp)
        assertEquals(14, policy.chapterFontSp)
        assertEquals(14, policy.thumbIdleSizeDp)
        assertEquals(20, policy.thumbDraggingSizeDp)
    }
}
