package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.*
import org.junit.Test

class DanmakuUidParityPolicyTest {
    @Test fun numericUidMatchesItsCrc32HashLocallyAndRemotely() {
        val hash = DanmakuCloudRuleSyncPolicy.crc32Hex("12345")
        assertTrue(matchesDanmakuBlockRule("hello", "uid:12345", hash))
        assertEquals(hash, DanmakuCloudRuleSyncPolicy.localUidRuleToCloudFilter("uid:12345"))
        assertFalse(matchesDanmakuBlockRule("hello", "uid:12345", "different"))
    }

    @Test fun numericOnlyHashNeverGetsRehashed() {
        assertEquals("hash:12345678", DanmakuCloudRuleSyncPolicy.cloudRuleToLocalRule(2,"12345678"))
        assertEquals("12345678", DanmakuCloudRuleSyncPolicy.localUidRuleToCloudFilter("hash:12345678"))
        assertTrue(matchesDanmakuBlockRule("hello", "hash:12345678", "12345678"))
        assertEquals("hash:12345678", appendDanmakuUserHashBlockRule("", "12345678"))
    }
}
