// 文件路径: test/.../DanmakuCloudRuleSyncPolicyTest.kt
package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.data.repository.DanmakuCloudFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuCloudRuleSyncPolicyTest {

    @Test
    fun `crc32 matches standard check value`() {
        // CRC32("123456789") 的标准校验值
        assertEquals("cbf43926", DanmakuCloudRuleSyncPolicy.crc32Hex("123456789"))
    }

    @Test
    fun `cloud keyword rule maps to plain local rule`() {
        assertEquals(
            "剧透",
            DanmakuCloudRuleSyncPolicy.cloudRuleToLocalRule(
                DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_KEYWORD,
                "剧透"
            )
        )
    }

    @Test
    fun `cloud regex rule maps to prefixed local rule`() {
        assertEquals(
            "regex:第\\d+集",
            DanmakuCloudRuleSyncPolicy.cloudRuleToLocalRule(
                DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_REGEX,
                "第\\d+集"
            )
        )
    }

    @Test
    fun `cloud uid rule maps to prefixed local rule`() {
        assertEquals(
            "uid:abc123",
            DanmakuCloudRuleSyncPolicy.cloudRuleToLocalRule(
                DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_UID,
                "abc123"
            )
        )
    }

    @Test
    fun `cloud rule with empty filter maps to null`() {
        assertEquals(
            null,
            DanmakuCloudRuleSyncPolicy.cloudRuleToLocalRule(
                DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_KEYWORD,
                "  "
            )
        )
    }

    @Test
    fun `local regex rule strips prefix and slash delimiters`() {
        assertEquals(
            "第\\d+集",
            DanmakuCloudRuleSyncPolicy.localRegexRuleToCloudFilter("regex:第\\d+集")
        )
        assertEquals(
            "哈{3,}",
            DanmakuCloudRuleSyncPolicy.localRegexRuleToCloudFilter("/哈{3,}/")
        )
    }

    @Test
    fun `local uid digits convert to crc32 and hex passes through`() {
        val crc = DanmakuCloudRuleSyncPolicy.crc32Hex("12345")
        assertEquals(crc, DanmakuCloudRuleSyncPolicy.localUidRuleToCloudFilter("uid:12345"))
        assertEquals("abc123", DanmakuCloudRuleSyncPolicy.localUidRuleToCloudFilter("hash:abc123"))
        assertFalse(crc == "12345")
    }

    @Test
    fun `only newly added rules are uploaded`() {
        val baseline = DanmakuBlockRuleSections(
            keywordRules = listOf("旧关键词"),
            regexRules = listOf("regex:旧\\d+"),
            userHashRules = listOf("uid:abc123")
        )
        val current = DanmakuBlockRuleSections(
            keywordRules = listOf("旧关键词", "新关键词"),
            regexRules = listOf("regex:旧\\d+"),
            userHashRules = listOf("uid:abc123", "uid:12345")
        )
        val adds = DanmakuCloudRuleSyncPolicy.resolveCloudRuleAdds(baseline, current)
        assertEquals(2, adds.size)
        assertTrue(adds.any {
            it.type == DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_KEYWORD && it.filter == "新关键词"
        })
        assertTrue(adds.any {
            it.type == DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_UID &&
                it.filter == DanmakuCloudRuleSyncPolicy.crc32Hex("12345")
        })
    }

    @Test
    fun `cloud rules missing from local sections are deleted`() {
        val cloudRules = listOf(
            DanmakuCloudFilterRule(id = 1, type = DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_KEYWORD, filter = "保留"),
            DanmakuCloudFilterRule(id = 2, type = DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_KEYWORD, filter = "已删除"),
            DanmakuCloudFilterRule(id = 3, type = DanmakuCloudRuleSyncPolicy.CLOUD_TYPE_UID, filter = "abc123")
        )
        val current = DanmakuBlockRuleSections(
            keywordRules = listOf("保留"),
            regexRules = emptyList(),
            userHashRules = listOf("uid:abc123")
        )
        val deletes = DanmakuCloudRuleSyncPolicy.resolveCloudRuleDeletes(cloudRules, current)
        assertEquals(listOf(2L), deletes)
    }
}
