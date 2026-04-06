package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuClickPolicyTest {

    @Test
    fun resolveDanmakuClickUserHash_preservesNonNumericHash() {
        assertEquals(
            "abcXYZ",
            resolveDanmakuClickUserHash("  abcXYZ  ")
        )
    }

    @Test
    fun resolveDanmakuClickIsSelf_onlyMatchesNumericMid() {
        assertTrue(resolveDanmakuClickIsSelf(userHash = "12345", currentMid = 12345L))
        assertFalse(resolveDanmakuClickIsSelf(userHash = "abc123", currentMid = 123L))
    }
}
