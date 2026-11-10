package com.android.purebilibili.core.plugin.js

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiliPaiJsModuleResultCacheTest {

    private fun newCache(ttlClock: () -> Long = { System.currentTimeMillis() }): BiliPaiJsModuleResultCache {
        val dir = File.createTempFile("bilipai_js_cache_test", "").let { temp ->
            temp.delete()
            temp.mkdir()
            temp
        }
        dir.deleteOnExit()
        return BiliPaiJsModuleResultCache(cacheDir = dir, clock = ttlClock)
    }

    @Test
    fun writeThenReadReturnsPayloadWithinTtl() {
        val cache = newCache()

        cache.write(
            pluginId = "tv.live",
            moduleId = "channels",
            paramsJson = """{"category":"cctv"}""",
            cacheDurationSeconds = 600L,
            payload = """[{"id":"cctv-1","title":"CCTV1"}]"""
        )

        assertEquals(
            """[{"id":"cctv-1","title":"CCTV1"}]""",
            cache.read(
                pluginId = "tv.live",
                moduleId = "channels",
                paramsJson = """{"category":"cctv"}""",
                cacheDurationSeconds = 600L
            )
        )
    }

    @Test
    fun differentParamsGetSeparateEntries() {
        val cache = newCache()

        cache.write("tv.live", "channels", """{"page":1}""", 600L, """[{"id":"p1"}]""")
        cache.write("tv.live", "channels", """{"page":2}""", 600L, """[{"id":"p2"}]""")

        assertEquals("""[{"id":"p1"}]""", cache.read("tv.live", "channels", """{"page":1}""", 600L))
        assertEquals("""[{"id":"p2"}]""", cache.read("tv.live", "channels", """{"page":2}""", 600L))
    }

    @Test
    fun readReturnsNullAfterTtlExpires() {
        var nowMillis = 1_000_000L
        val cache = newCache(ttlClock = { nowMillis })

        cache.write("tv.live", "channels", "{}", 60L, """[{"id":"a"}]""")
        nowMillis += 61_000L

        assertNull(cache.read("tv.live", "channels", "{}", 60L))
    }

    @Test
    fun nonPositiveCacheDurationNeverCaches() {
        val cache = newCache()

        cache.write("tv.live", "channels", "{}", 0L, """[{"id":"a"}]""")

        assertNull(cache.read("tv.live", "channels", "{}", 0L))
    }

    @Test
    fun clearPluginRemovesOnlyThatPluginEntries() {
        val cache = newCache()
        cache.write("tv.live", "channels", "{}", 600L, """[{"id":"live"}]""")
        cache.write("dev.other", "main", "{}", 600L, """[{"id":"other"}]""")

        cache.clearPlugin("tv.live")

        assertNull(cache.read("tv.live", "channels", "{}", 600L))
        assertEquals("""[{"id":"other"}]""", cache.read("dev.other", "main", "{}", 600L))
    }

    @Test
    fun corruptedEntryIsDroppedInsteadOfFailing() {
        val cache = newCache()
        cache.write("tv.live", "channels", "{}", 600L, """[{"id":"a"}]""")
        val pluginDir = cache.let { File(it.cacheDirForTest(), "tv.live") }
        val entryFiles = pluginDir.listFiles { file -> file.isFile } ?: emptyArray()
        assertTrue(entryFiles.isNotEmpty())
        entryFiles.forEach { file -> file.writeText("not-json", Charsets.UTF_8) }

        assertNull(cache.read("tv.live", "channels", "{}", 600L))
    }

    @Test
    fun moduleIdMismatchInsideEntryInvalidatesCache() {
        val cache = newCache()
        cache.write("tv.live", "channels", "{}", 600L, """[{"id":"a"}]""")

        // 同一 paramsJson 在不同模块下 key 不同，这里验证跨模块读取不串数据。
        assertNotEquals(
            cache.read("tv.live", "channels", "{}", 600L),
            cache.read("tv.live", "playback", "{}", 600L)
        )
    }
}

/** 测试辅助：暴露内部缓存根目录，用于构造损坏文件场景。 */
private fun BiliPaiJsModuleResultCache.cacheDirForTest(): File {
    return javaClass.getDeclaredField("cacheDir").let { field ->
        field.isAccessible = true
        field.get(this) as File
    }
}
