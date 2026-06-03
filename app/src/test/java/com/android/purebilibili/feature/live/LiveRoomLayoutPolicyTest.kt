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
    fun `overlaying live layouts expose chat toggle`() {
        assertTrue(shouldShowLiveChatToggle(LiveRoomLayoutMode.PortraitVerticalOverlay))
        assertTrue(shouldShowLiveChatToggle(LiveRoomLayoutMode.LandscapeSplit))
        assertTrue(shouldShowLiveChatToggle(LiveRoomLayoutMode.LandscapeOverlay))
        assertFalse(shouldShowLiveChatToggle(LiveRoomLayoutMode.PortraitPanel))
    }

    @Test
    fun `live interaction panel defaults to collapsed`() {
        assertFalse(defaultLiveInteractionPanelVisible())
    }

    @Test
    fun `split mode and overlay mode use different chat containers`() {
        assertTrue(
            shouldShowLiveSplitChatPanel(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isInteractionPanelVisible = true
            )
        )
        assertFalse(
            shouldShowLiveSplitChatPanel(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isInteractionPanelVisible = false
            )
        )
        assertFalse(
            shouldShowLiveSplitChatPanel(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isInteractionPanelVisible = true
            )
        )
        assertTrue(
            shouldShowLiveLandscapeChatOverlay(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isInteractionPanelVisible = true
            )
        )
        assertFalse(
            shouldShowLiveLandscapeChatOverlay(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isInteractionPanelVisible = false
            )
        )
        assertFalse(
            shouldShowLiveLandscapeChatOverlay(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isInteractionPanelVisible = true
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

    @Test
    fun `embedded player controls do not consume system bar insets`() {
        assertFalse(
            shouldApplyLiveTopControlSystemInsets(
                layoutMode = LiveRoomLayoutMode.PortraitPanel,
                isFullscreen = false
            )
        )
        assertFalse(
            shouldApplyLiveBottomControlSystemInsets(
                layoutMode = LiveRoomLayoutMode.LandscapeSplit,
                isFullscreen = false,
                hasReservedBottomOverlay = false
            )
        )
    }

    @Test
    fun `edge player controls only reserve bottom insets when no lower panel is present`() {
        assertTrue(
            shouldApplyLiveTopControlSystemInsets(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isFullscreen = false
            )
        )
        assertFalse(
            shouldApplyLiveBottomControlSystemInsets(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isFullscreen = false,
                hasReservedBottomOverlay = true
            )
        )
        assertTrue(
            shouldApplyLiveBottomControlSystemInsets(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isFullscreen = false,
                hasReservedBottomOverlay = false
            )
        )
    }

    @Test
    fun `portrait overlay panel keeps video as primary content`() {
        val metrics = resolveLivePortraitOverlayMetrics(screenHeightDp = 844)
        val panelHeight = resolveLivePortraitOverlayPanelHeightDp(
            screenHeightDp = 844,
            metrics = metrics
        )

        assertEquals(0.38f, metrics.panelHeightFraction)
        assertTrue(panelHeight <= (844 * 0.40f).toInt())
        assertTrue(844 - panelHeight >= metrics.minPlayerClearanceDp)
        assertTrue(metrics.playerControlsGapDp >= 8)
    }

    @Test
    fun `portrait overlay uses room app bar instead of duplicate player top bar`() {
        assertFalse(
            shouldShowLivePlayerControlsTopBar(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isFullscreen = false
            )
        )
        assertTrue(
            shouldShowLivePlayerControlsTopBar(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isFullscreen = true
            )
        )
        assertTrue(
            shouldShowLivePlayerControlsTopBar(
                layoutMode = LiveRoomLayoutMode.LandscapeOverlay,
                isFullscreen = false
            )
        )
    }

    @Test
    fun `portrait overlay danmaku avoids app bar controls and visible interaction panel`() {
        val metrics = resolveLivePortraitOverlayMetrics(screenHeightDp = 844)
        val panelHeight = resolveLivePortraitOverlayPanelHeightDp(
            screenHeightDp = 844,
            metrics = metrics
        )
        val insets = resolveLiveOverlayContentInsets(
            layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
            portraitPanelHeightDp = panelHeight,
            portraitMetrics = metrics,
            isInteractionPanelVisible = true
        )

        assertTrue(insets.topDp >= metrics.topChromeReserveDp)
        assertTrue(insets.bottomDp > panelHeight)
        assertTrue(844 - insets.topDp - insets.bottomDp >= 320)
        assertEquals(
            LiveOverlayContentInsets(topDp = 0, bottomDp = 0),
            resolveLiveOverlayContentInsets(
                layoutMode = LiveRoomLayoutMode.PortraitPanel,
                portraitPanelHeightDp = panelHeight,
                portraitMetrics = metrics,
                isInteractionPanelVisible = true
            )
        )
    }

    @Test
    fun `portrait overlay does not reserve interaction panel when it is collapsed`() {
        val metrics = resolveLivePortraitOverlayMetrics(screenHeightDp = 844)
        val panelHeight = resolveLivePortraitOverlayPanelHeightDp(
            screenHeightDp = 844,
            metrics = metrics
        )
        val insets = resolveLiveOverlayContentInsets(
            layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
            portraitPanelHeightDp = panelHeight,
            portraitMetrics = metrics,
            isInteractionPanelVisible = false
        )

        assertEquals(metrics.topChromeReserveDp, insets.topDp)
        assertEquals(
            metrics.playerControlsGapDp + metrics.playerControlsReserveDp,
            insets.bottomDp
        )
        assertFalse(
            shouldReserveLivePortraitInteractionPanel(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isInteractionPanelVisible = false
            )
        )
        assertTrue(
            shouldReserveLivePortraitInteractionPanel(
                layoutMode = LiveRoomLayoutMode.PortraitVerticalOverlay,
                isInteractionPanelVisible = true
            )
        )
    }

    @Test
    fun `landscape chat overlay is bounded away from player chrome`() {
        val metrics = resolveLiveLandscapeChatOverlayMetrics(
            screenWidthDp = 844,
            screenHeightDp = 390
        )
        val overlayHeight = resolveLiveLandscapeChatOverlayHeightDp(
            screenHeightDp = 390,
            metrics = metrics
        )
        val overlayWidth = resolveLiveLandscapeChatOverlayWidthDp(
            screenWidthDp = 844,
            metrics = metrics
        )

        assertTrue(metrics.bottomControlReserveDp >= 88)
        assertTrue(overlayHeight <= 390 - metrics.topControlReserveDp - metrics.bottomControlReserveDp)
        assertTrue(overlayWidth <= metrics.maxWidthDp)
        assertTrue(overlayWidth >= metrics.minWidthDp)
    }
}
