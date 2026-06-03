package com.android.purebilibili.core.plugin.json

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPluginStatsNotificationPolicyTest {

    @Test
    fun buildJsonPluginStatsSummaryNotification_usesDeltaSinceLastSummary() {
        val notification = buildJsonPluginStatsSummaryNotification(
            config = JsonPluginStatsNotificationConfig(enabled = true),
            plugins = listOf(
                jsonPlugin(id = "ad", name = "广告过滤"),
                jsonPlugin(id = "keyword", name = "关键词过滤")
            ),
            currentStats = mapOf("ad" to 20, "keyword" to 12),
            lastSummaryStats = mapOf("ad" to 5, "keyword" to 10)
        )

        assertEquals("插件统计汇总", notification?.title)
        assertEquals("上次汇总后已过滤 17 项：广告过滤 15、关键词过滤 2", notification?.body)
        assertEquals(mapOf("ad" to 20, "keyword" to 12), notification?.nextSummaryStats)
    }

    @Test
    fun buildJsonPluginStatsSummaryNotification_skipsWhenDisabledOrNoDelta() {
        val plugins = listOf(jsonPlugin(id = "ad", name = "广告过滤"))
        val disabled = buildJsonPluginStatsSummaryNotification(
            config = JsonPluginStatsNotificationConfig(enabled = false),
            plugins = plugins,
            currentStats = mapOf("ad" to 20),
            lastSummaryStats = mapOf("ad" to 0)
        )
        val noDelta = buildJsonPluginStatsSummaryNotification(
            config = JsonPluginStatsNotificationConfig(enabled = true),
            plugins = plugins,
            currentStats = mapOf("ad" to 20),
            lastSummaryStats = mapOf("ad" to 20)
        )

        assertNull(disabled)
        assertNull(noDelta)
    }

    @Test
    fun buildJsonPluginStatsTestNotification_doesNotRequireRealStats() {
        val notification = buildJsonPluginStatsTestNotification()

        assertEquals("插件统计测试通知", notification.title)
        assertEquals("通知权限正常后，这里会汇总 JSON 规则插件过滤数量", notification.body)
        assertEquals(emptyMap(), notification.nextSummaryStats)
    }

    private fun jsonPlugin(id: String, name: String): LoadedJsonPlugin {
        return LoadedJsonPlugin(
            plugin = JsonRulePlugin(
                id = id,
                name = name,
                type = "feed",
                rules = listOf(
                    Rule(
                        field = "title",
                        op = "contains",
                        value = JsonPrimitive("测试"),
                        action = "hide"
                    )
                )
            ),
            enabled = true,
            sourceUrl = null
        )
    }
}
