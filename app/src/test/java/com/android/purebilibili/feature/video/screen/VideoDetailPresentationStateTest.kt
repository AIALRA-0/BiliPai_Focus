package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailPresentationStateTest {

    @Test
    fun switchingVideoUpdatesTargetAndResetsSelectedTab() {
        val state = VideoDetailPresentationState.create(
            initialBvid = "BV1",
            initialCid = 11L,
            initialPortraitFullscreen = false,
            initialPipMode = false,
        )

        state.selectTab(1)
        state.switchVideo("BV2", 22L)

        assertEquals("BV2", state.currentBvidState.value)
        assertEquals(22L, state.currentCidState.longValue)
        assertEquals(0, state.selectedTabIndexState.intValue)
    }

    @Test
    fun syncingPlaybackIdentityKeepsSelectedTab() {
        val state = VideoDetailPresentationState.create(
            initialBvid = "BV1",
            initialCid = 11L,
            initialPortraitFullscreen = false,
            initialPipMode = false,
        )

        state.selectTab(1)
        state.syncPlaybackIdentity("BV1", 22L)

        assertEquals("BV1", state.currentBvidState.value)
        assertEquals(22L, state.currentCidState.longValue)
        assertEquals(1, state.selectedTabIndexState.intValue)
    }

    @Test
    fun automaticPageCommitsPreventReloadingThePreviouslySelectedPart() {
        val state = VideoDetailPresentationState.create(
            initialBvid = "BV1",
            initialCid = 11L,
            initialPortraitFullscreen = false,
            initialPipMode = false,
        )
        state.selectTab(1)
        // Manually select P2, then let playback advance through P3 and P4.
        state.syncPlaybackIdentity("BV1", 22L)
        for (nextCid in listOf(33L, 44L)) {
            // Without the automatic commit notification the guard reloads the old part.
            assertTrue(
                shouldSyncMainPlayerToInternalBvid(
                    isPortraitFullscreen = false,
                    routeBvid = "BV1",
                    currentBvid = state.currentBvidState.value,
                    currentBvidCid = state.currentCidState.longValue,
                    loadedBvid = "BV1",
                    loadedCid = nextCid,
                )
            )
            state.syncPlaybackIdentity("BV1", nextCid)

            assertFalse(
                shouldSyncMainPlayerToInternalBvid(
                    isPortraitFullscreen = false,
                    routeBvid = "BV1",
                    currentBvid = state.currentBvidState.value,
                    currentBvidCid = state.currentCidState.longValue,
                    loadedBvid = "BV1",
                    loadedCid = nextCid,
                )
            )
            assertEquals(nextCid, state.currentCidState.longValue)
            assertEquals(1, state.selectedTabIndexState.intValue)
        }
    }

    @Test
    fun namedSessionOperationsOwnTransientPresentationFlags() {
        val state = VideoDetailPresentationState.create(
            initialBvid = "BV1",
            initialCid = 0L,
            initialPortraitFullscreen = false,
            initialPipMode = false,
        )

        state.setPortraitFullscreen(true)
        state.syncPipMode(true)
        state.markNavigatingToVideo()
        state.markNavigatingToAudioMode()
        state.markNavigatingToMiniMode()

        assertTrue(state.portraitFullscreenState.value)
        assertTrue(state.pipModeState.value)
        assertTrue(state.navigatingToVideoState.value)
        assertTrue(state.navigatingToAudioModeState.value)
        assertTrue(state.navigatingToMiniModeState.value)
    }

}
