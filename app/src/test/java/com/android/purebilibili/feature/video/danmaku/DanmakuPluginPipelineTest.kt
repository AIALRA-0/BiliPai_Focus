package com.android.purebilibili.feature.video.danmaku

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.json.HighlightStyle
import com.android.purebilibili.core.plugin.json.JsonPluginManager
import com.android.purebilibili.core.plugin.json.JsonRulePlugin
import com.android.purebilibili.core.plugin.json.LoadedJsonPlugin
import com.android.purebilibili.core.plugin.json.Rule
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.parser.AdvancedDanmakuData
import com.android.purebilibili.danmaku.parser.bas.BasDanmaku
import com.android.purebilibili.danmaku.parser.bas.BasScriptParser
import com.android.purebilibili.feature.plugin.DanmakuEnhancePlugin
import com.android.purebilibili.plugin.sdk.DanmakuItem as PluginDanmakuItem
import com.android.purebilibili.plugin.sdk.DanmakuPluginApi
import com.android.purebilibili.plugin.sdk.DanmakuStyle
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
class DanmakuPluginPipelineTest {
    private lateinit var manager: DanmakuManager
    private var nativePlugins: List<DanmakuPluginApi> = emptyList()
    private lateinit var jsonPlugins: MutableStateFlow<List<LoadedJsonPlugin>>
    private lateinit var previousJsonPlugins: List<LoadedJsonPlugin>

    @Before
    fun setUp() {
        mockkObject(PluginManager)
        every { PluginManager.getEnabledDanmakuPlugins() } answers { nativePlugins }
        every { PluginManager.danmakuPluginUpdateToken } returns MutableStateFlow(0L)
        @Suppress("UNCHECKED_CAST")
        val state = JsonPluginManager::class.java.getDeclaredField("_plugins").run {
            isAccessible = true
            get(JsonPluginManager) as MutableStateFlow<List<LoadedJsonPlugin>>
        }
        jsonPlugins = state
        previousJsonPlugins = state.value
        state.value = emptyList()
        manager = DanmakuManager.createSession(
            RuntimeEnvironment.getApplication() as Context,
            CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @After
    fun tearDown() {
        manager.release()
        jsonPlugins.value = previousJsonPlugins
        unmockkObject(PluginManager)
    }

    @Test
    fun nativeEditsAreFilteredByRealJsonRulesForOrdinaryAdvancedAndBasItems() {
        installCleaner()
        nativePlugins = listOf(plugin(filter = { it.copy(content = "哈哈哈哈哈") }))
        val source = standard("普通弹幕")
        val advanced = advanced("高级弹幕")
        val script = bas("BAS弹幕")

        val result = pipeline(listOf(source), listOf(advanced))
        assertTrue(result.first.isEmpty())
        assertTrue(result.second.isEmpty())
        assertTrue(filterBas(listOf(script)).isEmpty())
        assertEquals("普通弹幕", source.text)
        assertEquals("高级弹幕", advanced.content)
        assertEquals("BAS弹幕", script.program.textContent)
    }

    @Test
    fun realNativeKeywordFilterStopsItemsBeforeLaterPluginsCanRewriteThem() {
        nativePlugins = listOf(
            DanmakuEnhancePlugin(),
            plugin(filter = { it.copy(content = "安全内容") })
        )
        val result = pipeline(listOf(standard("包含剧透")), listOf(advanced("前方高能")))
        assertTrue(result.first.isEmpty())
        assertTrue(result.second.isEmpty())
        assertTrue(filterBas(listOf(bas("包含剧透"))).isEmpty())
        assertEquals("安全内容", pipeline(listOf(standard("普通弹幕"))).first.single().text)
    }

    @Test
    fun realNativeHighlightAndCleanerPreserveArgbAndNeverPolluteCachedSources() {
        installCleaner()
        nativePlugins = listOf(DanmakuEnhancePlugin())
        val source = standard("【翻译】")
        val advanced = advanced("【翻译】")
        val script = bas("【翻译】")
        val first = pipeline(listOf(source), listOf(advanced))
        val second = pipeline(listOf(source), listOf(advanced))
        val renderedScript = filterBas(listOf(script)).single()

        assertNotSame(source, first.first.single())
        assertEquals(0xFFFFD700.toInt(), first.first.single().textColor)
        assertEquals(Typeface.DEFAULT_BOLD, first.first.single().typeface)
        assertEquals(1.05f, first.first.single().textSizeScale)
        assertEquals(1.05f, second.first.single().textSizeScale)
        assertEquals(0xFFD700, first.second.single().color)
        assertEquals(0xFFD700, renderedScript.colorOverride)
        val style = assertNotNull(style(PluginDanmakuItem(1, "【翻译】", 0)))
        assertEquals(0x80000000.toInt(), style.backgroundColor)
        assertTrue(style.bold)
        assertEquals(0xFFFFFFFF.toInt(), source.textColor)
        assertEquals(1f, source.textSizeScale)
        assertNull(source.typeface)
        assertEquals(0xFFFFFF, advanced.color)
        assertNull(script.colorOverride)
        assertEquals(1f, script.fontScale)
    }

    @Test
    fun jsonTransparentColorOverridesNativeButUnspecifiedOrInvalidColorDoesNot() {
        nativePlugins = listOf(DanmakuEnhancePlugin())
        fun render(color: String?): DanmakuItem {
            installRules(listOf(highlight(color)))
            return pipeline(listOf(standard("【翻译】"))).first.single()
        }

        assertEquals(0, render("#00000000").textColor)
        assertEquals(0x80000000.toInt(), render("#80000000").textColor)
        assertEquals(0xFFFFD700.toInt(), render(null).textColor)
        assertEquals(0xFFFFD700.toInt(), render("invalid").textColor)
        val merged = assertNotNull(style(PluginDanmakuItem(1, "【翻译】", 0)))
        assertEquals(0x80000000.toInt(), merged.backgroundColor)
        assertEquals(1.05f, merged.scale)
    }

    @Test
    fun nativeFailuresPassThroughToJsonAndJsonFailuresKeepTheItemAndNativeStyle() {
        val failing = plugin(
            filter = { error("native filter failure") },
            style = { error("native style failure") }
        )
        nativePlugins = listOf(failing, DanmakuEnhancePlugin())
        installCleaner()
        assertTrue(pipeline(listOf(standard("哈哈哈哈哈"))).first.isEmpty())
        assertEquals(0xFFFFD700.toInt(), pipeline(listOf(standard("【翻译】"))).first.single().textColor)

        val malformed = Rule(
            field = "content", op = "contains", value = JsonArray(emptyList()), action = "hide"
        )
        installRules(listOf(malformed, malformed.copy(action = "highlight")))
        val rendered = pipeline(listOf(standard("【翻译】"))).first.single()
        assertEquals("【翻译】", rendered.text)
        assertEquals(0xFFFFD700.toInt(), rendered.textColor)
    }

    @Test
    fun basEditsMustParseAndDoNotMutateTheOriginalProgram() {
        val original = bas("原始")
        val replacement = """def text title { content = "替换" }"""
        nativePlugins = listOf(plugin(filter = { it.copy(content = replacement, timeMs = 500) }))
        val edited = filterBas(listOf(original)).single()
        assertEquals("替换", edited.program.textContent)
        assertEquals(500L, edited.startTimeMs)
        assertEquals("原始", original.program.textContent)
        assertEquals(0L, original.startTimeMs)

        nativePlugins = listOf(plugin(filter = { it.copy(content = "def text {") }))
        assertTrue(filterBas(listOf(original)).isEmpty())
        nativePlugins = listOf(plugin(filter = { error("filter failure") }))
        assertSame(original, filterBas(listOf(original)).single())
    }

    private fun installCleaner() {
        val file = sequenceOf(
            File("plugins/samples/danmaku_cleaner.json"),
            File("../plugins/samples/danmaku_cleaner.json")
        ).first { it.isFile }
        val cleaner = Json.decodeFromString<JsonRulePlugin>(file.readText())
        jsonPlugins.value = listOf(LoadedJsonPlugin(cleaner, true, null))
    }

    private fun installRules(rules: List<Rule>) {
        jsonPlugins.value = listOf(
            LoadedJsonPlugin(JsonRulePlugin(id = "pipeline", name = "Pipeline", type = "danmaku", rules = rules), true, null)
        )
    }

    private fun highlight(color: String?) = Rule(
        field = "content", op = "startsWith", value = JsonPrimitive("【"), action = "highlight",
        style = HighlightStyle(color = color)
    )

    private fun plugin(
        filter: (PluginDanmakuItem) -> PluginDanmakuItem? = { it },
        style: (PluginDanmakuItem) -> DanmakuStyle? = { null }
    ): DanmakuPluginApi = object : DanmakuPluginApi {
        override val capabilityManifest = PluginCapabilityManifest(
            pluginId = "pipeline-native", displayName = "Pipeline native", version = "1",
            apiVersion = 1, entryClassName = "PipelineNative", capabilities = emptySet()
        )
        override fun filterDanmaku(danmaku: PluginDanmakuItem) = filter(danmaku)
        override fun styleDanmaku(danmaku: PluginDanmakuItem) = style(danmaku)
    }

    private fun standard(content: String) = DanmakuItem().apply {
        danmakuId = 1
        text = content
        textColor = 0xFFFFFFFF.toInt()
    }

    private fun advanced(content: String) = AdvancedDanmakuData(
        id = "2", content = content, startTimeMs = 0, durationMs = 1_000, startX = 0f, startY = 0f
    )

    private fun bas(content: String): BasDanmaku {
        val source = """def text title { content = "$content" }"""
        return BasDanmaku(3, 0, source, BasScriptParser.parse(source))
    }

    @Suppress("UNCHECKED_CAST")
    private fun pipeline(
        standard: List<DanmakuItem>, advanced: List<AdvancedDanmakuData> = emptyList()
    ): Pair<List<DanmakuItem>, List<AdvancedDanmakuData>> =
        DanmakuManager::class.java.getDeclaredMethod("applyDanmakuPluginPipeline", List::class.java, List::class.java).run {
            isAccessible = true
            invoke(manager, standard, advanced) as Pair<List<DanmakuItem>, List<AdvancedDanmakuData>>
        }

    @Suppress("UNCHECKED_CAST")
    private fun filterBas(items: List<BasDanmaku>): List<BasDanmaku> =
        DanmakuManager::class.java.getDeclaredMethod("filterBasDanmakuForRender", List::class.java).run {
            isAccessible = true
            invoke(manager, items) as List<BasDanmaku>
        }

    private fun style(item: PluginDanmakuItem): DanmakuStyle? =
        DanmakuManager::class.java.getDeclaredMethod(
            "collectDanmakuStyle", PluginDanmakuItem::class.java, List::class.java, Boolean::class.javaPrimitiveType!!
        ).run {
            isAccessible = true
            invoke(manager, item, nativePlugins, true) as DanmakuStyle?
        }
}
