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

    @Test
    fun videoWindowModesTakePriorityOverAudioBar() {
        for (mode in com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.entries) {
            val activate = shouldActivateAudioBarOnVideoExit(
                barEnabled = true,
                hasVideoIdentity = true,
                isLive = false,
                isMiniOrPip = false,
                isNavigatingToVideo = false,
                inAppMiniPlayerEnabled = mode.supportsInAppMiniPlayer,
            )
            kotlin.test.assertEquals(!mode.supportsInAppMiniPlayer, activate)
        }
    }

    @Test
    fun stopOnExitStillPublishesThePausedResumeEntry() {
        assertTrue(
            shouldActivateAudioBarOnVideoExit(
                barEnabled = true,
                hasVideoIdentity = true,
                isLive = false,
                isMiniOrPip = false,
                isNavigatingToVideo = false,
                stopPlaybackOnExit = true,
            )
        )
    }

    @Test
    fun pausedResumeEntryDoesNotOverrideStopOnExitCleanup() {
        assertTrue(shouldActivateAudioBarOnVideoExit(
            barEnabled = true, hasVideoIdentity = true, isLive = false,
            isMiniOrPip = false, isNavigatingToVideo = false,
            stopPlaybackOnExit = true, inAppMiniPlayerEnabled = true,
        ))
        assertTrue(shouldClearPlaybackNotificationOnNavigationExit(
            mode = com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.OFF,
            stopPlaybackOnExit = true,
            keepForAudioNowPlaying = true,
        ))
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
