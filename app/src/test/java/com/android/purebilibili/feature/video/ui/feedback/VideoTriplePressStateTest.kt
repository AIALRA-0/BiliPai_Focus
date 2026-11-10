package com.android.purebilibili.feature.video.ui.feedback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoTriplePressStateTest {
    @Test
    fun holdProgress_usesElapsedTimeAndRequiresTheFullHold() {
        assertEquals(0f, resolveVideoTripleHoldProgress(-1L))
        assertEquals(0.5f, resolveVideoTripleHoldProgress(450_000_000L))
        assertTrue(resolveVideoTripleHoldProgress(899_000_000L) < 1f)
        assertEquals(1f, resolveVideoTripleHoldProgress(900_000_000L))
        assertEquals(1f, resolveVideoTripleHoldProgress(1_800_000_000L))
    }

    @Test
    fun releasedOrIncompleteHold_cannotSubmit() {
        val state = VideoTriplePressState()
        state.start()
        state.updateProgress(0.6f)
        assertFalse(state.complete())
        state.release()
        state.updateProgress(1f)
        assertFalse(state.complete())
    }

    @Test
    fun completedHold_submitsOnlyOnceAndNextHoldStartsFresh() {
        val state = VideoTriplePressState()
        state.start()
        state.updateProgress(1f)
        assertTrue(state.complete())
        assertTrue(state.completed)
        assertFalse(state.complete())
        state.start()
        assertEquals(0f, state.progress)
        assertFalse(state.completed)
        assertFalse(state.complete())
    }

    @Test
    fun restartingDuringRetraction_doesNotInheritOldProgress() {
        val state = VideoTriplePressState()
        state.start()
        state.updateProgress(0.8f)
        state.release()
        state.start()
        assertEquals(0f, state.progress)
        assertTrue(state.isPressing)
    }
}
