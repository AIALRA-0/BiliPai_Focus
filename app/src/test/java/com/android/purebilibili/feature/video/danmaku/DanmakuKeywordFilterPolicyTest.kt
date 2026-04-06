package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuKeywordFilterPolicyTest {

    @Test
    fun parseDanmakuBlockRules_splitsByLineAndComma() {
        val rules = parseDanmakuBlockRules(
            """
            剧透
            前方高能,哈哈
            regex:\d{2}年
            """.trimIndent()
        )

        assertEquals(
            listOf("剧透", "前方高能", "哈哈", "regex:\\d{2}年"),
            rules
        )
    }

    @Test
    fun matchesDanmakuBlockRule_supportsPlainKeywordIgnoreCase() {
        assertTrue(matchesDanmakuBlockRule(content = "这段有剧透注意", rule = "剧透"))
        assertTrue(matchesDanmakuBlockRule(content = "Spoiler alert", rule = "spoiler"))
    }

    @Test
    fun matchesDanmakuBlockRule_supportsRegexPrefix() {
        assertTrue(matchesDanmakuBlockRule(content = "2026年新番", rule = "regex:\\d{4}年"))
        assertTrue(matchesDanmakuBlockRule(content = "第12集封神", rule = "re:第\\d+集"))
    }

    @Test
    fun matchesDanmakuBlockRule_supportsSlashRegex() {
        assertTrue(matchesDanmakuBlockRule(content = "哈哈哈哈", rule = "/哈{3,}/"))
    }

    @Test
    fun matchesDanmakuBlockRule_supportsUserHashRules() {
        assertTrue(
            matchesDanmakuBlockRule(
                content = "普通弹幕",
                rule = "uid:abc123",
                userHash = "abc123"
            )
        )
        assertTrue(
            matchesDanmakuBlockRule(
                content = "普通弹幕",
                rule = "hash:xyz",
                userHash = "XYZ"
            )
        )
    }

    @Test
    fun matchesDanmakuBlockRule_invalidRegexFallsBackToFalse() {
        assertFalse(matchesDanmakuBlockRule(content = "abc", rule = "regex:[a-"))
        assertFalse(matchesDanmakuBlockRule(content = "abc", rule = "/[a-/"))
    }

    @Test
    fun shouldBlockDanmakuByRules_returnsTrueWhenAnyRuleMatches() {
        val blocked = shouldBlockDanmakuByRules(
            content = "第24集剧透",
            rules = listOf("前方高能", "re:第\\d+集", "哈哈")
        )

        assertTrue(blocked)
    }

    @Test
    fun shouldBlockDanmakuByRules_returnsFalseWhenNoRulesMatch() {
        val blocked = shouldBlockDanmakuByRules(
            content = "纯路人弹幕",
            rules = listOf("剧透", "regex:第\\d+集")
        )

        assertFalse(blocked)
    }

    @Test
    fun partitionDanmakuBlockRules_groupsRulesForManagerTabs() {
        val grouped = partitionDanmakuBlockRules(
            listOf("剧透", "regex:第\\d+集", "uid:abc123", "hash:XYZ", "哈哈")
        )

        assertEquals(listOf("剧透", "哈哈"), grouped.keywordRules)
        assertEquals(listOf("regex:第\\d+集"), grouped.regexRules)
        assertEquals(listOf("uid:abc123", "hash:XYZ"), grouped.userHashRules)
    }

    @Test
    fun mergeDanmakuBlockRuleSections_normalizesAndDeduplicatesRules() {
        val merged = mergeDanmakuBlockRuleSections(
            keywordRules = listOf("剧透", "  哈哈 "),
            regexRules = listOf("regex:第\\d+集", "regex:第\\d+集"),
            userHashRules = listOf("abc123", "uid:xyz")
        )

        assertEquals(
            listOf("剧透", "哈哈", "regex:第\\d+集", "uid:abc123", "uid:xyz"),
            merged
        )
    }

    @Test
    fun appendDanmakuKeywordBlockRule_appendsUniqueKeywordToRawRules() {
        val updated = appendDanmakuKeywordBlockRule(
            rawRules = "剧透\nregex:第\\d+集",
            keyword = "哈哈"
        )

        assertEquals(
            "剧透\nregex:第\\d+集\n哈哈",
            updated
        )
    }

    @Test
    fun appendDanmakuUserHashBlockRule_normalizesPrefixAndDeduplicates() {
        val updated = appendDanmakuUserHashBlockRule(
            rawRules = "剧透\nuid:abc123",
            userHash = "hash:abc123"
        )

        assertEquals(
            "剧透\nuid:abc123",
            updated
        )
    }
}
