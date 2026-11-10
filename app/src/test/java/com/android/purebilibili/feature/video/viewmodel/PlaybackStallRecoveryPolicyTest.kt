package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackStallRecoveryPolicyTest {

    @Test
    fun `startup and stalled CDN recovery wait ten seconds while rewritten URL timeout stays short`() {
        assertEquals(10_000L, resolvePlaybackStallRecoveryTimeoutMs(firstFrameRendered = false))
        assertEquals(10_000L, resolvePlaybackStallRecoveryTimeoutMs(firstFrameRendered = true))
        assertEquals(2_500L, PLAYBACK_CDN_FIRST_FRAME_FALLBACK_TIMEOUT_MS)
    }

    @Test
    fun `stalled playback switches to the next untried CDN`() {
        val decision = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = true,
            firstFrameRendered = true,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 0,
            cdnCandidateCount = 3,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = false
        )

        assertTrue(decision.shouldSwitchCdn)
        assertEquals(1, decision.nextCdnIndex)
    }

    @Test
    fun `each CDN candidate is selected at most once`() {
        val decision = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = true,
            firstFrameRendered = true,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 1,
            cdnCandidateCount = 3,
            attemptedCdnIndexes = setOf(0, 1),
            usesAdaptivePlayback = false
        )

        assertEquals(2, decision.nextCdnIndex)
        assertFalse(
            resolvePlaybackStallRecoveryDecision(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                firstFrameRendered = true,
                forwardBufferDurationMs = 0L,
                currentCdnIndex = 2,
                cdnCandidateCount = 3,
                attemptedCdnIndexes = setOf(0, 1, 2),
                usesAdaptivePlayback = false
            ).shouldSwitchCdn
        )
    }

    @Test
    fun `startup bad HTTP status tries each CDN once and stops after exhaustion`() {
        var recoveryState = StartupHttpStatusRecoveryState()
        var attemptedCdnIndexes = emptySet<Int>()
        var exhausted: StartupHttpStatusRecoveryDecision? = null

        for (currentCdnIndex in 0..2) {
            val decision = recordStartupHttpStatusFailure(
                state = recoveryState,
                attemptedCdnIndexes = attemptedCdnIndexes,
                currentCdnIndex = currentCdnIndex,
                cdnCandidateCount = 3
            )
            assertEquals(currentCdnIndex.takeIf { it < 2 }?.plus(1), decision.nextCdnIndex)
            attemptedCdnIndexes = decision.attemptedCdnIndexes + listOfNotNull(decision.nextCdnIndex)
            recoveryState = decision.state
            if (currentCdnIndex == 2) exhausted = decision
        }

        assertTrue(requireNotNull(exhausted).shouldReportExhaustion)
        val repeated = recordStartupHttpStatusFailure(
            state = recoveryState,
            attemptedCdnIndexes = attemptedCdnIndexes,
            currentCdnIndex = 2,
            cdnCandidateCount = 3
        )
        assertEquals(null, repeated.nextCdnIndex)
        assertFalse(repeated.shouldReportExhaustion)
    }

    @Test
    fun `first rendered frame disables startup HTTP recovery`() {
        val failure = recordStartupHttpStatusFailure(
            state = StartupHttpStatusRecoveryState(),
            attemptedCdnIndexes = emptySet(),
            currentCdnIndex = 0,
            cdnCandidateCount = 2
        )
        val afterFirstFrame = markPlaybackCdnFirstFrameRendered(failure.state)

        assertTrue(afterFirstFrame.hasRenderedFirstFrame)
        assertFalse(
            shouldUseStartupHttpStatusRecovery(
                errorCode = androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                state = afterFirstFrame
            )
        )
        assertFalse(
            shouldUseStartupHttpStatusRecovery(
                errorCode = androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                state = StartupHttpStatusRecoveryState()
            )
        )
    }

    @Test
    fun `explicit retry or route change clears only the startup failure notice`() {
        val state = StartupHttpStatusRecoveryState(
            hasRenderedFirstFrame = false,
            startupFailureReported = true
        )

        val afterUserAction = resetStartupHttpStatusRecoveryForUserAction(state)

        assertFalse(afterUserAction.startupFailureReported)
        assertTrue(shouldUseStartupHttpStatusRecovery(
            errorCode = androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            state = afterUserAction
        ))
        assertTrue(
            resetStartupHttpStatusRecoveryForUserAction(
                StartupHttpStatusRecoveryState(hasRenderedFirstFrame = true, startupFailureReported = true)
            ).hasRenderedFirstFrame
        )
    }

    @Test
    fun `ready paused ended or buffered playback does not recover`() {
        val common = { state: Int, playWhenReady: Boolean, forwardBufferMs: Long ->
            resolvePlaybackStallRecoveryDecision(
                playbackState = state,
                playWhenReady = playWhenReady,
                firstFrameRendered = true,
                forwardBufferDurationMs = forwardBufferMs,
                currentCdnIndex = 0,
                cdnCandidateCount = 2,
                attemptedCdnIndexes = setOf(0),
                usesAdaptivePlayback = false
            )
        }

        assertFalse(common(Player.STATE_READY, true, 0L).shouldSwitchCdn)
        assertFalse(common(Player.STATE_BUFFERING, false, 0L).shouldSwitchCdn)
        assertFalse(common(Player.STATE_ENDED, true, 0L).shouldSwitchCdn)
        assertFalse(common(Player.STATE_BUFFERING, true, 1L).shouldSwitchCdn)
    }

    @Test
    fun `startup without a first frame recovers only with an armed alternate CDN`() {
        assertTrue(
            resolvePlaybackStallRecoveryDecision(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                firstFrameRendered = false,
                forwardBufferDurationMs = 0L,
                currentCdnIndex = 0,
                cdnCandidateCount = 2,
                attemptedCdnIndexes = setOf(0),
                usesAdaptivePlayback = false,
                allowNoFirstFrameRecovery = true,
            ).shouldSwitchCdn
        )
        assertFalse(
            resolvePlaybackStallRecoveryDecision(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                firstFrameRendered = true,
                forwardBufferDurationMs = 0L,
                currentCdnIndex = 0,
                cdnCandidateCount = 1,
                attemptedCdnIndexes = setOf(0),
                usesAdaptivePlayback = false,
                allowNoFirstFrameRecovery = true,
            ).shouldSwitchCdn
        )
    }

    @Test
    fun `ten second no first frame recovery allows one stalled alternative including adaptive playback`() {
        val buffering = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = true,
            firstFrameRendered = false,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 0,
            cdnCandidateCount = 2,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = true,
            allowNoFirstFrameRecovery = true
        )
        val readyWithoutFrame = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_READY,
            playWhenReady = true,
            firstFrameRendered = false,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 0,
            cdnCandidateCount = 2,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = true,
            allowNoFirstFrameRecovery = true
        )

        assertEquals(1, buffering.nextCdnIndex)
        assertEquals(1, readyWithoutFrame.nextCdnIndex)
    }

    @Test
    fun `no first frame recovery keeps pause progress and candidate limits`() {
        val common = { playWhenReady: Boolean, forwardBufferMs: Long, candidateCount: Int ->
            resolvePlaybackStallRecoveryDecision(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = playWhenReady,
                firstFrameRendered = false,
                forwardBufferDurationMs = forwardBufferMs,
                currentCdnIndex = 0,
                cdnCandidateCount = candidateCount,
                attemptedCdnIndexes = setOf(0),
                usesAdaptivePlayback = true,
                allowNoFirstFrameRecovery = true
            )
        }

        assertFalse(common(false, 0L, 2).shouldSwitchCdn)
        assertFalse(common(true, 501L, 2).shouldSwitchCdn)
        assertFalse(common(true, 0L, 1).shouldSwitchCdn)
        assertFalse(
            resolvePlaybackStallRecoveryDecision(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                firstFrameRendered = false,
                forwardBufferDurationMs = 0L,
                currentCdnIndex = 0,
                cdnCandidateCount = 2,
                attemptedCdnIndexes = setOf(0),
                usesAdaptivePlayback = true
            ).shouldSwitchCdn
        )
    }

    @Test
    fun `startup recovery handles a stalled short buffer but preserves a usable buffer`() {
        fun decision(bufferMs: Long, playWhenReady: Boolean = true) = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = playWhenReady,
            firstFrameRendered = false,
            forwardBufferDurationMs = bufferMs,
            currentCdnIndex = 0,
            cdnCandidateCount = 2,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = false,
            allowNoFirstFrameRecovery = true,
        )
        assertTrue(decision(400L).shouldSwitchCdn)
        assertFalse(decision(700L).shouldSwitchCdn)
        assertFalse(decision(0L, playWhenReady = false).shouldSwitchCdn)
    }

    @Test
    fun `AUTO adaptive playback remains with Media3 while locked quality can switch CDN`() {
        val adaptive = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = true,
            firstFrameRendered = true,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 0,
            cdnCandidateCount = 2,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = true
        )
        val locked = resolvePlaybackStallRecoveryDecision(
            playbackState = Player.STATE_BUFFERING,
            playWhenReady = true,
            firstFrameRendered = true,
            forwardBufferDurationMs = 0L,
            currentCdnIndex = 0,
            cdnCandidateCount = 2,
            attemptedCdnIndexes = setOf(0),
            usesAdaptivePlayback = false
        )

        assertFalse(adaptive.shouldSwitchCdn)
        assertEquals(1, locked.nextCdnIndex)
    }
}
