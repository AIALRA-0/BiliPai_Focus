package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodayWatchCandidatePoolPolicyTest {

    @Test
    fun `expanded candidates append valid unique videos without replacing local order`() {
        val local = listOf(
            VideoItem(bvid = "a", title = "A"),
            VideoItem(bvid = "b", title = "B")
        )
        val expanded = listOf(
            VideoItem(bvid = "b", title = "B duplicate"),
            VideoItem(bvid = "c", title = "C"),
            VideoItem(bvid = "", title = "invalid")
        )

        val merged = mergeTodayWatchCandidates(local, expanded)

        assertEquals(listOf("a", "b", "c"), merged.map { it.bvid })
    }

    @Test
    fun `expanded cache requires same base candidates and unexpired ttl`() {
        val cache = TodayWatchExpandedCandidateCache(
            baseSignature = "a|b",
            candidates = listOf(VideoItem(bvid = "c", title = "C")),
            loadedAtMillis = 1_000L
        )

        assertTrue(canReuseTodayWatchExpandedCache(cache, "a|b", 2_000L))
        assertFalse(canReuseTodayWatchExpandedCache(cache, "changed", 2_000L))
        assertFalse(
            canReuseTodayWatchExpandedCache(
                cache,
                "a|b",
                1_000L + TODAY_WATCH_EXPANDED_CACHE_TTL_MS + 1L
            )
        )
    }

    @Test
    fun `only latest generation may replace recommendation result`() {
        assertTrue(shouldApplyTodayWatchBuildResult(resultGeneration = 4L, currentGeneration = 4L))
        assertFalse(shouldApplyTodayWatchBuildResult(resultGeneration = 3L, currentGeneration = 4L))
    }
}
