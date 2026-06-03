package com.android.purebilibili.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppNotificationChannelsTest {

    @Test
    fun resolveAppNotificationChannels_includesDownloadChannel() {
        val channel = resolveAppNotificationChannels()
            .firstOrNull { it.id == DOWNLOAD_NOTIFICATION_CHANNEL_ID }

        assertNotNull(channel)
        assertEquals("下载任务", channel.name)
    }

    @Test
    fun resolveAppNotificationChannels_includesSponsorBlockChannel() {
        val channel = resolveAppNotificationChannels()
            .firstOrNull { it.id == SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID }

        assertNotNull(channel)
        assertEquals("空降助手", channel.name)
    }

    @Test
    fun resolveAppNotificationChannels_includesJsonPluginStatsChannel() {
        val channel = resolveAppNotificationChannels()
            .firstOrNull { it.id == JSON_PLUGIN_STATS_NOTIFICATION_CHANNEL_ID }

        assertNotNull(channel)
        assertEquals("插件统计", channel.name)
    }
}
