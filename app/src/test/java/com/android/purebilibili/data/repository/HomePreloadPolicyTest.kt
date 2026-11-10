package com.android.purebilibili.data.repository

import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class HomePreloadPolicyTest {

    @Test
    fun preloadKeepsSourceNoticeWithThePageAndRejectsChangedFeedSettings() = runTest {
        var session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<HomeFeedPage> { session }
        val page = HomeFeedPage(listOf(VideoItem(bvid = "web")), setOf(HomeFeedSource.WEB), "手机端不可用")
        state.start(backgroundScope) { Result.success(page) }
        runCurrent()
        assertEquals(page, state.consumeOrAwait(session)?.getOrThrow())

        for (changed in listOf(
            session.copy(feedApiType = SettingsManager.FeedApiType.MERGED),
            session.copy(refreshCount = session.refreshCount + 1),
            session.copy(anonymousWeb = true),
        )) {
            val cachedSession = session
            val cached = HomePreloadState<HomeFeedPage> { session }
            cached.start(backgroundScope) { Result.success(page) }
            runCurrent()
            session = changed
            assertFalse(cached.isReady())
            assertNull(cached.consumeOrAwait(session))
            assertFailsWith<CancellationException> { cached.consumeOrAwait(cachedSession) }
        }
    }

    @Test
    fun startsHomePreloadOnlyWhenNoCacheAndNoTaskExist() {
        assertTrue(
            shouldStartHomePreload(
                hasPreloadedData = false,
                hasActivePreloadTask = false
            )
        )
        assertFalse(
            shouldStartHomePreload(
                hasPreloadedData = true,
                hasActivePreloadTask = false
            )
        )
        assertFalse(
            shouldStartHomePreload(
                hasPreloadedData = false,
                hasActivePreloadTask = true
            )
        )
    }

    @Test
    fun primesBuvidOnlyForMobileFeedApi() {
        assertFalse(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.WEB))
        assertTrue(shouldPrimeBuvidForHomePreload(SettingsManager.FeedApiType.MOBILE))
    }


    @Test
    fun reusesInFlightPreloadOnlyForInitialHomeRequest() {
        assertTrue(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 1,
                isPreloading = true,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = false,
                hasPreloadedData = false
            )
        )
        assertFalse(
            shouldReuseInFlightPreloadForHomeRequest(
                idx = 0,
                isPreloading = true,
                hasPreloadedData = true
            )
        )
    }

    @Test
    fun reportsHomeDataReadyAfterPreloadCompletesEvenIfCacheWasConsumed() {
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = true,
                hasPreloadedData = false
            )
        )
        assertTrue(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = true
            )
        )
        assertFalse(
            shouldReportHomeDataReadyForSplash(
                hasCompletedPreload = false,
                hasPreloadedData = false
            )
        )
    }

    @Test
    fun completedPreloadIsConsumedOnceAndNotRestartedForTheSameSession() = runTest {
        val session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<List<VideoItem>> { session }
        val videos = listOf(VideoItem(bvid = "A"))
        var loads = 0
        assertTrue(state.start(backgroundScope) {
            loads++
            Result.success(videos)
        })
        runCurrent()

        assertTrue(state.isReady())
        assertEquals(videos, state.consumeOrAwait(session)?.getOrThrow())
        assertNull(state.consumeOrAwait(session))
        assertTrue(state.isReady())
        assertFalse(state.start(backgroundScope) {
            loads++
            Result.success(videos)
        })
        assertEquals(1, loads)
    }

    @Test
    fun concurrentRequestsShareOnePreloadButOnlyOneConsumesItsResult() = runTest {
        val session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<List<VideoItem>> { session }
        val gate = CompletableDeferred<Unit>()
        val videos = listOf(VideoItem(bvid = "A"))
        var loads = 0
        assertTrue(state.start(backgroundScope) {
            loads++
            gate.await()
            Result.success(videos)
        })
        assertFalse(state.start(backgroundScope) { error("Duplicate preload") })
        val first = async { state.consumeOrAwait(session) }
        val second = async { state.consumeOrAwait(session) }
        runCurrent()

        assertEquals(1, loads)
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        gate.complete(Unit)
        runCurrent()

        val consumed = listOfNotNull(first.await(), second.await())
        assertEquals(1, consumed.size)
        assertEquals(videos, consumed.single().getOrThrow())
        assertNull(state.consumeOrAwait(session))
    }

    @Test
    fun completedCacheAndReadinessAreInvalidatedBySwitchLogoutAndReturningGeneration() = runTest {
        val first = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        var session = first
        val state = HomePreloadState<List<VideoItem>> { session }
        state.start(backgroundScope) { Result.success(listOf(VideoItem(bvid = "old A"))) }
        runCurrent()
        assertTrue(state.isReady())

        session = HomePreloadSession(AccountSessionIdentity(202L, 2L))
        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
        state.start(backgroundScope) { Result.success(listOf(VideoItem(bvid = "B"))) }
        runCurrent()
        assertTrue(state.isReady())

        session = HomePreloadSession(AccountSessionIdentity(null, 3L))
        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
        session = HomePreloadSession(AccountSessionIdentity(101L, 4L))
        assertNull(state.consumeOrAwait(session))
        assertFailsWith<CancellationException> { state.consumeOrAwait(first) }
    }

    @Test
    fun returningToTheSameAccountCannotPublishItsEarlierInFlightGeneration() = runTest {
        val first = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        var session = first
        val state = HomePreloadState<List<VideoItem>> { session }
        val oldGate = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        state.start(backgroundScope) {
            withContext(NonCancellable) { oldGate.await() }
            Result.success(listOf(VideoItem(bvid = "old A")))
        }
        val oldRequest = async { state.consumeOrAwait(first) }
        runCurrent()

        session = HomePreloadSession(AccountSessionIdentity(202L, 2L))
        assertFalse(state.isReady())
        session = HomePreloadSession(AccountSessionIdentity(101L, 3L))
        val returned = session
        assertTrue(state.start(backgroundScope) {
            newGate.await()
            Result.success(listOf(VideoItem(bvid = "new A")))
        })
        oldGate.complete(Unit)
        runCurrent()

        assertFailsWith<CancellationException> { oldRequest.await() }
        assertFalse(state.isReady())
        assertFalse(state.start(backgroundScope) { error("Current preload was lost") })
        newGate.complete(Unit)
        runCurrent()
        assertEquals("new A", state.consumeOrAwait(returned)?.getOrThrow()?.single()?.bvid)
    }

    @Test
    fun oldPreloadCleanupCannotCompleteOrClearTheReplacementTask() = runTest {
        var session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<List<VideoItem>> { session }
        val cleanup = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val newGate = CompletableDeferred<Unit>()
        state.start(backgroundScope) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    cleanup.await()
                    oldFinished.complete(Unit)
                }
            }
        }
        runCurrent()

        session = HomePreloadSession(AccountSessionIdentity(202L, 2L))
        assertTrue(state.start(backgroundScope) {
            newGate.await()
            Result.success(listOf(VideoItem(bvid = "B")))
        })
        runCurrent()
        cleanup.complete(Unit)
        runCurrent()

        assertTrue(oldFinished.isCompleted)
        assertFalse(state.isReady())
        assertFalse(state.start(backgroundScope) { error("Replacement task was cleared") })
        newGate.complete(Unit)
        runCurrent()
        assertTrue(state.isReady())
        assertEquals("B", state.consumeOrAwait(session)?.getOrThrow()?.single()?.bvid)
    }

    @Test
    fun accountChangeAfterDeferredCompletionStillRejectsTheWaitingRequest() = runTest {
        val first = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        var session = first
        val state = HomePreloadState<List<VideoItem>> { session }
        val gate = CompletableDeferred<Unit>()
        val preloadScope = CoroutineScope(
            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)
        )
        state.start(preloadScope) {
            gate.await()
            Result.success(listOf(VideoItem(bvid = "A")))
        }
        val waiting = async { state.consumeOrAwait(first) }
        runCurrent()

        gate.complete(Unit)
        session = HomePreloadSession(AccountSessionIdentity(202L, 2L))
        runCurrent()

        assertFailsWith<CancellationException> { waiting.await() }
        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
    }

    @Test
    fun credentialChangesInvalidateTheCacheBeforeIdentityPublication() = runTest {
        val identity = AccountSessionIdentity(101L, 1L)
        var session = HomePreloadSession(identity, sessData = "session A", accessToken = "token A")
        val original = session
        val state = HomePreloadState<List<VideoItem>> { session }
        state.start(backgroundScope) { Result.success(listOf(VideoItem(bvid = "A"))) }
        runCurrent()

        session = session.copy(sessData = "session B")
        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
        assertFailsWith<CancellationException> { state.consumeOrAwait(original) }
        session = session.copy(cachedMid = 202L)
        assertFalse(state.start(backgroundScope) { error("Inconsistent identity must not preload") })
        assertFailsWith<CancellationException> { state.captureSession() }
    }

    @Test
    fun cancellationIsNeverPublishedAsACompletedPreload() = runTest {
        val session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<List<VideoItem>> { session }
        state.start(backgroundScope) {
            Result.failure(CancellationException("Cancelled network request"))
        }
        runCurrent()

        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
        assertTrue(state.start(backgroundScope) {
            Result.success(listOf(VideoItem(bvid = "retry")))
        })
        runCurrent()
        assertEquals("retry", state.consumeOrAwait(session)?.getOrThrow()?.single()?.bvid)
    }

    @Test
    fun logoutBeforePreloadCompletionPreventsPublicationWithoutAnyNewRequest() = runTest {
        var session = HomePreloadSession(AccountSessionIdentity(101L, 1L))
        val state = HomePreloadState<List<VideoItem>> { session }
        val gate = CompletableDeferred<Unit>()
        state.start(backgroundScope) {
            gate.await()
            Result.success(listOf(VideoItem(bvid = "A")))
        }
        runCurrent()

        session = HomePreloadSession(AccountSessionIdentity(null, 2L))
        gate.complete(Unit)
        runCurrent()

        assertFalse(state.isReady())
        assertNull(state.consumeOrAwait(session))
    }
}
