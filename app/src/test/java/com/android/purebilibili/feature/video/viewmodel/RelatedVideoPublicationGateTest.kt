package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RelatedVideoPublicationGateTest {
    @Test
    fun waitsForEntryAndRejectsAnObsoleteResult() = runTest {
        val allowed = MutableStateFlow(false)
        var current = true
        val result = async { awaitRelatedVideoPublication(allowed) { current } }
        runCurrent()
        assertFalse(result.isCompleted)
        current = false
        allowed.value = true
        assertFalse(result.await())
    }

    @Test
    fun currentResultResumesAndCancellationDoesNotPublish() = runTest {
        val allowed = MutableStateFlow(false)
        val cancelled = async { awaitRelatedVideoPublication(allowed) { true } }
        runCurrent()
        cancelled.cancelAndJoin()
        val current = async { awaitRelatedVideoPublication(allowed) { true } }
        allowed.value = true
        assertTrue(current.await())
        assertTrue(cancelled.isCancelled)
    }
}
