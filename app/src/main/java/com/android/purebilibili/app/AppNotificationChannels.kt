package com.android.purebilibili.app

import android.app.NotificationManager

internal const val MEDIA_PLAYBACK_NOTIFICATION_CHANNEL_ID = "media_playback_channel"
internal const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
internal const val SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID = "sponsor_block_channel"
internal const val JSON_PLUGIN_STATS_NOTIFICATION_CHANNEL_ID = "json_plugin_stats_channel"

internal data class AppNotificationChannelSpec(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int,
    val showBadge: Boolean = false,
    val silent: Boolean = true
)

internal fun resolveAppNotificationChannels(): List<AppNotificationChannelSpec> {
    return listOf(
        AppNotificationChannelSpec(
            id = MEDIA_PLAYBACK_NOTIFICATION_CHANNEL_ID,
            name = "媒体播放",
            description = "显示正在播放的视频控制条",
            importance = NotificationManager.IMPORTANCE_LOW
        ),
        AppNotificationChannelSpec(
            id = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            name = "下载任务",
            description = "显示后台下载进度",
            importance = NotificationManager.IMPORTANCE_LOW
        ),
        AppNotificationChannelSpec(
            id = SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID,
            name = "空降助手",
            description = "显示空降助手每日节省时间汇总",
            importance = NotificationManager.IMPORTANCE_LOW
        ),
        AppNotificationChannelSpec(
            id = JSON_PLUGIN_STATS_NOTIFICATION_CHANNEL_ID,
            name = "插件统计",
            description = "显示 JSON 规则插件过滤数量汇总",
            importance = NotificationManager.IMPORTANCE_LOW
        )
    )
}
