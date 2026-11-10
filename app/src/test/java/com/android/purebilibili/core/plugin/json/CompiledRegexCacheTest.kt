package com.android.purebilibili.core.plugin.json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompiledRegexCacheTest {
    @Test fun repeatedAndMalformedRulesCompileOnlyOnce() {
        var compilations = 0
        val cache = CompiledRegexCache { pattern -> compilations++; Regex(pattern) }
        repeat(500) {
            assertTrue(cache.get("广告|推广")!!.containsMatchIn("这是推广内容"))
            assertNull(cache.get("["))
        }
        assertEquals(2, compilations)
    }

    @Test fun editedPatternsTakeEffectAndOldPatternsAreEvicted() {
        var compilations = 0
        val cache = CompiledRegexCache(capacity = 2) { pattern -> compilations++; Regex(pattern) }
        assertTrue(cache.get("广告")!!.containsMatchIn("广告"))
        assertFalse(cache.get("游戏")!!.containsMatchIn("广告"))
        cache.get("广告") // Most recently used rule survives.
        cache.get("音乐")
        cache.get("广告")
        assertEquals(3, compilations)
        cache.get("游戏")
        assertEquals(4, compilations)
    }
}
