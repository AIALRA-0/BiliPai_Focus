@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.video.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.Logger
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

internal fun shouldStartForegroundWithFallback(primaryNotification: Any?): Boolean {
    return primaryNotification == null
}

internal fun shouldRefreshPlaybackServiceForeground(
    action: String?,
    isForegroundStarted: Boolean
): Boolean {
    return action == PlaybackService.ACTION_START_FOREGROUND ||
        action == PlaybackService.ACTION_MEDIA_CONTROL
}

internal fun shouldStopPlaybackServiceForStartId(
    stopActionStartId: Int,
    latestHandledStartId: Int
): Boolean {
    return stopActionStartId >= latestHandledStartId
}

internal fun resolvePlaybackServiceFallbackIconRes(iconKey: String): Int {
    return resolveNotificationSmallIconRes(iconKey)
}

/** 媒体服务负责会话通知的状态、动作和前台生命周期，启动阶段保留准备通知。 */
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "mini_player_channel"
        const val ACTION_START_FOREGROUND = "com.android.purebilibili.action.START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.android.purebilibili.action.STOP_FOREGROUND"
        const val ACTION_MEDIA_CONTROL = "com.android.purebilibili.action.MEDIA_CONTROL"
        const val NOTIFICATION_ID = 1002 // 必须与 MiniPlayerManager 中的 ID 一致
        @Volatile private var isForegroundStarted = false
        @Volatile private var latestHandledStartId = 0
    }

    private var notificationOwner: MiniPlayerManager? = null
    private val refreshNotification: () -> Unit = {
        bindCurrentSession()
        triggerNotificationUpdate()
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        bindNotificationOwner(MiniPlayerManager.getInstance(applicationContext))
    }

    private fun bindNotificationOwner(owner: MiniPlayerManager?) {
        if (notificationOwner === owner) return
        notificationOwner?.let { previous ->
            if (previous.onNotificationRefreshRequested === refreshNotification) {
                previous.onNotificationRefreshRequested = null
            }
        }
        notificationOwner = owner
        if (owner != null) {
            owner.onNotificationRefreshRequested = refreshNotification
            setMediaNotificationProvider(PlaybackMediaNotificationProvider(this, owner::decoratePlaybackNotification))
        }
    }

    private fun bindCurrentSession() {
        // 播放器管理器释放后可重新创建，存活的服务不能继续绑定旧实例。
        bindNotificationOwner(MiniPlayerManager.getInstanceOrNull())
        val current = notificationOwner?.mediaSession
        sessions.filter { it !== current }.forEach(::removeSession)
        if (current != null && !isSessionAdded(current)) addSession(current)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        bindCurrentSession()
        return notificationOwner?.mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        latestHandledStartId = maxOf(latestHandledStartId, startId)
        Logger.d(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_START_FOREGROUND, ACTION_MEDIA_CONTROL -> {
                if (shouldRefreshPlaybackServiceForeground(action, isForegroundStarted)) {
                    try {
                        // 启动时复用最近的媒体通知；尚未生成时使用准备通知。
                        val primaryNotification = MiniPlayerManager.getInstance(applicationContext).currentNotification
                        val notification = if (shouldStartForegroundWithFallback(primaryNotification)) {
                            Logger.w(TAG, "Primary notification missing, starting foreground with fallback notification")
                            buildFallbackNotification()
                        } else {
                            primaryNotification as Notification
                        }
                        startAsForeground(notification)
                        isForegroundStarted = true
                        bindCurrentSession()
                        triggerNotificationUpdate()
                        // A notification action must promote the service before resuming audio.
                        if (action == ACTION_MEDIA_CONTROL) {
                            resolveMediaControlType(
                                intent?.getIntExtra(MiniPlayerManager.EXTRA_CONTROL_TYPE, 0) ?: 0
                            )?.let { control ->
                                MiniPlayerManager.getInstance(applicationContext).performMediaControl(control)
                            }
                        }
                    } catch (e: Exception) {
                        isForegroundStarted = false
                        Logger.e(TAG, "Failed to start foreground service", e)
                        stopSelf()
                    }
                }
            }
            ACTION_STOP_FOREGROUND -> {
                Logger.d(TAG, "Stopping foreground service")
                isForegroundStarted = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)
                if (shouldStopPlaybackServiceForStartId(startId, latestHandledStartId)) {
                    stopSelfResult(startId)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 切后台继续播放；只有从最近任务移除时才关闭当前播放会话。
        MiniPlayerManager.getInstanceOrNull()?.dismissForTaskRemoval()
        isForegroundStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        bindNotificationOwner(null)
        super.onDestroy()
        isForegroundStarted = false
        latestHandledStartId = 0
        Logger.d(TAG, "onDestroy")
    }

    private fun startAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildFallbackNotification(): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(resolveNotificationIconRes(SettingsManager.getAppIconSync(this)))
            .setContentTitle("BiliPai")
            .setContentText("正在准备播放控件")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "小窗播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "小窗播放控制"
            setShowBadge(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
