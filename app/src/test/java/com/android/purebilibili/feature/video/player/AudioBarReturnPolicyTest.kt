package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioBarReturnPolicyTest {
    @Test
    fun videoReturnActivatesAudioBarWithoutPlaybackState() {
        assertTrue(
            shouldActivateAudioBarOnVideoExit(
                barEnabled = true,
                hasVideoIdentity = true,
                isLive = false,
                isMiniOrPip = false,
                isNavigatingToVideo = false,
            )
        )
    }

    @Test
    fun nonVideoDestinationsDoNotActivateAudioBar() {
        assertFalse(
            shouldActivateAudioBarOnVideoExit(
                barEnabled = false,
                hasVideoIdentity = true,
                isLive = false,
                isMiniOrPip = false,
                isNavigatingToVideo = false,
            )
        )
        assertFalse(eligible(isLive = true))
        assertFalse(eligible(isMiniOrPip = true))
        assertFalse(eligible(isNavigatingToVideo = true))
    }

    private fun eligible(
        isLive: Boolean = false,
        isMiniOrPip: Boolean = false,
        isNavigatingToVideo: Boolean = false,
    ) = shouldActivateAudioBarOnVideoExit(
        barEnabled = true,
        hasVideoIdentity = true,
        isLive = isLive,
        isMiniOrPip = isMiniOrPip,
        isNavigatingToVideo = isNavigatingToVideo,
    )
}
