package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoCardDeferredPixelCaptureTest {
    @Test
    fun landingFramesDoNotReadPixelsUntilTheMatchingDetailReleasesEntryWork() = runTest {
        val clock = BroadcastFrameClock()
        val owner = com.android.purebilibili.core.util.VideoCardPixelOwner("home:BV1", 1L)
        val unrelated = com.android.purebilibili.core.util.VideoCardPixelOwner("home:BV1", 1L)
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                { VideoCardTransitionBackgroundPhase.HELD }, { false }, { true },
                entryWorkReleased = { owner.entryWorkReleased },
            ) { captures++ }
        }
        runCurrent()
        repeat(8) { clock.sendFrame(it.toLong()); runCurrent() }
        assertEquals(0, captures)
        unrelated.releaseEntryWork()
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(0, captures)
        owner.releaseEntryWork()
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        clock.sendFrame(10L)
        runCurrent()
        assertEquals(0, captures)
        clock.sendFrame(11L)
        runCurrent()
        assertEquals(1, captures)
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun returnWhileWaitingForDetailWorkPermanentlyRevokesCapture() = runTest {
        val clock = BroadcastFrameClock()
        val phase = mutableStateOf(VideoCardTransitionBackgroundPhase.HELD)
        val released = mutableStateOf(false)
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                { phase.value }, { false }, { true }, entryWorkReleased = { released.value },
            ) { captures++ }
        }
        runCurrent()
        phase.value = VideoCardTransitionBackgroundPhase.RETURNING
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(true, job.isCompleted)
        phase.value = VideoCardTransitionBackgroundPhase.HELD
        released.value = true
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        repeat(4) { clock.sendFrame(it.toLong()); runCurrent() }
        assertEquals(0, captures)
    }

    @Test
    fun captureWaitsForLandingAndACompleteFrame() = runTest {
        val clock = BroadcastFrameClock()
        val phase = mutableStateOf(VideoCardTransitionBackgroundPhase.OPENING)
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry({ phase.value }, { false }, { true }) { captures++ }
        }
        runCurrent()
        assertEquals(0, captures)
        phase.value = VideoCardTransitionBackgroundPhase.HELD
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        clock.sendFrame(1L)
        runCurrent()
        assertEquals(0, captures)
        clock.sendFrame(2L)
        runCurrent()
        assertEquals(1, captures)
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun quickReturnDoesNotStartPixelReadback() = runTest {
        val phase = mutableStateOf(VideoCardTransitionBackgroundPhase.OPENING)
        var captures = 0
        launch {
            captureVideoCardPixelsAfterEntry({ phase.value }, { false }, { true }) { captures++ }
        }
        runCurrent()
        phase.value = VideoCardTransitionBackgroundPhase.RETURNING
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(0, captures)
    }

    @Test
    fun repeatedClickAndReturnGestureRevokePendingCapture() = runTest {
        for (loseOwnership in listOf(false, true)) {
            val clock = BroadcastFrameClock()
            var ownsCapture = true
            var gesture = false
            var captures = 0
            launch(clock) {
                captureVideoCardPixelsAfterEntry(
                    { VideoCardTransitionBackgroundPhase.HELD }, { gesture }, { ownsCapture },
                ) { captures++ }
            }
            runCurrent()
            clock.sendFrame(1L)
            runCurrent()
            if (loseOwnership) ownsCapture = false else gesture = true
            clock.sendFrame(2L)
            runCurrent()
            assertEquals(0, captures)
        }
    }

    @Test
    fun cancelledBackGestureDoesNotReviveCaptureBetweenFrames() = runTest {
        val clock = BroadcastFrameClock()
        val gesture = mutableStateOf(false)
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                { VideoCardTransitionBackgroundPhase.HELD }, { gesture.value }, { true },
            ) { captures++ }
        }
        runCurrent()
        clock.sendFrame(1L)
        runCurrent()
        gesture.value = true
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        gesture.value = false
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        clock.sendFrame(2L)
        runCurrent()
        assertEquals(0, captures)
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun returningWhilePixelReadbackIsPendingCancelsPublication() = runTest {
        val clock = BroadcastFrameClock()
        val phase = mutableStateOf(VideoCardTransitionBackgroundPhase.HELD)
        val pixelsReady = CompletableDeferred<Unit>()
        var started = false
        var published = false
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry({ phase.value }, { false }, { true }) {
                started = true
                pixelsReady.await()
                published = true
            }
        }
        runCurrent()
        repeat(2) { index ->
            clock.sendFrame(index.toLong() + 1L)
            runCurrent()
        }
        assertEquals(true, started)
        phase.value = VideoCardTransitionBackgroundPhase.RETURNING
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        pixelsReady.complete(Unit)
        runCurrent()
        assertEquals(false, published)
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun restoringAnotherActiveSourceRevokesCaptureWithoutWaitingForAnotherFrame() = runTest {
        val clock = BroadcastFrameClock()
        val activeOwner = mutableStateOf(true)
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                { VideoCardTransitionBackgroundPhase.HELD }, { false }, { activeOwner.value },
            ) { captures++ }
        }
        runCurrent()
        activeOwner.value = false
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(true, job.isCompleted)
        assertEquals(0, captures)
    }

    @Test
    fun backgroundingDuringOpeningEndsTheTaskAndForegroundDoesNotRestartIt() = runTest {
        val phase = mutableStateOf(VideoCardTransitionBackgroundPhase.OPENING)
        val allowed = mutableStateOf(true)
        var captures = 0
        val job = launch {
            captureVideoCardPixelsAfterEntry(
                phaseProvider = { phase.value },
                isReturnGestureInProgress = { false },
                ownsCapture = { true },
                captureAllowed = { allowed.value },
            ) { captures++ }
        }
        runCurrent()
        allowed.value = false
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(true, job.isCompleted)
        allowed.value = true
        phase.value = VideoCardTransitionBackgroundPhase.HELD
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(0, captures)
    }

    @Test
    fun losingWindowPermissionCancelsPendingReadbackAndDoesNotPublishOnResume() = runTest {
        val clock = BroadcastFrameClock()
        val allowed = mutableStateOf(true)
        val pixelsReady = CompletableDeferred<Unit>()
        var started = false
        var published = false
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                phaseProvider = { VideoCardTransitionBackgroundPhase.HELD },
                isReturnGestureInProgress = { false },
                ownsCapture = { true },
                captureAllowed = { allowed.value },
            ) {
                started = true
                pixelsReady.await()
                published = true
            }
        }
        runCurrent()
        repeat(2) { index ->
            clock.sendFrame(index.toLong() + 1L)
            runCurrent()
        }
        assertEquals(true, started)
        allowed.value = false
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        runCurrent()
        allowed.value = true
        pixelsReady.complete(Unit)
        runCurrent()
        assertEquals(false, published)
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun disposingSourceCancelsPendingCapture() = runTest {
        val clock = BroadcastFrameClock()
        var captures = 0
        val job = launch(clock) {
            captureVideoCardPixelsAfterEntry(
                { VideoCardTransitionBackgroundPhase.HELD }, { false }, { true },
            ) { captures++ }
        }
        runCurrent()
        clock.sendFrame(1L)
        runCurrent()
        job.cancel()
        runCurrent()
        clock.sendFrame(2L)
        runCurrent()
        assertEquals(0, captures)
    }
}
