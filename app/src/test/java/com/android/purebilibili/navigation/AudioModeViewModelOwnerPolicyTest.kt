package com.android.purebilibili.navigation

import androidx.lifecycle.Lifecycle
import com.android.purebilibili.navigation3.BiliPaiNavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioModeViewModelOwnerPolicyTest {

    @Test
    fun `shares previous entry when it is an active video route`() {
        assertTrue(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = "video/BV1active?cid=1",
                previousLifecycleState = Lifecycle.State.RESUMED
            )
        )
    }

    @Test
    fun `does not share previous entry after it is destroyed`() {
        assertFalse(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = "video/BV1destroyed?cid=1",
                previousLifecycleState = Lifecycle.State.DESTROYED
            )
        )
    }

    @Test
    fun `does not share previous entry when route is not video`() {
        assertFalse(
            shouldShareAudioModeViewModelWithPreviousEntry(
                previousRoute = ScreenRoutes.Settings.route,
                previousLifecycleState = Lifecycle.State.STARTED
            )
        )
    }

    @Test
    fun `audio mode returns to previous video when current bvid is unchanged`() {
        assertFalse(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1same",
                currentVideoBvid = "BV1same"
            )
        )
    }

    @Test
    fun `audio mode navigates to current video when queue advanced`() {
        assertTrue(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1first",
                currentVideoBvid = "BV2current"
            )
        )
    }

    @Test
    fun `audio mode ignores blank current bvid`() {
        assertFalse(
            shouldNavigateAudioModeBackToCurrentVideo(
                previousVideoBvid = "BV1first",
                currentVideoBvid = ""
            )
        )
    }

    @Test
    fun `audio mode requests initial load when sourced from a video and state is empty`() {
        assertEquals(
            AudioModeInitialLoadRequest(
                bvid = "BV1audio",
                cid = 123L,
                resumePositionMs = 4567L
            ),
            resolveAudioModeInitialLoadRequest(
                key = BiliPaiNavKey.AudioMode(
                    sourceBvid = "BV1audio",
                    sourceCid = 123L,
                    sourceResumePositionMs = 4567L
                ),
                hasDisplayState = false
            )
        )
    }

    @Test
    fun `audio mode skips initial load when state already exists`() {
        assertNull(
            resolveAudioModeInitialLoadRequest(
                key = BiliPaiNavKey.AudioMode(
                    sourceBvid = "BV1audio",
                    sourceCid = 123L
                ),
                hasDisplayState = true
            )
        )
    }

    @Test
    fun `audio mode skips initial load without a source video`() {
        assertNull(
            resolveAudioModeInitialLoadRequest(
                key = BiliPaiNavKey.AudioMode(),
                hasDisplayState = false
            )
        )
    }
}
