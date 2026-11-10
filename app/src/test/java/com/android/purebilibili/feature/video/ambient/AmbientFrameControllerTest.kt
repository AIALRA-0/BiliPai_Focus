package com.android.purebilibili.feature.video.ambient

import androidx.compose.runtime.snapshots.Snapshot
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AmbientFrameControllerTest {
    private val policy = AmbientRunPolicy(true, true, true, false, 0, false)

    @Test fun cancelledPlatformRequestMustCompleteBeforeNextSessionCanCapture() = runTest {
        val complete = CompletableDeferred<Unit>()
        var captures = 0
        var released = false
        val source = object : AmbientFrameSource {
            override suspend fun capture(width: Int, height: Int): Bitmap? {
                captures++
                withContext(NonCancellable) { complete.await() }
                return null
            }
            override fun release() { released = true }
        }
        val controller = AmbientFrameController(AmbientPresentation()) { testScheduler.currentTime }
        val first = launch { controller.run(source, 1920, 1080) { policy } }
        runCurrent()
        first.cancel()
        controller.invalidate()
        val second = launch { controller.run(source, 1920, 1080) { policy } }
        runCurrent()
        assertEquals(1, captures)
        assertFalse(released)
        complete.complete(Unit)
        runCurrent()
        assertTrue(released)
        assertEquals(2, captures)
        second.cancelAndJoin()
        first.join()
    }

    @Test fun failuresBackOffWithoutQueuingRequestsAndInactivePlayerDoesNotSample() = runTest {
        var captures = 0
        var active by mutableStateOf(false)
        val source = object : AmbientFrameSource {
            override suspend fun capture(width: Int, height: Int): Bitmap? { captures++; return null }
            override fun release() = Unit
        }
        val controller = AmbientFrameController(AmbientPresentation()) { testScheduler.currentTime }
        val job = launch { controller.run(source, 1920, 1080) { policy.copy(active = active) } }
        advanceTimeBy(1000); runCurrent()
        assertEquals(0, captures)
        active = true
        Snapshot.sendApplyNotifications()
        advanceTimeBy(350); runCurrent()
        assertEquals(3, captures)
        advanceTimeBy(1000); runCurrent()
        assertEquals(3, captures)
        job.cancelAndJoin()
    }
    @Test fun pausedAndHiddenSamplerHasNoPeriodicWakeupsAndRefreshWakesIt() = runTest {
        var current by mutableStateOf(policy.copy(playing = false))
        var policyReads = 0
        var samples = 0
        val paused = launch {
            awaitAmbientSamplePolicy(lastRefresh = 0) { policyReads++; current }
            samples++
        }
        runCurrent()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(0, samples)
        assertEquals(1, policyReads)
        current = current.copy(refreshGeneration = 1)
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(1, samples)
        paused.join()

        current = current.copy(active = false, playing = true)
        Snapshot.sendApplyNotifications()
        val hidden = launch {
            awaitAmbientSamplePolicy(lastRefresh = 1) { current }
            samples++
        }
        runCurrent()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, samples)
        current = current.copy(active = true)
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(2, samples)
        hidden.join()
    }

}
