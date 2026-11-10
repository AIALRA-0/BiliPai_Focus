package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class CoinTossMotionTest {
    @Test
    fun characterAndBoxBounceBeforeTheCoinFades() {
        assertEquals(0f, resolveCoinTossFrame(0f, 1).characterLift)
        assertEquals(0.2f, resolveCoinTossFrame(50f, 1).characterLift)
        assertEquals(0f, resolveCoinTossFrame(100f, 1).characterLift)
        assertEquals(0.2f, resolveCoinTossFrame(150f, 1).boxLift)
        assertEquals(0f, resolveCoinTossFrame(200f, 1).boxLift)
        assertEquals(0f, resolveCoinTossFrame(100f, 1).coinLift)
        assertEquals(2f, resolveCoinTossFrame(300f, 1).coinLift)
        assertEquals(1f, resolveCoinTossFrame(300f, 1).coinAlpha)
        assertEquals(0.5f, resolveCoinTossFrame(350f, 1).coinAlpha)
        assertEquals(0f, resolveCoinTossFrame(400f, 1).coinAlpha)
    }

    @Test
    fun onlyTwoCoinsTriggerExactlyThreeThunderFrames() {
        assertEquals(-1, resolveCoinTossFrame(99f, 2).thunderIndex)
        assertEquals(0, resolveCoinTossFrame(100f, 2).thunderIndex)
        assertEquals(1, resolveCoinTossFrame(116f, 2).thunderIndex)
        assertEquals(2, resolveCoinTossFrame(132f, 2).thunderIndex)
        assertEquals(-1, resolveCoinTossFrame(148f, 2).thunderIndex)
        for (time in 0..400) assertEquals(-1, resolveCoinTossFrame(time.toFloat(), 1).thunderIndex)
    }
}
