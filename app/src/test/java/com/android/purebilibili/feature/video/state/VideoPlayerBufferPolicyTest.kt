package com.android.purebilibili.feature.video.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerBufferPolicyTest {

    @Test
    fun wifiPolicyShouldUseLowerStartupBufferForFasterAutoplay() {
        val policy = resolvePlayerBufferPolicy(isOnWifi = true)

        assertEquals(10000, policy.minBufferMs)
        assertEquals(40000, policy.maxBufferMs)
        assertEquals(700, policy.bufferForPlaybackMs)
        assertEquals(1400, policy.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun mobilePolicyShouldUseFasterStartupWhileKeepingRebufferSafetyMargin() {
        val policy = resolvePlayerBufferPolicy(isOnWifi = false)

        assertEquals(12000, policy.minBufferMs)
        assertEquals(45000, policy.maxBufferMs)
        assertEquals(1000, policy.bufferForPlaybackMs)
        assertEquals(2200, policy.bufferForPlaybackAfterRebufferMs)
        assertTrue(policy.bufferForPlaybackAfterRebufferMs >= policy.bufferForPlaybackMs)
    }
}
