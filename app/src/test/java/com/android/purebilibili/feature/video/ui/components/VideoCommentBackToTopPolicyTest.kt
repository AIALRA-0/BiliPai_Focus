package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCommentBackToTopPolicyTest {

    @Test
    fun `back to top button stays hidden near comment list top`() {
        assertFalse(
            shouldShowVideoCommentBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
        assertFalse(
            shouldShowVideoCommentBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 240
            )
        )
    }

    @Test
    fun `back to top button appears after meaningful comment scroll`() {
        assertTrue(
            shouldShowVideoCommentBackToTop(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 720
            )
        )
        assertTrue(
            shouldShowVideoCommentBackToTop(
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }
}
