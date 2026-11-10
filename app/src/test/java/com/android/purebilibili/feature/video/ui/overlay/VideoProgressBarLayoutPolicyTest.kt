package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertTrue
import com.android.purebilibili.feature.video.ui.components.resolveSeekPreviewBubbleHeightDp

class VideoProgressBarLayoutPolicyTest {
    @Test
    fun draggingPreview_reservesFullImageHeightAndGapWithOrWithoutChapter() {
        listOf(420, 720, 1024, 1920).forEach { width ->
            val policy = resolveVideoProgressBarLayoutPolicy(width)
            val imageHeight = resolveSeekPreviewBubbleHeightDp(width)
            listOf(false, true).forEach { hasChapter ->
                val areaHeight = resolveVideoProgressPreviewAreaHeightDp(policy, hasChapter, imageHeight)
                assertTrue(areaHeight - policy.previewBottomPaddingDp >= imageHeight)
            }
        }
    }

    @Test
    fun idleLayout_keepsThumbAndChapterInsideProgressAreaAtWidthBoundaries() {
        listOf(393, 599, 600, 839, 840, 1599, 1600).forEach { width ->
            val policy = resolveVideoProgressBarLayoutPolicy(width)
            assertTrue(policy.touchContainerHeightDp >= policy.thumbIdleSizeDp)
            assertTrue(policy.baseHeightWithoutChapterDp >= policy.touchContainerHeightDp)
            val chapterHeight = maxOf(policy.chapterIconSizeDp, policy.chapterFontSp) +
                policy.chapterBottomPaddingDp
            assertTrue(policy.baseHeightWithChapterDp >= policy.touchContainerHeightDp + chapterHeight)
        }
    }
}
