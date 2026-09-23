package com.android.purebilibili.feature.video.screen

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoDetailPlayerCollapsePolicyTest {

    @Test
    fun `resolveNextPlayerHeightOffset ignores tiny deltas`() {
        val next = resolveNextPlayerHeightOffset(
            currentOffsetPx = -24f,
            deltaPx = 0.2f,
            minOffsetPx = -360f
        )

        assertNull(next)
    }

    @Test
    fun `resolveNextPlayerHeightOffset clamps into valid range`() {
        assertEquals(
            -360f,
            resolveNextPlayerHeightOffset(
                currentOffsetPx = -300f,
                deltaPx = -120f,
                minOffsetPx = -360f
            )
        )

        assertEquals(
            0f,
            resolveNextPlayerHeightOffset(
                currentOffsetPx = -10f,
                deltaPx = 30f,
                minOffsetPx = -360f
            )
        )
    }

    @Test
    fun `resolveNextPlayerHeightOffset returns null when clamped result unchanged`() {
        val next = resolveNextPlayerHeightOffset(
            currentOffsetPx = 0f,
            deltaPx = 32f,
            minOffsetPx = -360f
        )

        assertNull(next)
    }

    @Test
    fun `resolveIsPlayerCollapsed respects setting switch`() {
        assertFalse(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = false,
                playerHeightOffsetPx = -600f,
                videoHeightPx = 500f
            )
        )
    }

    @Test
    fun `resolveIsPlayerCollapsed enters collapsed state near lower bound`() {
        assertTrue(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = true,
                playerHeightOffsetPx = -495f,
                videoHeightPx = 500f,
                collapseTolerancePx = 10f
            )
        )

        assertFalse(
            resolveIsPlayerCollapsed(
                swipeHidePlayerEnabled = true,
                playerHeightOffsetPx = -460f,
                videoHeightPx = 500f,
                collapseTolerancePx = 10f
            )
        )
    }

    @Test
    fun `resolveIsPlaybackPausedForCollapse accepts explicit pause and ended playback`() {
        assertTrue(
            resolveIsPlaybackPausedForCollapse(
                playWhenReady = false,
                playbackState = Player.STATE_READY
            )
        )
        assertFalse(
            resolveIsPlaybackPausedForCollapse(
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING
            )
        )
        assertTrue(
            resolveIsPlaybackPausedForCollapse(
                playWhenReady = true,
                playbackState = Player.STATE_ENDED
            )
        )
        assertTrue(
            resolveIsPlaybackPausedForCollapse(
                playWhenReady = false,
                playbackState = Player.STATE_ENDED
            )
        )
    }

    @Test
    fun `collapsed player media darkens instead of receiving a light surface wash`() {
        assertEquals(0f, resolveCollapsedPlayerMediaScrimAlpha(0f), 0.0001f)
        assertEquals(0.21f, resolveCollapsedPlayerMediaScrimAlpha(0.5f), 0.0001f)
        assertEquals(0.42f, resolveCollapsedPlayerMediaScrimAlpha(1f), 0.0001f)
    }

    @Test
    fun `collapsed toolbar waits until late in collapse before covering media`() {
        assertEquals(0f, resolveCollapsedPlayerToolbarAlpha(0.55f), 0.0001f)
        assertEquals(1f, resolveCollapsedPlayerToolbarAlpha(1f), 0.0001f)
    }
}
