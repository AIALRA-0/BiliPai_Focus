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
    fun parseDanmakuBlockRules_preservesCommaInRegexSyntax() {
        val rules = parseDanmakuBlockRules(
            """
            regex:\d{1,3}秒
            /哈{3,}/
            re:[a,b]
            r=第\d{1,2}集
            剧透, regex:^.{1,5}$
            """.trimIndent()
        )

        assertEquals(
            listOf(
                "regex:\\d{1,3}秒",
                "/哈{3,}/",
                "re:[a,b]",
                "r=第\\d{1,2}集",
                "剧透",
                "regex:^.{1,5}$"
            ),
            rules
        )
    }

    @Test
    fun parseDanmakuBlockRules_preservesCommaWhileTypingRegex() {
        val rules1 = parseDanmakuBlockRules("regex:\\d{1,")
        assertEquals(listOf("regex:\\d{1,"), rules1)

        val rules2 = parseDanmakuBlockRules("/哈{3,")
        assertEquals(listOf("/哈{3,"), rules2)
    }

    @Test
    fun parseDanmakuBlockRules_supportsJsonImportPayload() {
        val rules = parseDanmakuBlockRules(
            """
            {
              "keywords": ["剧透", "前方高能"],
              "regex": ["第\\d+集"],
              "userHashes": ["abc123"]
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("剧透", "前方高能", "regex:第\\d+集", "uid:abc123"),
            rules
        )
    }

    @Test
    fun parseDanmakuBlockRuleImport_supportsTextJsonAndXml() {
        val text = parseDanmakuBlockRuleImport("t=剧透\nr=第\\d+集\nu=abc123")
        assertEquals(listOf("剧透", "regex:第\\d+集", "uid:abc123"), text.rules)

        val json = parseDanmakuBlockRuleImport("""{"regex":["哈{3,}"]}""")
        assertEquals(listOf("regex:哈{3,}"), json.rules)

        val xml = parseDanmakuBlockRuleImport(
            """
            <filters>
              <item enabled="true">t=剧透</item>
              <item enabled="true">r=第\d+集</item>
              <filter enabled="true">u=abc123</filter>
              <item enabled="false">t=已禁用</item>
            </filters>
            """.trimIndent()
        )
        assertEquals(listOf("剧透", "regex:第\\d+集", "uid:abc123"), xml.rules)
        assertEquals(1, xml.skippedDisabledCount)
        assertTrue(xml.canImport)
    }

    @Test
    fun parseDanmakuBlockRuleImport_reportsInvalidRegexAndMalformedFiles() {
        val mixed = parseDanmakuBlockRuleImport("剧透\nr=[a-")
        assertEquals(listOf("剧透"), mixed.rules)
        assertEquals(listOf("r=[a-"), mixed.invalidEntries)

        val malformedXml = parseDanmakuBlockRuleImport("<filters><item>剧透</filters>")
        assertFalse(malformedXml.canImport)
        assertTrue(malformedXml.errorMessage?.startsWith("XML 屏蔽规则格式无效") == true)

        val malformedJson = parseDanmakuBlockRuleImport("{not-json}")
        assertEquals("JSON 屏蔽规则格式无效", malformedJson.errorMessage)
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
        assertTrue(matchesDanmakuBlockRule(content = "123秒后高能", rule = "regex:\\d{1,3}秒"))
        assertTrue(matchesDanmakuBlockRule(content = "第12集封神", rule = "r=第\\d{1,2}集"))
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
    fun compileDanmakuBlockRules_reusesCompiledMatchersForSameRule() {
        val rules = listOf("剧透", "regex:\\d{4}年", "/哈{3,}/")
        val first = compileDanmakuBlockRules(rules)
        val second = compileDanmakuBlockRules(rules)

        assertEquals(first.map { it::class }, second.map { it::class })
        assertEquals(3, first.size)
        assertTrue(first[0].matches("这段有剧透注意"))
        assertTrue(first[1].matches("2026年新番"))
        assertTrue(first[2].matches("哈哈哈哈"))
    }

    @Test
    fun shouldBlockDanmakuByRules_repeatedCallsKeepSemantics() {
        val rules = listOf("剧透", "regex:第\\d+集")
        assertTrue(shouldBlockDanmakuByRules("第24集剧透", rules))
        assertTrue(shouldBlockDanmakuByRules("第24集剧透", rules))
        assertFalse(shouldBlockDanmakuByRules("纯路人弹幕", rules))
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
            regexRules = listOf("regex:第\\d+集", "regex:第\\d+集", "\\d{1,3}"),
            userHashRules = listOf("abc123", "uid:xyz")
        )

        assertEquals(
            listOf("剧透", "哈哈", "regex:第\\d+集", "regex:\\d{1,3}", "uid:abc123", "uid:xyz"),
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
