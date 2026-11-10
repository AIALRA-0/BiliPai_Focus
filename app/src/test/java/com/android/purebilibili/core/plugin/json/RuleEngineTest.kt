package com.android.purebilibili.core.plugin.json

import android.app.Application
import com.android.purebilibili.plugin.sdk.DanmakuItem
import java.io.File
import kotlinx.serialization.json.Json
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class RuleEngineTest {

    @Test
    fun shouldShowVideo_exposesAdditionalFeedFieldsForFiltering() {
        val video = VideoItem(
            bvid = "BV1",
            title = "测试视频",
            owner = Owner(mid = 42L, name = "UP-A"),
            stat = Stat(view = 1_000, reply = 8),
            tid = 36,
            tname = "知识"
        )
        val rules = listOf(
            Rule(field = "stat.reply", op = "ge", value = JsonPrimitive(5), action = "hide"),
            Rule(field = "tid", op = "eq", value = JsonPrimitive(36), action = "hide"),
            Rule(field = "tname", op = "eq", value = JsonPrimitive("知识"), action = "hide")
        )

        assertFalse(RuleEngine.shouldShowVideo(video, rules))
    }

    @Test
    fun cleanerSampleFiltersSpamAndHighlightsTranslations() {
        val sample = sequenceOf(
            File("plugins/samples/danmaku_cleaner.json"),
            File("../plugins/samples/danmaku_cleaner.json")
        ).first { it.isFile }
        val plugin = Json.decodeFromString<JsonRulePlugin>(sample.readText())
        assertFalse(RuleEngine.shouldShowDanmaku(item("哈哈哈哈哈"), plugin.rules))
        val translation = item("【翻译】")
        assertTrue(RuleEngine.shouldShowDanmaku(translation, plugin.rules))
        val style = assertNotNull(RuleEngine.getDanmakuHighlightStyle(translation, plugin.rules))
        assertEquals(0xFFFFD700.toInt(), style.textColor)
        assertTrue(style.bold)
        assertNull(RuleEngine.getDanmakuHighlightStyle(item("普通弹幕"), plugin.rules))
    }

    @Test
    fun highlightColorParsingPreservesAlphaAndDistinguishesTransparentFromUnspecified() {
        fun color(value: String?) = assertNotNull(
            RuleEngine.getDanmakuHighlightStyle(
                item("【翻译】"),
                listOf(
                    Rule(
                        field = "content", op = "startsWith", value = JsonPrimitive("【"),
                        action = "highlight", style = HighlightStyle(color = value)
                    )
                )
            )
        ).textColor

        assertEquals(0x80000000.toInt(), color("#80000000"))
        assertEquals(0, color("#00000000"))
        assertNull(color(null))
        assertNull(color("not-a-color"))
    }

    @Test
    fun typeRulesSupportAdvancedAndBasModes() {
        val rules = listOf(
            Rule(field = "type", op = "eq", value = JsonPrimitive(9), action = "hide")
        )
        assertFalse(RuleEngine.shouldShowDanmaku(item("script").copy(type = 9), rules))
        assertTrue(RuleEngine.shouldShowDanmaku(item("advanced").copy(type = 7), rules))
    }

    private fun item(content: String) = DanmakuItem(id = 1, content = content, timeMs = 0)
}
