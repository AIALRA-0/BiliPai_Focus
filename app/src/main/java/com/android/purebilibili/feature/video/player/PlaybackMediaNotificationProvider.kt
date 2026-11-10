@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.video.player

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.android.purebilibili.R
import com.google.common.collect.ImmutableList

/** 复用标准媒体通知及动作，只保留应用外观和原有的下一曲、播放、上一曲顺序。 */
internal class PlaybackMediaNotificationProvider(
    context: Context,
    private val decorateAppearance: (Notification) -> Notification,
) : MediaNotification.Provider {
    private val delegate = object : DefaultMediaNotificationProvider(
        context, { PlaybackService.NOTIFICATION_ID }, "mini_player_channel",
        R.string.mini_player_notification_channel,
    ) {
        override fun addNotificationActions(
            mediaSession: MediaSession,
            mediaButtons: ImmutableList<CommandButton>,
            builder: NotificationCompat.Builder,
            actionFactory: MediaNotification.ActionFactory,
        ): IntArray {
            val transportOrder = resolveExternalTransportActionOrder().toList()
            val ordered = ImmutableList.copyOf(mediaButtons.sortedBy { button ->
                val action = when (button.playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> MiniPlayerManager.ACTION_NEXT
                    Player.COMMAND_PLAY_PAUSE -> MiniPlayerManager.ACTION_PLAY_PAUSE
                    Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> MiniPlayerManager.ACTION_PREVIOUS
                    else -> null
                }
                action?.let(transportOrder::indexOf)?.takeIf { it >= 0 } ?: Int.MAX_VALUE
            })
            super.addNotificationActions(mediaSession, ordered, builder, actionFactory)
            return IntArray(ordered.size.coerceAtMost(3)) { it }
        }
    }

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification = decorate(delegate.createNotification(
        mediaSession, mediaButtonPreferences, actionFactory,
        { updated -> onNotificationChangedCallback.onNotificationChanged(decorate(updated)) },
    ))

    private fun decorate(notification: MediaNotification): MediaNotification =
        MediaNotification(notification.notificationId, decorateAppearance(notification.notification))

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        delegate.handleCustomCommand(session, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.notificationChannelInfo
}
