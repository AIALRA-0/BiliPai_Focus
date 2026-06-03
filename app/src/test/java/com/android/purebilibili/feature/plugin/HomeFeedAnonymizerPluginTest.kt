package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.network.policy.HomeFeedAnonymizerRuntime
import com.android.purebilibili.core.network.policy.HomeFeedAnonymizerStatsSnapshot
import com.android.purebilibili.core.plugin.PluginCapability
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedAnonymizerPluginTest {

    @Test
    fun plugin_declaresMetadataAndCapabilities() {
        val plugin = HomeFeedAnonymizerPlugin()

        assertEquals(HOME_FEED_ANONYMIZER_PLUGIN_ID, plugin.id)
        assertEquals("初见推荐", plugin.name)
        assertEquals("1.0.1", plugin.version)
        assertEquals("BiliPai项目组", plugin.author)
        assertFalse(plugin.unavailable)
        assertEquals(
            setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.NETWORK
            ),
            plugin.capabilityManifest.capabilities
        )
    }

    @Test
    fun plugin_enableAndDisable_updatesRuntimeState() = runTest {
        val plugin = HomeFeedAnonymizerPlugin()
        HomeFeedAnonymizerRuntime.setEnabled(false)

        plugin.onEnable()
        assertTrue(HomeFeedAnonymizerRuntime.enabled)

        plugin.onDisable()
        assertFalse(HomeFeedAnonymizerRuntime.enabled)
    }

    @Test
    fun credits_includeOriginalAuthorPlusForkAndAppinnSource() {
        val credits = buildHomeFeedAnonymizerCreditRows()

        assertEquals(
            listOf(
                "原作者",
                "Plus 改版",
                "介绍来源"
            ),
            credits.map { it.label }
        )
        assertEquals("wangdaodao", credits[0].summary)
        assertEquals("https://github.com/wangdaodaodao/TabulaBili", credits[0].url)
        assertEquals("tjsky", credits[1].summary)
        assertEquals("https://github.com/tjsky/TabulaBili", credits[1].url)
        assertEquals("Appinn 介绍帖", credits[2].summary)
        assertEquals("https://meta.appinn.net/t/topic/85526", credits[2].url)
    }

    @Test
    fun statsUiModel_formatsEmptyHitAndResetStates() {
        val empty = buildHomeFeedAnonymizerStatsUiModel(
            snapshot = HomeFeedAnonymizerStatsSnapshot(),
            enabled = false
        )

        assertEquals("未启用", empty.stateRow.summary)
        assertEquals("0 次", empty.totalRow.summary)
        assertEquals("暂无命中", empty.lastHitRow.summary)
        assertEquals(1, empty.totalRow.maxLines)
        assertEquals(1, empty.lastHitRow.maxLines)
    }

    @Test
    fun statsUiModel_formatsHitStateAndFullLongPressContent() {
        val model = buildHomeFeedAnonymizerStatsUiModel(
            snapshot = HomeFeedAnonymizerStatsSnapshot(
                totalHits = 12L,
                lastHitAtMs = 1_700_000_000_000L,
                lastHitHost = "api.bilibili.com",
                lastHitEncodedPath = "/x/web-interface/wbi/index/top/feed/rcmd"
            ),
            enabled = true
        )

        assertEquals("运行中", model.stateRow.summary)
        assertEquals("12 次", model.totalRow.summary)
        assertEquals("api.bilibili.com", model.lastHitRow.summary)
        assertTrue(model.lastHitRow.fullContent.contains("/x/web-interface/wbi/index/top/feed/rcmd"))
        assertTrue(model.scopeRow.fullContent.contains("https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd"))
        assertTrue(model.scopeRow.fullContent.contains("频繁刷新"))
        assertTrue(model.scopeRow.fullContent.contains("关闭插件"))
        assertEquals(2, model.scopeRow.maxLines)
    }

    @Test
    fun creditRows_useShortLayoutTextButKeepFullLongPressContent() {
        val rows = buildHomeFeedAnonymizerCreditRows()

        rows.forEach { row ->
            assertEquals(1, row.maxLines)
            assertTrue(row.fullContent.contains(row.url.orEmpty()))
        }
        assertTrue(rows[0].fullContent.contains("TabulaBili 原项目"))
        assertTrue(rows[1].fullContent.contains("TabulaBili-Plus"))
        assertTrue(rows[2].fullContent.contains("Appinn"))
    }
}
