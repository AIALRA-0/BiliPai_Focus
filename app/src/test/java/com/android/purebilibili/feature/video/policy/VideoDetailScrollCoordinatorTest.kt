package com.android.purebilibili.feature.video.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoDetailScrollCoordinatorTest {

    @Test
    fun collapseJankSignalOnlyTracksRealOffsetMovement() {
        assertTrue(
            shouldTrackVideoDetailCollapseMotion(
                inlinePortraitScrollEnabled = true,
                previousOffsetPx = -80f,
                currentOffsetPx = -96f,
            )
        )
        assertFalse(
            shouldTrackVideoDetailCollapseMotion(
                inlinePortraitScrollEnabled = true,
                previousOffsetPx = -96f,
                currentOffsetPx = -96f,
            )
        )
        assertFalse(
            shouldTrackVideoDetailCollapseMotion(
                inlinePortraitScrollEnabled = true,
                previousOffsetPx = -96f,
                currentOffsetPx = -96.5f,
            )
        )
        assertFalse(
            shouldTrackVideoDetailCollapseMotion(
                inlinePortraitScrollEnabled = false,
                previousOffsetPx = -80f,
                currentOffsetPx = -96f,
            )
        )
    }

    @Test
    fun preScroll_doesNothingWhenInlineCollapseDisabled() {
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = -40f,
            deltaPx = -30f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = false,
            isPortraitFullscreen = false
        )

        assertNull(update)
    }

    @Test
    fun preScroll_consumesUpwardSwipeToCollapsePlayer() {
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = -40f,
            deltaPx = -30f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false
        )

        assertEquals(-70f, update?.nextOffsetPx)
        assertEquals(-30f, update?.consumedDeltaPx)
    }

    @Test
    fun postScroll_consumesDownwardSwipeToRestorePlayer() {
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = -80f,
            deltaPx = 25f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false
        )

        assertEquals(-55f, update?.nextOffsetPx)
        assertEquals(25f, update?.consumedDeltaPx)
    }

    @Test
    fun postScroll_doesNothingInPortraitFullscreen() {
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = -80f,
            deltaPx = 25f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = true
        )

        assertNull(update)
    }

    @Test
    fun preScroll_doesNothingWhenLayoutAlreadyCollapsedByIntroThreshold() {
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = 0f,
            deltaPx = -40f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = true,
        )
        assertNull(update)
        assertFalse(
            shouldSkipGesturePlayerCollapseForLayout(
                compactForIntroScroll = true,
                compactForCommentTab = false,
            )
        )
        assertFalse(
            shouldSkipGesturePlayerCollapseForLayout(
                compactForIntroScroll = false,
                compactForCommentTab = false,
            )
        )
    }

    @Test
    fun postScroll_doesNothingWhenLayoutAlreadyCollapsed() {
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = -80f,
            deltaPx = 25f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = true,
        )
        assertNull(update)
    }

    @Test
    fun postScroll_restoresPortraitPlayerAfterIntroThreshold() {
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = -320f,
            deltaPx = 48f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = false,
        )

        assertEquals(-272f, update?.nextOffsetPx)
        assertEquals(48f, update?.consumedDeltaPx)
    }

    @Test
    fun restoredCompactCommentsAllowTheNextCollapseGesture() {
        assertTrue(
            shouldSkipGesturePlayerCollapseForLayout(
                compactForIntroScroll = false,
                compactForCommentTab = true,
            )
        )
        val skipCollapse = shouldSkipGesturePlayerCollapseForLayout(
            compactForIntroScroll = false,
            compactForCommentTab = true,
            restoreRequested = true,
        )
        assertFalse(skipCollapse)
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = 0f,
            deltaPx = -40f,
            minOffsetPx = -320f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = skipCollapse,
        )
        assertEquals(-40f, update?.nextOffsetPx)
    }

    @Test
    fun pausedCommentHide_continuesPastCompactFoldableLayoutAndRestoresAtTop() {
        val skipCollapse = shouldSkipGesturePlayerCollapseForLayout(
            compactForIntroScroll = false,
            compactForCommentTab = true,
            hidePausedPlayerForComments = true,
        )
        assertFalse(skipCollapse)
        val collapsed = reduceVideoDetailPreScroll(
            currentOffsetPx = -360f,
            deltaPx = -300f,
            minOffsetPx = -600f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = skipCollapse,
        )
        assertEquals(-600f, collapsed?.nextOffsetPx)
        assertEquals(-240f, collapsed?.consumedDeltaPx)
        val restored = reduceVideoDetailPostScroll(
            currentOffsetPx = -600f,
            deltaPx = 600f,
            minOffsetPx = -600f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
            layoutAlreadyCollapsed = skipCollapse,
        )
        assertEquals(0f, restored?.nextOffsetPx)
        assertEquals(600f, restored?.consumedDeltaPx)
    }

    @Test
    fun collapseProgress_clampsIntoZeroToOneRange() {
        assertEquals(
            0f,
            resolveVideoDetailCollapseProgress(
                playerHeightOffsetPx = 40f,
                collapseRangePx = 320f,
                isPortraitFullscreen = false
            )
        )
        assertEquals(
            0.5f,
            resolveVideoDetailCollapseProgress(
                playerHeightOffsetPx = -160f,
                collapseRangePx = 320f,
                isPortraitFullscreen = false
            )
        )
        assertEquals(
            0f,
            resolveVideoDetailCollapseProgress(
                playerHeightOffsetPx = -160f,
                collapseRangePx = 320f,
                isPortraitFullscreen = true
            )
        )
    }
}
