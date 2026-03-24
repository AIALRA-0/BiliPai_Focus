package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailInternalBvidSyncPolicyTest {

    @Test
    fun nonPortraitRegularPlaybackSwitch_shouldNotForceReloadFromRouteBvid() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_ROUTE",
                currentBvidCid = 0L,
                loadedBvid = "BV_ROUTE",
                loadedCid = 0L
            )
        )
    }

    @Test
    fun portraitExitInternalTarget_shouldSyncMainPlayer() {
        assertTrue(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                currentBvidCid = 0L,
                loadedBvid = "BV_ROUTE",
                loadedCid = 0L
            )
        )
    }

    @Test
    fun alreadySyncedTarget_shouldNotReloadAgain() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                currentBvidCid = 0L,
                loadedBvid = "BV_TARGET",
                loadedCid = 0L
            )
        )
    }

    @Test
    fun portraitFullscreenActive_shouldNotSyncMainPlayer() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = true,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_TARGET",
                currentBvidCid = 0L,
                loadedBvid = "BV_ROUTE",
                loadedCid = 0L
            )
        )
    }

    @Test
    fun portraitExitSameBvidDifferentCid_shouldSyncMainPlayer() {
        assertTrue(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_TARGET",
                currentBvid = "BV_TARGET",
                currentBvidCid = 202L,
                loadedBvid = "BV_TARGET",
                loadedCid = 101L
            )
        )
    }

    @Test
    fun blankCurrentBvid_shouldNotSyncMainPlayer() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = " ",
                currentBvidCid = 202L,
                loadedBvid = "BV_TARGET",
                loadedCid = 101L
            )
        )
    }

    @Test
    fun inPlaceAutoAdvanceToAnotherBvid_shouldNotSyncBackToRouteVideo() {
        assertFalse(
            shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = false,
                routeBvid = "BV_ROUTE",
                currentBvid = "BV_ROUTE",
                currentBvidCid = 101L,
                loadedBvid = "BV_NEXT",
                loadedCid = 202L
            )
        )
    }

    @Test
    fun normalInternalSync_shouldRespectUserSettingByNotForcingAutoplay() {
        assertEquals(
            null,
            resolveAutoPlayOverrideForInternalBvidSync(forceAutoPlay = false)
        )
    }

    @Test
    fun explicitInternalSyncAutoplay_shouldStillForcePlayback() {
        assertEquals(
            true,
            resolveAutoPlayOverrideForInternalBvidSync(forceAutoPlay = true)
        )
    }
}
