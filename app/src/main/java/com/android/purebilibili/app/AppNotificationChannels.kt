package com.android.purebilibili.app

import android.app.NotificationManager

internal const val MEDIA_PLAYBACK_NOTIFICATION_CHANNEL_ID = "media_playback_channel"
internal const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"

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
        )
    )
}
