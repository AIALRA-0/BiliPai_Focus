package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerCoverPolicyTest {

    @Test
    fun verticalVideo_fillsPlayerViewportDuringCoverPhase() {
        assertTrue(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = false,
                forceCoverDuringReturnAnimation = false,
                isVerticalVideo = true
            )
        )
    }

    @Test
    fun returnCoverSharedBounds_doesNotForceViewportFill() {
        assertFalse(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = true,
                isVerticalVideo = true
            )
        )
    }
}
