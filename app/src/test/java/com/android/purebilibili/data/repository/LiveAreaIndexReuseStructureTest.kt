package com.android.purebilibili.data.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 直播分区索引（area list）复用的结构约束：
 * - 并发去重：多个调用共享同一个 in-flight 请求，不重复打接口；
 * - 短 TTL：缓存有过期时间，过期后重新请求；
 * - 失败不缓存：只有成功结果写缓存，失败后下次仍会重新请求；
 * - LiveListScreen 首次仍需数据：首屏 areaList 为空时依然会拉取。
 */
class LiveAreaIndexReuseStructureTest {

    @Test
    fun `area index reuses a single in-flight request with short ttl`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/data/repository/LiveRepository.kt")

        assertTrue(source.contains("areaIndexInFlight"))
        assertTrue(source.contains("AREA_INDEX_TTL_MS"))
        // 并发调用复用同一个在飞请求，而不是各打一次接口。
        assertTrue(source.contains("val inFlight = areaIndexInFlight"))
        assertTrue(source.contains("AppScope.ioScope.async"))
    }

    @Test
    fun `area index caches only successful results and never caches failures`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/data/repository/LiveRepository.kt")

        // 只有 result.getOrNull()（成功）才写缓存；失败时缓存保持不变。
        assertTrue(source.contains("result.getOrNull()?.let { areas ->"))
        assertTrue(source.contains("areaIndexCache = areas"))
        // 无论成功失败都清 in-flight，避免失败后 in-flight 永久卡住。
        assertTrue(source.contains("areaIndexInFlight = null"))
    }

    @Test
    fun `live list screen still requires area data on first load`() {
        val screen = loadSource("app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt")

        // 首屏 areaList 为空时仍然要走一次 getLiveAreaIndex()，不能因为缓存而跳过首次数据。
        assertTrue(screen.contains("if (_uiState.value.areaList.isEmpty())"))
        assertTrue(screen.contains("LiveRepository.getLiveAreaIndex()"))
        // 不再直接裸调 web area list 接口。
        assertFalse(screen.contains("NetworkModule.api.getLiveAreaList()"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app/$path"),
            File("../$path"),
            File("../core-data/$path"),
            File("core-data/$path"),
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
