package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RelatedVideoLoadSchedulerTest {
    private val token = 7L
    private val weakStartup = RelatedVideoLoadSnapshot(token, constrainedNetwork = true)
    private val bufferedPlayback = weakStartup.copy(
        playbackPrepared = true,
        firstFrameRendered = true,
        playbackReady = true,
        bufferedDurationMs = 1_500L
    )

    @Test
    fun normalNetworkWaitsForFirstFrame() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(token, RelatedVideoLoadSnapshot(token)))
        assertEquals(RelatedVideoLoadDecision.START, relatedVideoLoadDecision(
            token, RelatedVideoLoadSnapshot(token, playbackPrepared = true, firstFrameRendered = true)
        ))
    }

    @Test
    fun weakNetworkDefersStartup() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(token, weakStartup))
    }

    @Test
    fun cachedRelatedResultsBypassWeakNetworkDelay() = runTest {
        assertTrue(RelatedVideoLoadScheduler().awaitPermit(token, { weakStartup.copy(hasCachedRelated = true) }))
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun readyPlaybackWithoutAFirstFrameStillDefers() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(firstFrameRendered = false)
        ))
    }

    @Test
    fun oldPlayerReadinessCannotReleaseNewStartup() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(playbackPrepared = false)
        ))
    }

    @Test
    fun firstFrameWithTooLittleBufferStillDefers() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(bufferedDurationMs = 1_499L)
        ))
    }

    @Test
    fun firstFrameWithBufferHeadroomReleasesRelatedRequest() {
        assertEquals(RelatedVideoLoadDecision.START, relatedVideoLoadDecision(token, bufferedPlayback))
    }

    @Test
    fun bufferingEvenAfterFirstFrameDoesNotReleaseRelatedRequest() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(playbackReady = false, buffering = true)
        ))
    }

    @Test
    fun rebufferingDefersNewRelatedAttemptsEvenWithoutALowBandwidthHint() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(constrainedNetwork = false, playbackReady = false, buffering = true)
        ))
    }

    @Test
    fun pausedReadyPlaybackDoesNotWaitForAFirstFrameOrBuffer() {
        assertEquals(RelatedVideoLoadDecision.START, relatedVideoLoadDecision(
            token, bufferedPlayback.copy(firstFrameRendered = false, playWhenReady = false, bufferedDurationMs = 0L)
        ))
    }

    @Test
    fun disabledAutoplayStillWaitsForPreparationAndReadiness() {
        assertEquals(RelatedVideoLoadDecision.DEFER, relatedVideoLoadDecision(
            token, weakStartup.copy(playWhenReady = false)
        ))
    }

    @Test
    fun playbackFailureAndEndReleaseRelatedRequest() {
        assertEquals(RelatedVideoLoadDecision.START, relatedVideoLoadDecision(token, weakStartup.copy(playbackFailed = true)))
        assertEquals(RelatedVideoLoadDecision.START, relatedVideoLoadDecision(token, weakStartup.copy(playbackEnded = true)))
    }

    @Test
    fun staleRequestsCannotUseTheCacheOrFailureEscapeHatch() {
        assertEquals(RelatedVideoLoadDecision.CANCEL, relatedVideoLoadDecision(
            token, weakStartup.copy(requestToken = token + 1, hasCachedRelated = true, playbackFailed = true)
        ))
    }

    @Test
    fun bufferingReleaseIsObservedWithoutWaitingForTheTimeout() = runTest {
        var snapshot = weakStartup
        val permit = async { RelatedVideoLoadScheduler().awaitPermit(token, { snapshot }) }
        runCurrent()
        assertFalse(permit.isCompleted)
        snapshot = bufferedPlayback
        advanceTimeBy(200L)
        runCurrent()
        assertTrue(permit.await())
        assertEquals(200L, testScheduler.currentTime)
    }

    @Test
    fun networkRecoveryReleasesStartupWithoutWaitingForTheFirstFrame() = runTest {
        var snapshot = weakStartup
        val permit = async { RelatedVideoLoadScheduler().awaitPermit(token, { snapshot }) }
        runCurrent()
        snapshot = snapshot.copy(constrainedNetwork = false)
        advanceTimeBy(200L)
        runCurrent()
        assertTrue(permit.await())
    }

    @Test
    fun aCacheFilledByAnotherConsumerReleasesTheWait() = runTest {
        var snapshot = weakStartup
        val permit = async { RelatedVideoLoadScheduler().awaitPermit(token, { snapshot }) }
        runCurrent()
        snapshot = snapshot.copy(hasCachedRelated = true)
        advanceTimeBy(200L)
        runCurrent()
        assertTrue(permit.await())
    }

    @Test
    fun permanentlySlowPlaybackGetsABoundedSixSecondWait() = runTest {
        var deferrals = 0
        val permit = async { RelatedVideoLoadScheduler().awaitPermit(token, { weakStartup }, { deferrals += 1 }) }
        runCurrent()
        advanceTimeBy(5_999L)
        assertFalse(permit.isCompleted)
        advanceTimeBy(1L)
        runCurrent()
        assertTrue(permit.await())
        assertEquals(6_000L, testScheduler.currentTime)
        assertEquals(1, deferrals)
    }

    @Test
    fun changingVideosWhileWaitingNeverStartsTheOldRequest() = runTest {
        var snapshot = weakStartup
        var networkCalls = 0
        val request = async {
            if (RelatedVideoLoadScheduler().awaitPermit(token, { snapshot })) networkCalls += 1
        }
        runCurrent()
        snapshot = snapshot.copy(requestToken = token + 1)
        advanceTimeBy(200L)
        runCurrent()
        request.await()
        assertEquals(0, networkCalls)
    }

    @Test
    fun aTokenChangeAtTheTimeoutBoundaryCannotStartTheOldRequest() = runTest {
        var snapshot = weakStartup
        val permit = async { RelatedVideoLoadScheduler().awaitPermit(token, { snapshot }) }
        runCurrent()
        advanceTimeBy(5_999L)
        snapshot = snapshot.copy(requestToken = token + 1)
        advanceTimeBy(1L)
        runCurrent()
        assertFalse(permit.await())
    }

    @Test
    fun exitingThePageCancelsTheWaitWithoutStartingAnyRequest() = runTest {
        var networkCalls = 0
        val request = async {
            if (RelatedVideoLoadScheduler().awaitPermit(token, { weakStartup })) networkCalls += 1
        }
        runCurrent()
        request.cancelAndJoin()
        advanceTimeBy(6_000L)
        runCurrent()
        assertEquals(0, networkCalls)
        assertTrue(request.isCancelled)
    }

    @Test
    fun recentMeasuredBandwidthOverridesTheLinkEstimate() {
        assertTrue(isConstrainedVideoNetwork(1L, 100_000, VideoBandwidthSample(1L, 500_000L, 0L), 1_000L))
        assertFalse(isConstrainedVideoNetwork(1L, 500, VideoBandwidthSample(1L, 10_000_000L, 0L), 1_000L))
    }

    @Test
    fun staleMeasurementsFallBackToTheCurrentLinkEstimate() {
        assertFalse(isConstrainedVideoNetwork(1L, 100_000, VideoBandwidthSample(1L, 500_000L, 0L), 30_001L))
        assertTrue(isConstrainedVideoNetwork(1L, 500, VideoBandwidthSample(1L, 10_000_000L, 0L), 30_001L))
    }

    @Test
    fun anotherNetworksMeasurementCannotMarkTheCurrentNetworkAsWeak() {
        assertFalse(isConstrainedVideoNetwork(2L, 100_000, VideoBandwidthSample(1L, 500_000L, 0L), 1_000L))
    }

    @Test
    fun unknownBandwidthAndUnavailableCapabilitiesDoNotPenalizeNormalStartup() {
        assertFalse(isConstrainedVideoNetwork(1L, 0, null, 0L))
        assertFalse(isConstrainedVideoNetwork(null, 500, VideoBandwidthSample(1L, 500_000L, 0L), 0L))
    }

    @Test
    fun invalidOrFutureSamplesAreIgnored() {
        assertFalse(isConstrainedVideoNetwork(1L, 100_000, VideoBandwidthSample(1L, 0L, 0L), 1_000L))
        assertFalse(isConstrainedVideoNetwork(1L, 100_000, VideoBandwidthSample(1L, 500_000L, 2_000L), 1_000L))
    }

    @Test
    fun linkBandwidthThresholdDoesNotClassifyEveryCellularNetworkAsWeak() {
        assertTrue(isConstrainedVideoNetwork(1L, 1_500, null, 0L))
        assertFalse(isConstrainedVideoNetwork(1L, 1_501, null, 0L))
    }
}
