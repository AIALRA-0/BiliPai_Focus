package com.android.purebilibili.core.cache

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayUrlCachePolicyTest {
    private val nowMs = 1_700_000_000_000L
    private val maxAgeMs = 600_000L

    @Test
    fun unsignedUrlsUseTheExistingMaximumAge() {
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, listOf("https://example.com/video")))
    }

    @Test
    fun earliestTrackDeadlineExpiresBeforeTheUrlDoes() {
        val deadlineSec = nowMs / 1000L + 90L
        val urls = listOf("https://example.com/video?deadline=$deadlineSec", "https://example.com/audio?deadline=${deadlineSec + 30L}")
        assertEquals(nowMs + 60_000L, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }

    @Test
    fun pastDeadlinesCannotBeCachedAsFresh() {
        val urls = listOf("https://example.com/video?deadline=${nowMs / 1000L - 1L}")
        assertTrue(resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls) < nowMs)
    }

    @Test
    fun malformedAndOverflowingDeadlinesDoNotCrashLoading() {
        val urls = listOf("bad url", "https://example.com/video?deadline=invalid", "https://example.com/video?deadline=9223372036854775807")
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }

    @Test
    fun serverDeadlineDoesNotExtendTheLocalMaximumAge() {
        val urls = listOf("https://example.com/video?deadline=${nowMs / 1000L + 3600L}")
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }

    /**
     * 双账号会让 `accountScope` 键空间翻倍（同一 bvid 的不同账号缓存必须隔离，不能互用）。
     *
     * 因此：① 容量必须为账号变体留足余量，否则热条目被 LRU 挤掉后重进同一视频必然
     * 重新解析 playurl；② 回退查找与整视频失效不得再整表 `cache.snapshot()` 拷贝，
     * 应走二级索引只遍历该视频的变体。
     *
     * 依赖 `android.util.LruCache` 运行时，无法纯单测；此结构用例锁定上述两条不变量。
     */
    @Test
    fun cacheCapacityAndVideoIndexedLookupSurviveMultiAccountKeySpace() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/cache/PlayUrlCache.kt"),
            File("src/main/java/com/android/purebilibili/core/cache/PlayUrlCache.kt"),
        ).first { it.exists() }.readText()

        // 容量为双账号变体留余量（原为 80）。
        val maxSize = source.substringAfter("private const val MAX_CACHE_SIZE =")
            .substringBefore("\n").trim().toInt()
        assertTrue(maxSize >= 160, "cache capacity must absorb the extra account-scope variants, was $maxSize")

        // 二级索引存在，且被回退查找与整视频失效复用。
        assertTrue(source.contains("private val variantsByVideo"), "video-keyed index must exist")
        assertTrue(source.contains("private fun variantsFor("))
        assertTrue(source.contains("private fun indexKey("))
        assertTrue(source.contains("private fun unindexKey("))

        // 回退查找 / 整视频失效里不得再出现整表拷贝与全表前缀扫描。
        val invalidateBlock = source.substringAfter("fun invalidate(").substringBefore("fun clear()")
        val finderBlock = source.substringAfter("private fun findNewestValidCacheForVideo(")
        assertFalse(
            invalidateBlock.contains("cache.snapshot()"),
            "invalidate must use the index instead of snapshotting the whole cache",
        )
        assertFalse(
            finderBlock.contains("cache.snapshot()"),
            "fallback lookup must use the index instead of snapshotting the whole cache",
        )

        // 索引必须在 clear / put 淘汰 / 失效路径同步维护，避免无界增长。
        assertTrue(source.substringAfter("fun clear()").contains("variantsByVideo.clear()"))
        val putBlock = source.substringAfter("fun put(").substringBefore("fun invalidate(")
        assertTrue(putBlock.contains("indexKey("))
        assertTrue(putBlock.contains("unindexKey("))
    }
}
