package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailInlineScrollRecoveryTest {
    @Test
    fun fullscreenReturnReleasesRetainedMutationsWithoutResettingPositions() = runTest {
        val positions = floatArrayOf(120f, 480f, 1.25f)
        val states = positions.indices.map { index ->
            ScrollableState { delta ->
                positions[index] += delta
                delta
            }
        }
        // List flings, direct drags and a higher-priority mutation must all release ownership.
        val priorities = listOf(
            MutatePriority.Default,
            MutatePriority.UserInput,
            MutatePriority.PreventUserInput,
        )
        val oldMutations = states.mapIndexed { index, state ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                state.scroll(priorities[index]) { awaitCancellation() }
            }
        }
        states.forEach { assertTrue(it.isScrollInProgress) }

        stopVideoDetailInlineScrolls(states[0], states[1], states[2])

        oldMutations.forEach { assertTrue(it.isCancelled) }
        states.forEach { assertFalse(it.isScrollInProgress) }
        assertEquals(listOf(120f, 480f, 1.25f), positions.toList())
        states.forEach { state ->
            state.scroll(MutatePriority.UserInput) { scrollBy(10f) }
        }
        assertEquals(listOf(130f, 490f, 11.25f), positions.toList())
        states.forEach { assertFalse(it.isScrollInProgress) }
    }

    @Test
    fun unopenedListsDoNotDelayRecoveryOfTheActivePager() = runTest {
        // A restored or unopened tab has no layout to unblock LazyListState.scroll().
        val introList = LazyListState(4, 29)
        val commentList = LazyListState(11, 37)
        var pagerPosition = 1f
        val pager = ScrollableState { delta ->
            pagerPosition += delta
            delta
        }
        val oldPagerDrag = launch(start = CoroutineStart.UNDISPATCHED) {
            pager.scroll(MutatePriority.UserInput) { awaitCancellation() }
        }

        withTimeout(1_000) {
            stopVideoDetailInlineScrolls(introList, commentList, pager)
        }

        assertTrue(oldPagerDrag.isCancelled)
        assertFalse(pager.isScrollInProgress)
        assertEquals(4, introList.firstVisibleItemIndex)
        assertEquals(29, introList.firstVisibleItemScrollOffset)
        assertEquals(11, commentList.firstVisibleItemIndex)
        assertEquals(37, commentList.firstVisibleItemScrollOffset)
        pager.scroll(MutatePriority.UserInput) { scrollBy(1f) }
        assertEquals(2f, pagerPosition)
    }
}
