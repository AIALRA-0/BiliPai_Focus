package com.android.purebilibili.feature.home.components.cards

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VideoCardOnlineCountStoreTest {

    @Test
    fun cancellingHiddenCardRefreshPreservesTextAndAllowsNextRefresh() = runTest {
        var nowMs = 0L
        var calls = 0
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { _, _ ->
                calls++
                if (calls == 2) awaitCancellation()
                if (calls == 1) "100" else "200"
            },
            nowMs = { nowMs },
            ttlMs = 10L,
        )
        store.refreshIfNeeded("BV_A", 1L)
        nowMs = 20L
        val refresh = launch { store.refreshIfNeeded("BV_A", 1L) }
        runCurrent()
        refresh.cancelAndJoin()
        assertEquals("100", store.observe("BV_A", 1L).value)
        store.refreshIfNeeded("BV_A", 1L)
        assertEquals(3, calls)
        assertEquals("200", store.observe("BV_A", 1L).value)
    }

    @Test
    fun shouldLoadOnlineCount_onlyWhenEnabledAndVideoIdentityValid() {
        assertTrue(
            shouldLoadVideoCardOnlineCount(
                showOnlineCount = true,
                bvid = "BV1test",
                cid = 123L
            )
        )
        assertFalse(
            shouldLoadVideoCardOnlineCount(
                showOnlineCount = false,
                bvid = "BV1test",
                cid = 123L
            )
        )
        assertFalse(
            shouldLoadVideoCardOnlineCount(
                showOnlineCount = true,
                bvid = "   ",
                cid = 123L
            )
        )
        assertFalse(
            shouldLoadVideoCardOnlineCount(
                showOnlineCount = true,
                bvid = "BV1test",
                cid = 0L
            )
        )
    }

    @Test
    fun resolveOnlineCountText_returnsTrimmedCountOnlyWhenEnabled() {
        assertEquals("9.4万+", resolveVideoCardOnlineCountText(true, " 9.4万+ "))
        assertEquals("", resolveVideoCardOnlineCountText(false, "9.4万+"))
        assertEquals("", resolveVideoCardOnlineCountText(true, "   "))
    }

    @Test
    fun resolveOnlineCountKey_normalizesIdentity() {
        assertEquals("BV1test#456", resolveVideoCardOnlineCountKey(" BV1test ", 456L))
    }

    @Test
    fun refreshIfNeeded_reusesFreshCache() = runTest {
        var nowMs = 1_000L
        var requestCount = 0
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { _, _ ->
                requestCount += 1
                "9.4万+"
            },
            nowMs = { nowMs },
            ttlMs = 60_000L
        )

        store.refreshIfNeeded("BV1cache", 1L)
        store.refreshIfNeeded("BV1cache", 1L)

        assertEquals(1, requestCount)
        assertEquals("9.4万+", store.observe("BV1cache", 1L).value)

        nowMs += 60_001L
        store.refreshIfNeeded("BV1cache", 1L)

        assertEquals(2, requestCount)
    }

    @Test
    fun refreshIfNeeded_deduplicatesInflightRequests() = runTest {
        val gate = CompletableDeferred<Unit>()
        var requestCount = 0
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { _, _ ->
                requestCount += 1
                gate.await()
                "2.1万+"
            }
        )

        val first = async { store.refreshIfNeeded("BV1inflight", 2L) }
        val second = async { store.refreshIfNeeded("BV1inflight", 2L) }

        runCurrent()
        assertEquals(1, requestCount)
        gate.complete(Unit)
        awaitAll(first, second)

        assertEquals(1, requestCount)
        assertEquals("2.1万+", store.observe("BV1inflight", 2L).value)
    }

    @Test
    fun refreshIfNeeded_evictsOldestEntryBeyondCapacityAndRefetches() = runTest {
        var nowMs = 1_000L
        val requestCounts = mutableMapOf<String, Int>()
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { bvid, _ ->
                requestCounts[bvid] = (requestCounts[bvid] ?: 0) + 1
                "1.2万+"
            },
            nowMs = { nowMs },
            ttlMs = 60_000L,
            maxEntries = 2
        )

        store.refreshIfNeeded("BV1a", 1L)
        nowMs += 1
        store.refreshIfNeeded("BV1b", 2L)
        nowMs += 1
        // 第 3 条触发清理，按最旧 fetchedAtMs 淘汰 BV1a。
        store.refreshIfNeeded("BV1c", 3L)

        assertEquals("1.2万+", store.observe("BV1c", 3L).value)
        nowMs += 1
        store.refreshIfNeeded("BV1a", 1L)
        assertEquals(2, requestCounts["BV1a"])
        assertEquals(1, requestCounts["BV1b"])
        assertEquals(1, requestCounts["BV1c"])
    }

    @Test
    fun refreshIfNeeded_sweepsStaleEntriesFirstWhenOverCapacity() = runTest {
        var nowMs = 1_000L
        val requestCounts = mutableMapOf<String, Int>()
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { bvid, _ ->
                requestCounts[bvid] = (requestCounts[bvid] ?: 0) + 1
                "3.4万+"
            },
            nowMs = { nowMs },
            ttlMs = 60_000L,
            maxEntries = 2
        )

        store.refreshIfNeeded("BV1old", 1L)
        nowMs += 61_000L
        store.refreshIfNeeded("BV1mid", 2L)
        nowMs += 61_000L
        // BV1old 与 BV1mid 均已过 TTL，插入第 3 条后应先清过期条目而不是按新旧淘汰。
        store.refreshIfNeeded("BV1new", 3L)

        nowMs += 1
        store.refreshIfNeeded("BV1old", 1L)
        store.refreshIfNeeded("BV1mid", 2L)
        assertEquals(2, requestCounts["BV1old"])
        assertEquals(2, requestCounts["BV1mid"])
        assertEquals(1, requestCounts["BV1new"])
    }

    @Test
    fun observationWithoutSuccessfulRequestsRemainsBounded() {
        val store = VideoCardOnlineCountStore(fetchOnlineCount = { _, _ -> "" }, maxEntries = 2)
        repeat(100) { store.observe("BV$it", it.toLong() + 1) }
        assertEquals(2, store.stateEntryCount)
    }

    @Test
    fun failedAndEmptyRequestsDoNotBypassStateCapacity() = runTest {
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { _, cid ->
                if (cid % 2L == 0L) error("offline") else ""
            },
            maxEntries = 2,
        )
        repeat(100) { store.refreshIfNeeded("BV$it", it.toLong() + 1) }
        assertEquals(2, store.stateEntryCount)
    }

    @Test
    fun composedCardKeepsItsFlowWhileCollectionIsPaused() = runTest {
        val store = VideoCardOnlineCountStore(fetchOnlineCount = { _, _ -> "42" }, maxEntries = 2)
        val pausedCard = store.observe("BVpaused", 1L)
        store.retain("BVpaused", 1L)
        store.retain("BVpaused", 1L)
        store.release("BVpaused", 1L)
        repeat(20) { store.observe("BVother$it", it.toLong() + 2) }
        store.refreshIfNeeded("BVpaused", 1L)
        assertEquals("42", pausedCard.value)
        assertEquals(2, store.stateEntryCount)
        store.release("BVpaused", 1L)
    }

    @Test
    fun cancelledRequestsAlsoTrimUnobservedStates() = runTest {
        val store = VideoCardOnlineCountStore(
            fetchOnlineCount = { _, _ -> awaitCancellation() }, maxEntries = 2,
        )
        repeat(20) { index ->
            val request = launch { store.refreshIfNeeded("BV$index", index.toLong() + 1) }
            runCurrent()
            request.cancelAndJoin()
        }
        assertEquals(2, store.stateEntryCount)
    }
}
