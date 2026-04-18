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
}
