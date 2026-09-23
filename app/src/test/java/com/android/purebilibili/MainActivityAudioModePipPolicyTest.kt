package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityAudioModePipPolicyTest {

    @Test
    fun `audio mode auto pip requires explicit opt-in`() {
        assertFalse(
            shouldTriggerPlaybackRoutePip(
                isInVideoDetail = false,
                isInAudioMode = true,
                isInMiniMode = false,
                audioModeAutoPipEnabled = false,
                shouldEnterPip = true,
                isActuallyPlaying = true
            )
        )
    }

    @Test
    fun `audio mode auto pip triggers when playback route is audio mode and feature enabled`() {
        assertTrue(
            shouldTriggerPlaybackRoutePip(
                isInVideoDetail = false,
                isInAudioMode = true,
                isInMiniMode = false,
                audioModeAutoPipEnabled = true,
                shouldEnterPip = true,
                isActuallyPlaying = true
            )
        )
    }

    @Test
    fun `video detail keeps existing pip behavior regardless of audio mode toggle`() {
        assertTrue(
            shouldTriggerPlaybackRoutePip(
                isInVideoDetail = true,
                isInAudioMode = false,
                isInMiniMode = false,
                audioModeAutoPipEnabled = false,
                shouldEnterPip = true,
                isActuallyPlaying = true
            )
        )
    }

    @Test
    fun `active mini player can transition to system pip`() {
        assertTrue(
            shouldTriggerPlaybackRoutePip(
                isInVideoDetail = false,
                isInAudioMode = false,
                isInMiniMode = true,
                audioModeAutoPipEnabled = false,
                shouldEnterPip = true,
                isActuallyPlaying = true
            )
        )
    }

    @Test
    fun `playback route becomes active for either video detail or audio mode`() {
        assertTrue(
            isPlaybackRouteActive(
                isInVideoDetail = true,
                isInAudioMode = false
            )
        )
        assertTrue(
            isPlaybackRouteActive(
                isInVideoDetail = false,
                isInAudioMode = true
            )
        )
        assertFalse(
            isPlaybackRouteActive(
                isInVideoDetail = false,
                isInAudioMode = false
            )
        )
    }
}
