package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardCoverColorConcurrencyPolicyTest {

    @Test
    fun `extraction allows more than one in flight`() {
        // 回归护栏：单 Mutex 会把「磁盘读 + 解码 + Palette 聚类」完全串行化，
        // 回前台后几十张卡片的队列要几百毫秒才排空，掉帧窗口被整体拉长。
        assertTrue(COVER_COLOR_EXTRACTION_PARALLELISM >= 2)
        // 但也不能放开太多：Palette 聚类是 CPU 密集的，并行度过高会挤占渲染预算。
        assertTrue(COVER_COLOR_EXTRACTION_PARALLELISM <= 4)
    }

    @Test
    fun `cache hits are resolved before taking an extraction permit`() {
        val source = loadStoreSource()
        val extractBody = source
            .substringAfter("suspend fun extractColor(")
            .substringBefore("/** 调用方负责持有")

        // 命中缓存必须在申请许可之前返回，否则缓存命中的卡片仍要排在慢提取后面。
        val firstCacheRead = extractBody.indexOf("getCachedColor(cacheKey)")
        val permitAcquire = extractBody.indexOf("withPermit")
        assertTrue(firstCacheRead >= 0, "extractColor must recheck the colour cache")
        assertTrue(permitAcquire >= 0, "extractColor must bound concurrency with a permit")
        assertTrue(
            firstCacheRead < permitAcquire,
            "cache hits must return before acquiring an extraction permit"
        )
        assertFalse(extractBody.contains("Mutex("))
    }

    private fun loadStoreSource(): String {
        val path = "app/src/main/java/com/android/purebilibili/feature/home/components/cards/" +
            "VideoCardCoverColorStore.kt"
        val file = listOf(File(path), File(path.removePrefix("app/"))).firstOrNull { it.exists() }
        require(file != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return file.readText()
    }
}
