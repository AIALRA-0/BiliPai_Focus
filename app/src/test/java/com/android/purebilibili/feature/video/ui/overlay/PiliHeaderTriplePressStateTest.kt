package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.BroadcastFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PiliHeaderTriplePressStateTest {
    @Test
    fun quickTapLikes_withoutSubmittingTriple() = runTest {
        var likes = 0
        var triples = 0
        val state = PiliHeaderTriplePressState(this)
        state.start(false, {}, { triples++ }, {})
        runCurrent()
        advanceTimeBy(199)
        state.release(tapUp = true, onLike = { likes++ })
        advanceTimeBy(2_000)
        assertEquals(1, likes)
        assertEquals(0, triples)
    }

    @Test
    fun canceledTouchDoesNotLike() = runTest {
        var likes = 0
        val state = PiliHeaderTriplePressState(this)
        state.start(false, {}, {}, {})
        runCurrent()
        state.release(tapUp = false, onLike = { likes++ })
        assertEquals(0, likes)
    }

    @Test
    fun alreadyTripled_holdShowsFeedbackWithoutResubmittingOrLiking() = runTest {
        var likes = 0
        var triples = 0
        var alreadyTripled = 0
        val state = PiliHeaderTriplePressState(this)
        state.start(true, {}, { triples++ }, { alreadyTripled++ })
        runCurrent()
        advanceTimeBy(PILI_HEADER_TRIPLE_DELAY_MILLIS)
        runCurrent()
        state.release(tapUp = true, onLike = { likes++ })
        assertEquals(1, alreadyTripled)
        assertEquals(0, triples)
        assertEquals(0, likes)
    }

    @Test
    fun completeHoldSubmitsOnce_andReleaseDoesNotAlsoLike() = runTest {
        var likes = 0
        var triples = 0
        val clock = BroadcastFrameClock()
        val state = PiliHeaderTriplePressState(CoroutineScope(coroutineContext + clock))
        state.start(false, {}, { triples++ }, {})
        runCurrent()
        advanceTimeBy(PILI_HEADER_TRIPLE_DELAY_MILLIS)
        runCurrent()
        clock.sendFrame(0L)
        runCurrent()
        clock.sendFrame(PILI_HEADER_TRIPLE_HOLD_MILLIS * 1_000_000L)
        runCurrent()
        state.release(tapUp = true, onLike = { likes++ })
        assertEquals(1, triples)
        assertEquals(0, likes)
        assertEquals(0f, state.progress)
    }

    @Test
    fun animationTimingMatchesMobileReference() {
        assertEquals(200L, PILI_HEADER_TRIPLE_DELAY_MILLIS)
        assertEquals(1200L, PILI_HEADER_TRIPLE_HOLD_MILLIS)
        assertEquals(400, PILI_HEADER_TRIPLE_REVERSE_MILLIS)
        assertEquals(0f, resolvePiliHeaderTripleProgress(-1))
        assertEquals(0.5f, resolvePiliHeaderTripleProgress(600_000_000L), 0.001f)
        assertEquals(1f, resolvePiliHeaderTripleProgress(2_000_000_000L))
    }
}
