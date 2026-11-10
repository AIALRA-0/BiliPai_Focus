package com.android.purebilibili.core.util

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeCoverReturnPrefetchPolicyTest {

    private fun entry(bvid: String, url: String, cacheKey: String) =
        HomeCoverReturnPrefetchEntry(bvid = bvid, url = url, cacheKey = cacheKey, decodeWidthPx = 320, decodeHeightPx = 180)

    @BeforeTest
    fun setUp() {
        HomeCoverReturnPrefetchRegistry.clearForTest()
    }

    // --- resolveHomeCoverReturnPrefetchCandidates ---

    @Test
    fun candidates_putsSourceCardFirst() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_SOURCE", "url_s", "cover_BV_SOURCE_n"),
            entry("BV_B", "url_b", "cover_BV_B_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_SOURCE",
        )

        assertEquals("cover_BV_SOURCE_n", result.first().cacheKey)
        assertEquals(3, result.size)
    }

    @Test
    fun candidates_deduplicatesByCacheKey() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_A", "url_a2", "cover_BV_A_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = null,
        )

        assertEquals(1, result.size)
    }

    @Test
    fun candidates_skipsBlankUrlOrCacheKey() {
        val entries = listOf(
            entry("BV_A", "", "cover_BV_A_n"),
            entry("BV_B", "url_b", ""),
            entry("BV_C", "url_c", "cover_BV_C_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = null,
        )

        assertEquals(1, result.size)
        assertEquals("cover_BV_C_n", result.single().cacheKey)
    }

    @Test
    fun candidates_respectsMaxCount() {
        val entries = (0 until 10).map { entry("BV_$it", "url_$it", "cover_BV_${it}_n") }

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_9",
            maxCount = 3,
        )

        assertEquals(3, result.size)
        // 源卡优先占一个名额
        assertEquals("cover_BV_9_n", result.first().cacheKey)
    }

    @Test
    fun candidates_maxCountZeroReturnsEmpty() {
        val entries = listOf(entry("BV_A", "url_a", "cover_BV_A_n"))

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_A",
            maxCount = 0,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun candidates_emptySourceBvidFallsBackToVisibilityOrder() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_B", "url_b", "cover_BV_B_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "  ",
        )

        assertEquals(2, result.size)
        assertEquals("cover_BV_A_n", result.first().cacheKey)
    }

    // --- HomeCoverReturnPrefetchRegistry ---

    @Test
    fun registry_keepsSnapshotAfterDisposeLikeSemantics() {
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "cover_BV_B_n"))

        // 详情打开时首页 scene dispose，注册表不清空——返回必须仍能取到离开前可视快照。
        assertEquals(2, HomeCoverReturnPrefetchRegistry.snapshot().size)
    }

    @Test
    fun registry_duplicateReportOnlyRefreshesOrder() {
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "cover_BV_B_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))

        assertEquals(2, HomeCoverReturnPrefetchRegistry.snapshot().size)
        // 最新可见的 BV_A 排到最前（LRU 语义）
        assertEquals("cover_BV_A_n", HomeCoverReturnPrefetchRegistry.snapshot().first().cacheKey)
    }

    @Test
    fun registry_skipsBlankEntriesAndCapsSize() {
        repeat(100) { index ->
            HomeCoverReturnPrefetchRegistry.onCardVisible(
                entry("BV_$index", "url_$index", "cover_BV_${index}_n")
            )
        }
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_EMPTY", "", ""))

        assertEquals(64, HomeCoverReturnPrefetchRegistry.snapshot().size)
    }

    @Test
    fun registry_refreshesDecodeSizeWithoutDuplicatingOrMutatingEarlierSnapshot() {
        val first = entry("BV_A", "url_a", "key_a").copy(decodeWidthPx = 320, decodeHeightPx = 180)
        HomeCoverReturnPrefetchRegistry.onCardVisible(first)
        val earlier = HomeCoverReturnPrefetchRegistry.snapshot()
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "key_b"))
        val updated = first.copy(decodeWidthPx = 640, decodeHeightPx = 360)
        HomeCoverReturnPrefetchRegistry.onCardVisible(updated)

        assertEquals(listOf(first), earlier)
        val current = HomeCoverReturnPrefetchRegistry.snapshot()
        assertEquals(2, current.size)
        assertEquals(updated, current.first())
        assertEquals(updated, resolveHomeCoverReturnPrefetchCandidates(current, "BV_A").first())
    }

    @Test
    fun invalidSourceEntryDoesNotDisplaceAValidSourceCover() {
        val valid = entry("BV_A", "url_a", "key_a")
        assertEquals(
            listOf(valid),
            resolveHomeCoverReturnPrefetchCandidates(listOf(valid.copy(url = ""), valid), "BV_A", 1),
        )
    }

    @Test
    fun decodeBudgetKeepsSourceFirstAndBoundsWholeBatch() {
        val entries = (0 until 24).map { index ->
            entry("BV_$index", "url_$index", "key_$index")
                .copy(decodeWidthPx = 1024, decodeHeightPx = 1024)
        }
        val result = resolveHomeCoverReturnPrefetchCandidates(
            entries, "BV_23", maxDecodeBytes = 8L * 1024 * 1024,
        )

        assertEquals(listOf("BV_23", "BV_0"), result.map { it.bvid })
        assertEquals(8L * 1024 * 1024, result.sumOf { it.estimatedDecodeBytes()!! })
    }

    @Test
    fun unknownOrInvalidDimensionsNeverTriggerOriginalSizePreload() {
        val valid = entry("BV_valid", "url", "valid")
        val entries = listOf(
            valid.copy(cacheKey = "unmeasured", decodeWidthPx = 0, decodeHeightPx = 0),
            valid.copy(cacheKey = "negative", decodeWidthPx = -1),
            valid.copy(cacheKey = "partial", decodeHeightPx = 0),
            valid.copy(cacheKey = "overflow", decodeWidthPx = Int.MAX_VALUE, decodeHeightPx = Int.MAX_VALUE),
            valid,
        )

        assertEquals(listOf(valid), resolveHomeCoverReturnPrefetchCandidates(entries, null))
    }

    @Test
    fun unmeasuredRemountDoesNotReplaceEarlierMeasuredRegistration() {
        val measured = entry("BV_A", "url", "key")
        HomeCoverReturnPrefetchRegistry.onCardVisible(measured)
        HomeCoverReturnPrefetchRegistry.onCardVisible(measured.copy(decodeWidthPx = 0, decodeHeightPx = 0))

        assertEquals(listOf(measured), HomeCoverReturnPrefetchRegistry.snapshot())
    }

    @Test
    fun oversizedHistoricalCoverDoesNotBlockSmallerCovers() {
        val oversized = entry("BV_large", "large", "large")
            .copy(decodeWidthPx = 4608, decodeHeightPx = 4608)
        val small = entry("BV_small", "small", "small")

        assertEquals(
            listOf(small),
            resolveHomeCoverReturnPrefetchCandidates(listOf(oversized, small), "BV_large"),
        )
        assertTrue(resolveHomeCoverReturnPrefetchCandidates(listOf(small), null, maxDecodeBytes = 0).isEmpty())
    }
}
