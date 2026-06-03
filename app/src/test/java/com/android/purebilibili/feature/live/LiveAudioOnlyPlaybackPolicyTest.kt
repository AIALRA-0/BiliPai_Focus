package com.android.purebilibili.feature.live

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveAudioOnlyPlaybackPolicyTest {

    @Test
    fun `audio only mode does not bind live player view`() {
        assertFalse(shouldBindLivePlayerViewForAudioOnly(isAudioOnly = true))
        assertTrue(shouldBindLivePlayerViewForAudioOnly(isAudioOnly = false))
    }

    @Test
    fun `audio only mode suppresses live danmaku overlay`() {
        assertFalse(
            shouldRenderLiveDanmakuOverlayForAudioOnly(
                isDanmakuEnabled = true,
                isAudioOnly = true
            )
        )
        assertFalse(
            shouldRenderLiveDanmakuOverlayForAudioOnly(
                isDanmakuEnabled = false,
                isAudioOnly = false
            )
        )
        assertTrue(
            shouldRenderLiveDanmakuOverlayForAudioOnly(
                isDanmakuEnabled = true,
                isAudioOnly = false
            )
        )
    }

    @Test
    fun `shared live player transition uses texture surface`() {
        assertTrue(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = false
            )
        )
    }

    @Test
    fun `audio only mode disables video track and video size`() {
        val current = TrackSelectionParameters.Builder().build()

        val result = resolveLiveTrackSelectionParametersForAudioOnly(
            currentTrackSelectionParameters = current,
            isAudioOnly = true
        )

        assertTrue(result.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO))
        assertEquals(0, result.maxVideoWidth)
        assertEquals(0, result.maxVideoHeight)
    }

    @Test
    fun `video mode keeps track selection untouched`() {
        val current = TrackSelectionParameters.Builder().build()

        val result = resolveLiveTrackSelectionParametersForAudioOnly(
            currentTrackSelectionParameters = current,
            isAudioOnly = false
        )

        assertEquals(current, result)
    }
}
