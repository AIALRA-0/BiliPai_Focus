package com.android.purebilibili.feature.live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveRoomLayoutPolicyTest {

    @Test
    fun `portrait vertical live uses overlay layout like PiliPlus portrait room`() {
        val mode = resolveLiveRoomLayoutMode(
            isLandscape = false,
            isTablet = false,
            isFullscreen = false,
            isPortraitLive = true
        )

        assertEquals(LiveRoomLayoutMode.PortraitVerticalOverlay, mode)
    }

    @Test
    fun `landscape tablet outside fullscreen keeps split chat panel`() {
        val mode = resolveLiveRoomLayoutMode(
            isLandscape = true,
            isTablet = true,
            isFullscreen = false,
            isPortraitLive = false
        )

        assertEquals(LiveRoomLayoutMode.LandscapeSplit, mode)
    }

    @Test
    fun `fullscreen landscape uses transparent overlay chat`() {
        val mode = resolveLiveRoomLayoutMode(
            isLandscape = true,
            isTablet = true,
            isFullscreen = true,
            isPortraitLive = false
        )

        assertEquals(LiveRoomLayoutMode.LandscapeOverlay, mode)
    }

    @Test
    fun `landscape modes expose chat toggle`() {
        assertTrue(shouldShowLiveChatToggle(LiveRoomLayoutMode.LandscapeSplit))
        assertTrue(shouldShowLiveChatToggle(LiveRoomLayoutMode.LandscapeOverlay))
        assertFalse(shouldShowLiveChatToggle(LiveRoomLayoutMode.PortraitPanel))
    }

    @Test
    fun `split mode and overlay mode use different chat containers`() {
        assertTrue(
            shouldShowLiveSplitChatPanel(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isChatVisible = true
            )
        )
        assertFalse(
            shouldShowLiveSplitChatPanel(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isChatVisible = true
            )
        )
        assertTrue(
            shouldShowLiveLandscapeChatOverlay(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isChatVisible = true
            )
        )
        assertFalse(
            shouldShowLiveLandscapeChatOverlay(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isChatVisible = true
            )
        )
    }

    @Test
    fun `duration format matches live room compact labels`() {
        val startedAt = 1_700_000_000L
        val now = startedAt * 1000L + 90L * 60_000L

        assertEquals("开播1小时30分钟", formatLiveDuration(startedAt, now))
    }

    @Test
    fun `viewer count uses compact chinese units`() {
        assertEquals("1.2万", formatLiveViewerCount(12_300))
        assertEquals("-", formatLiveViewerCount(0))
    }
}
