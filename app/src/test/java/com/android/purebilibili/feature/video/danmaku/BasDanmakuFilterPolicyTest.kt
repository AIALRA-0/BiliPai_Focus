package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.parser.bas.BasDanmaku
import com.android.purebilibili.danmaku.parser.bas.BasScriptParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasDanmakuFilterPolicyTest {
    @Test
    fun `BAS follows the special switch rather than ordinary scroll and pinned switches`() {
        val item = script("""def text title { content = "高级弹幕" }""")
        assertTrue(visible(item, DanmakuTypeFilterSettings(allowScroll = false, allowTop = false, allowBottom = false)))
        assertFalse(visible(item, DanmakuTypeFilterSettings(allowSpecial = false)))
    }

    @Test
    fun `keywords match displayed text not BAS identifiers and commands`() {
        val clean = script("""def text blocked { content = "干杯" }""")
        val blocked = script("""def text title { content = "屏蔽内容" }""")
        val later = script("""def text title { content = "开始" } set title {} 1s then set title { content = "屏蔽内容" } 2s""")
        assertTrue(visible(clean, matchers = listOf(DanmakuKeywordMatcher("blocked"))))
        assertFalse(visible(blocked, matchers = listOf(DanmakuKeywordMatcher("屏蔽"))))
        assertFalse(visible(later, matchers = listOf(DanmakuKeywordMatcher("屏蔽"))))
    }

    @Test
    fun `color blocking includes scripted and animated colors rather than only the protobuf tag`() {
        val settings = DanmakuTypeFilterSettings(allowColorful = false)
        val animated = script("""def text title { content = "变色" } set title { color = 0x00a1d6 } 1s""")
        val path = script("""def path shape { d = "M0,0L10,10Z" fillColor = 0xff0000 }""")
        val white = script("""def text title { content = "白色" color = 0xffffff }""")
        assertFalse(visible(animated, settings))
        assertFalse(visible(path, settings))
        assertTrue(visible(white, settings))
    }

    @Test
    fun `self scripts bypass the weight threshold but not user blocking`() {
        val item = script("""def text title { content = "自己的弹幕" }""")
            .copy(weight = 1, userHash = "owner")
        assertFalse(visible(item, weight = 5))
        assertTrue(visible(item.copy(isSelf = true), weight = 5))
        assertFalse(visible(item.copy(isSelf = true), matchers = listOf(DanmakuUserHashMatcher("owner")), weight = 5))
    }

    private fun script(source: String): BasDanmaku = BasDanmaku(
        id = 1,
        startTimeMs = 0,
        source = source,
        program = BasScriptParser.parse(source)
    )

    private fun visible(
        item: BasDanmaku,
        settings: DanmakuTypeFilterSettings = DanmakuTypeFilterSettings(),
        matchers: List<DanmakuBlockRuleMatcher> = emptyList(),
        weight: Int = 0
    ): Boolean = shouldDisplayBasDanmaku(item, settings, matchers, weight)
}
