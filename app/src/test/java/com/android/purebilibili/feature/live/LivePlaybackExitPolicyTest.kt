package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePlaybackExitPolicyTest {

    @Test
    fun `live playback stops when route is disposed by navigation`() {
        assertTrue(shouldStopLivePlaybackOnRouteDispose(isChangingConfigurations = false))
    }

    @Test
    fun `live playback is preserved across configuration changes`() {
        assertFalse(shouldStopLivePlaybackOnRouteDispose(isChangingConfigurations = true))
    }
}
