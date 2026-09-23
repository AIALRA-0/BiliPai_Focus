package com.android.purebilibili.navigation3

import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiVideoBackCancellationPolicyTest {
    @Test
    fun onlyCancelledNestedVideoBackRecoversTheCurrentPlayer() {
        val currentVideo = BiliPaiNavKey.VideoDetail("BVchild")
        val parentVideo = BiliPaiNavKey.VideoDetail("BVparent")

        assertTrue(
            shouldRecoverVideoPlayerAfterBackCancellation(
                settleState = VideoCardTransitionSettleState.CancelRestore,
                currentKey = currentVideo,
                targetKey = parentVideo,
            ),
        )
        assertFalse(
            shouldRecoverVideoPlayerAfterBackCancellation(
                settleState = VideoCardTransitionSettleState.AutoReturn,
                currentKey = currentVideo,
                targetKey = parentVideo,
            ),
        )
        assertFalse(
            shouldRecoverVideoPlayerAfterBackCancellation(
                settleState = VideoCardTransitionSettleState.CancelRestore,
                currentKey = currentVideo,
                targetKey = BiliPaiNavKey.Home,
            ),
        )
    }
}
