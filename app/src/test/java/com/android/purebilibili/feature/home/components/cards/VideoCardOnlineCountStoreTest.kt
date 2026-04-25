package com.android.purebilibili.feature.home.components.cards

import kotlinx.coroutines.CompletableDeferred
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
}
