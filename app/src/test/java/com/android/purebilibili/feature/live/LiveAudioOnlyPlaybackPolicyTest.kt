package com.android.purebilibili.feature.live

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveAudioOnlyPlaybackPolicyTest {

    @Test
    fun `container navigation clips live texture without requiring legacy shared scopes`() {
        assertTrue(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = false,
                preferCompatibleTextureSurface = false,
                hasContainerTransition = true,
            )
        )
        assertFalse(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = false,
                preferCompatibleTextureSurface = false,
                hasContainerTransition = true,
                requiresHdrSurfaceOutput = true,
            )
        )
    }

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
                hasAnimatedVisibilityScope = true,
                preferCompatibleTextureSurface = false,
            )
        )
        assertFalse(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true,
                preferCompatibleTextureSurface = false,
            )
        )
        assertFalse(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = false,
                preferCompatibleTextureSurface = false,
            )
        )
    }

    @Test
    fun `compatible live playback uses texture without a transition but retains hdr output`() {
        assertTrue(
            shouldUseTextureSurfaceForLivePlayer(
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = false,
                preferCompatibleTextureSurface = true,
            )
        )
        for (compatible in listOf(false, true)) {
            assertFalse(
                shouldUseTextureSurfaceForLivePlayer(
                    hasSharedTransitionScope = true,
                    hasAnimatedVisibilityScope = true,
                    requiresHdrSurfaceOutput = true,
                    preferCompatibleTextureSurface = compatible,
                )
            )
        }
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
