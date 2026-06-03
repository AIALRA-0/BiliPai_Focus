package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.plugin.CastPluginPlaybackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VideoPlayerOverlayCastPolicyTest {

    // --- cast dialog dismissal timing ---

    @Test
    fun shouldDismissCastDialogImmediatelyOnUrlFailure() {
        assertTrue(shouldDismissCastDialogOnUrlFailure(null))
    }

    @Test
    fun shouldDismissCastDialogImmediatelyOnBlankUrl() {
        assertTrue(shouldDismissCastDialogOnUrlFailure(""))
        assertTrue(shouldDismissCastDialogOnUrlFailure("   "))
    }

    @Test
    fun shouldNotDismissCastDialogWhenUrlResolved() {
        assertFalse(shouldDismissCastDialogOnUrlFailure("https://example.com/video.mp4"))
        assertFalse(shouldDismissCastDialogOnUrlFailure("http://127.0.0.1:8901/proxy"))
    }

    // --- effective progress resolution with plugin state ---

    @Test
    fun effectiveProgressUsesLocalWhenPluginIsNull() {
        val local = PlayerProgress(current = 5000L, duration = 30000L, buffered = 10000L)
        val result = resolveEffectivePlayerProgress(local, null)
        assertEquals(local, result)
    }

    @Test
    fun effectiveProgressUsesLocalWhenPluginInactive() {
        val local = PlayerProgress(current = 5000L, duration = 30000L, buffered = 10000L)
        val inactive = CastPluginPlaybackState(isActive = false)
        val result = resolveEffectivePlayerProgress(local, inactive)
        assertEquals(local, result)
    }

    @Test
    fun effectiveProgressUsesPluginWhenActive() {
        val local = PlayerProgress(current = 5000L, duration = 30000L, buffered = 10000L)
        val active = CastPluginPlaybackState(
            isActive = true,
            currentPositionMs = 15000L,
            durationMs = 60000L,
            bufferedPositionMs = 20000L
        )
        val result = resolveEffectivePlayerProgress(local, active)
        assertEquals(15000L, result.current)
        assertEquals(60000L, result.duration)
        assertEquals(20000L, result.buffered)
    }

    // --- effective playing state resolution ---

    @Test
    fun effectivePlayingUsesLocalWhenPluginNull() {
        assertTrue(resolveEffectivePlayingState(localIsPlaying = true, pluginState = null))
        assertFalse(resolveEffectivePlayingState(localIsPlaying = false, pluginState = null))
    }

    @Test
    fun effectivePlayingUsesLocalWhenPluginInactive() {
        val inactive = CastPluginPlaybackState(isActive = false, isPlaying = true)
        assertTrue(resolveEffectivePlayingState(localIsPlaying = true, pluginState = inactive))
        assertFalse(resolveEffectivePlayingState(localIsPlaying = false, pluginState = inactive))
    }

    @Test
    fun effectivePlayingUsesPluginWhenActive() {
        val active = CastPluginPlaybackState(isActive = true, isPlaying = false)
        assertFalse(resolveEffectivePlayingState(localIsPlaying = true, pluginState = active))
    }

    // --- shouldActivatePluginPlaybackAfterCast ---

    @Test
    fun activatesPluginPlaybackWhenCastPlaybackIsActive() {
        val state = CastPluginPlaybackState(isActive = true)
        assertTrue(shouldActivatePluginPlaybackAfterCast(state))
    }

    @Test
    fun doesNotActivatePluginPlaybackWhenCastPlaybackIsInactive() {
        val state = CastPluginPlaybackState(isActive = false)
        assertFalse(shouldActivatePluginPlaybackAfterCast(state))
    }

    // --- CastMediaSourceSignature ---

    @Test
    fun `CastMediaSourceSignature changes when quality changes`() {
        val sig1 = buildCastMediaSourceSignature(
            currentAid = 1L,
            cid = 2L,
            currentQuality = 80,
            currentVideoUrl = "https://example.com/video.mp4"
        )
        val sig2 = buildCastMediaSourceSignature(
            currentAid = 1L,
            cid = 2L,
            currentQuality = 64,
            currentVideoUrl = "https://example.com/video.mp4"
        )
        assertNotEquals(sig1, sig2)
    }

    // --- shouldReloadActiveCastAfterMediaSourceChange ---

    @Test
    fun `shouldReloadActiveCast returns true when plugin route state and signature differ`() {
        val currentSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val lastSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 64, currentVideoUrl = "url"
        )
        val activeState = CastPluginPlaybackState(isActive = true)

        assertTrue(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = true,
                activeRouteExists = true,
                pluginState = activeState,
                currentSignature = currentSig,
                lastCastSignature = lastSig
            )
        )
    }

    @Test
    fun `shouldReloadActiveCast returns false for inactive playback state`() {
        val currentSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val lastSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 64, currentVideoUrl = "url"
        )
        val inactiveState = CastPluginPlaybackState(isActive = false)

        assertFalse(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = true,
                activeRouteExists = true,
                pluginState = inactiveState,
                currentSignature = currentSig,
                lastCastSignature = lastSig
            )
        )
    }

    @Test
    fun `shouldReloadActiveCast returns false when no plugin exists`() {
        val currentSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val lastSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 64, currentVideoUrl = "url"
        )
        val activeState = CastPluginPlaybackState(isActive = true)

        assertFalse(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = false,
                activeRouteExists = true,
                pluginState = activeState,
                currentSignature = currentSig,
                lastCastSignature = lastSig
            )
        )
    }

    @Test
    fun `shouldReloadActiveCast returns false when no route exists`() {
        val currentSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val lastSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 64, currentVideoUrl = "url"
        )
        val activeState = CastPluginPlaybackState(isActive = true)

        assertFalse(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = true,
                activeRouteExists = false,
                pluginState = activeState,
                currentSignature = currentSig,
                lastCastSignature = lastSig
            )
        )
    }

    @Test
    fun `shouldReloadActiveCast returns false for first cast with no previous signature`() {
        val currentSig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val activeState = CastPluginPlaybackState(isActive = true)

        assertFalse(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = true,
                activeRouteExists = true,
                pluginState = activeState,
                currentSignature = currentSig,
                lastCastSignature = null
            )
        )
    }

    // --- resolveCastDashDurationMs ---

    @Test
    fun `resolveCastDashDurationMs prefers timelength in ms over dash duration`() {
        assertEquals(120000L, resolveCastDashDurationMs(timelengthMs = 120000L, dashDurationSec = 120))
    }

    @Test
    fun `resolveCastDashDurationMs falls back to dash duration converted to ms`() {
        assertEquals(120000L, resolveCastDashDurationMs(timelengthMs = 0L, dashDurationSec = 120))
    }

    @Test
    fun `resolveCastDashDurationMs returns zero when both are unavailable`() {
        assertEquals(0L, resolveCastDashDurationMs(timelengthMs = 0L, dashDurationSec = 0))
    }

    @Test
    fun `resolveCastDashDurationMs returns zero when timelength is negative and dash is zero`() {
        assertEquals(0L, resolveCastDashDurationMs(timelengthMs = -1L, dashDurationSec = 0))
    }

    @Test
    fun `shouldReloadActiveCast returns false when signature unchanged`() {
        val sig = buildCastMediaSourceSignature(
            currentAid = 1L, cid = 2L, currentQuality = 80, currentVideoUrl = "url"
        )
        val activeState = CastPluginPlaybackState(isActive = true)

        assertFalse(
            shouldReloadActiveCastAfterMediaSourceChange(
                activePluginExists = true,
                activeRouteExists = true,
                pluginState = activeState,
                currentSignature = sig,
                lastCastSignature = sig
            )
        )
    }
}
