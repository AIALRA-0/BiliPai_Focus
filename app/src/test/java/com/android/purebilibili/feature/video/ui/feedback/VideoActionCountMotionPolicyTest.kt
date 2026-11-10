package com.android.purebilibili.feature.video.ui.feedback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoActionCountMotionPolicyTest {
    @Test
    fun hydrationAndBackgroundRefresh_doNotAnimate() {
        assertFalse(shouldAnimateVideoActionCountChange("0", "120", 0, 0, false))
        assertFalse(shouldAnimateVideoActionCountChange("120", "121", 1, 1, false))
    }

    @Test
    fun localCountChange_canAnimateOnceIncludingZeroToOne() {
        assertTrue(shouldAnimateVideoActionCountChange("120", "121", 1, 0, false))
        assertTrue(shouldAnimateVideoActionCountChange("点赞", "1", 1, 0, false))
        assertTrue(shouldAnimateVideoActionCountChange("1", "点赞", 2, 1, false))
        assertFalse(shouldAnimateVideoActionCountChange("121", "122", 1, 1, false))
    }

    @Test
    fun formattedCountThatDidNotChange_andPlainLabelsStayStill() {
        assertFalse(shouldAnimateVideoActionCountChange("1.2万", "1.2万", 1, 0, false))
        assertFalse(shouldAnimateVideoActionCountChange("投币", "已投", 1, 0, false))
    }

    @Test
    fun reducedMotion_disablesCountMovement() {
        assertFalse(shouldAnimateVideoActionCountChange("120", "121", 1, 0, true))
    }

    @Test
    fun armingFavorite_doesNotArmLikeOrCoin() {
        val requests = VideoActionCountRequests()
        requests.requestFavorite()
        assertFalse(shouldAnimateVideoActionCountChange("10", "11", requests.like, 0, false))
        assertFalse(shouldAnimateVideoActionCountChange("10", "11", requests.coin, 0, false))
        assertTrue(shouldAnimateVideoActionCountChange("10", "11", requests.favorite, 0, false))
    }
}
