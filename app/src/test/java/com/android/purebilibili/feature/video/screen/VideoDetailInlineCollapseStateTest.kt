package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.policy.reduceVideoDetailPostScroll
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPreScroll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailInlineCollapseStateTest {
    @Test
    fun switchingBetweenIntroAndCommentsKeepsPartialCollapseAndNextDragContinues() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -120f)
        // Both tabs share one player. Their different list positions must not reset it.
        repeat(4) {
            state.reconcileLayout(collapseEnabled = true, collapseRangePx = 300f)
            assertEquals(-120f, state.offsetPx)
            assertFalse(state.restoreRequested)
        }
        val up = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -30f,
            minOffsetPx = -300f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )
        assertEquals(-30f, state.consumeScroll(up))
        assertEquals(-150f, state.offsetPx)
        state.reconcileLayout(collapseEnabled = true, collapseRangePx = 300f)
        val down = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 20f,
            minOffsetPx = -300f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )
        assertEquals(20f, state.consumeScroll(down))
        assertEquals(-130f, state.offsetPx)
    }

    @Test
    fun switchingTabsKeepsExpandedAndFullyCollapsedEndpoints() {
        for (offset in listOf(0f, -300f)) {
            val state = InlinePortraitPlayerCollapseState(initialOffsetPx = offset)
            repeat(4) { state.reconcileLayout(collapseEnabled = true, collapseRangePx = 300f) }
            assertEquals(offset, state.offsetPx)
            assertFalse(state.restoreRequested)
        }
    }

    @Test
    fun disablingCollapseRequestsAnimatedRestore() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -120f)
        state.reconcileLayout(collapseEnabled = false, collapseRangePx = 300f)
        assertEquals(0f, state.offsetPx)
        assertTrue(state.restoreRequested)
        assertFalse(state.layoutReconciled)
        state.reconcileLayout(collapseEnabled = false, collapseRangePx = 300f)
        assertTrue(state.restoreRequested)
    }

    @Test
    fun shrinkingPausedCommentRangeClampsSharedCollapseWithoutExpanding() {
        // The paused comment tab can hide the player past the intro tab's compact endpoint.
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -350f)
        state.reconcileLayout(collapseEnabled = true, collapseRangePx = 263f)

        assertEquals(-263f, state.offsetPx)
        assertFalse(state.restoreRequested)
        assertTrue(state.layoutReconciled)

        // Re-grabbing the clamped player follows the finger and consumes the correction.
        val down = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 20f,
            minOffsetPx = -263f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )
        assertEquals(20f, state.consumeScroll(down))
        assertEquals(-243f, state.offsetPx)
        assertFalse(state.layoutReconciled)
    }

    @Test
    fun growingCommentRangeKeepsTheSharedCollapse() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -263f)
        state.reconcileLayout(collapseEnabled = true, collapseRangePx = 494f)
        assertEquals(-263f, state.offsetPx)
        assertFalse(state.restoreRequested)
        assertFalse(state.layoutReconciled)
    }

    @Test
    fun scrollBeforeReconcileStartsFromTheVisibleOffsetInsteadOfSnapping() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -350f)
        // A scroll delivered between the tab change and the reconcile effect must start
        // from the currently visible offset, not jump to the new range's endpoint.
        val down = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 20f,
            minOffsetPx = minOf(-263f, state.offsetPx),
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )
        assertEquals(20f, state.consumeScroll(down))
        assertEquals(-330f, state.offsetPx)
    }

    @Test
    fun disabledCollapse_commentScrollDoesNotInterruptRestore() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -120f)
        state.restore()

        val preScroll = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -24f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = false,
            isPortraitFullscreen = false,
        )
        assertEquals(0f, state.consumeScroll(preScroll))
        val postScroll = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 24f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = false,
            isPortraitFullscreen = false,
        )
        assertEquals(0f, state.consumeScroll(postScroll))
        assertEquals(0f, state.offsetPx)
        assertTrue(state.restoreRequested)
    }

    @Test
    fun expandedPlayer_downwardCommentDragDoesNotInterruptRestore() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )

        assertEquals(0f, state.consumeScroll(update))
        assertTrue(state.restoreRequested)
    }

    @Test
    fun consumedPlayerDrag_interruptsRestoreAndFollowsFinger() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )

        assertEquals(-30f, state.consumeScroll(update))
        assertEquals(-30f, state.offsetPx)
        assertFalse(state.restoreRequested)
    }

    @Test
    fun fullscreenCommentScroll_doesNotMutateInlinePlayerState() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = true,
        )

        assertEquals(0f, state.consumeScroll(update))
        assertEquals(0f, state.offsetPx)
        assertTrue(state.restoreRequested)
    }
}
