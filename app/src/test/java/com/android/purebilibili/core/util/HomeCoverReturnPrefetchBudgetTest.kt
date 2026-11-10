package com.android.purebilibili.core.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeCoverReturnPrefetchBudgetTest {
    private val entries = (0 until 8).map {
        HomeCoverReturnPrefetchEntry("BV_$it", "url_$it", "key_$it", 320, 180)
    }

    @Test
    fun loadsSourceFirstAndBoundsOutstandingWork() = runTest {
        val releases = entries.associate { it.bvid to CompletableDeferred<Unit>() }
        val started = mutableListOf<String>()
        var running = 0
        var peak = 0
        val job = launch {
            prefetchHomeCoversWithBudget(entries) { entry ->
                started += entry.bvid
                running++
                peak = maxOf(peak, running)
                try { releases.getValue(entry.bvid).await() } finally { running-- }
            }
        }
        runCurrent()
        assertEquals(entries.take(2).map { it.bvid }, started)
        releases.getValue(entries.first().bvid).complete(Unit)
        runCurrent()
        assertEquals(entries.take(3).map { it.bvid }, started)
        releases.values.forEach { it.complete(Unit) }
        job.join()
        assertEquals(entries.map { it.bvid }, started)
        assertEquals(HOME_COVER_RETURN_PREFETCH_PARALLELISM, peak)
        assertEquals(0, running)
    }

    @Test
    fun supersedingReturnCancelsActiveAndQueuedCovers() = runTest {
        val started = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val job = launch {
            prefetchHomeCoversWithBudget(entries) { entry ->
                started += entry.bvid
                try { awaitCancellation() } finally { cancelled += entry.bvid }
            }
        }
        runCurrent()
        job.cancelAndJoin()
        runCurrent()
        assertTrue(job.isCancelled)
        assertEquals(entries.take(2).map { it.bvid }, started)
        assertEquals(started.toSet(), cancelled.toSet())
    }
}
