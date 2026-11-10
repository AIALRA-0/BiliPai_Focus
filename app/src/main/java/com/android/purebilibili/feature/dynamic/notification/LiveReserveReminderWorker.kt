package com.android.purebilibili.feature.dynamic.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.purebilibili.EXTRA_PENDING_NAVIGATION_ROUTE
import com.android.purebilibili.MainActivity
import com.android.purebilibili.R
import com.android.purebilibili.app.LIVE_RESERVE_NOTIFICATION_CHANNEL_ID
import com.android.purebilibili.feature.dynamic.components.resolveLiveReserveNotificationTime
import com.android.purebilibili.navigation.ScreenRoutes

internal class LiveReserveReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reserveId = inputData.getLong(KEY_RESERVE_ID, 0L)
        val dynamicId = inputData.getString(KEY_DYNAMIC_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val startAtMillis = inputData.getLong(KEY_START_AT, 0L)
        if (reserveId <= 0L || dynamicId.isBlank() || startAtMillis <= 0L) return Result.failure()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(LIVE_RESERVE_NOTIFICATION_CHANNEL_ID)?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) return Result.success()

        val notificationId = (reserveId % Int.MAX_VALUE).toInt().coerceAtLeast(1)
        val route = ScreenRoutes.DynamicDetail.createRoute(dynamicId)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, MainActivity::class.java).apply {
                data = Uri.Builder()
                    .scheme("bilipai-notification")
                    .authority("live-reserve")
                    .appendPath(reserveId.toString())
                    .build()
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_PENDING_NAVIGATION_ROUTE, route)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val formattedTime = resolveLiveReserveNotificationTime(startAtMillis)
        val notification = NotificationCompat.Builder(
            applicationContext,
            LIVE_RESERVE_NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("直播预约即将开播")
            .setContentText("${title.ifBlank { "你预约的直播" }} · 计划 $formattedTime 开播")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${title.ifBlank { "你预约的直播" }}将在 $formattedTime 开播，点击查看动态。",
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        return try {
            NotificationManagerCompat.from(applicationContext).notify(
                "live-reserve:$reserveId",
                notificationId,
                notification,
            )
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        }
    }

    companion object {
        const val KEY_RESERVE_ID = "reserve_id"
        const val KEY_DYNAMIC_ID = "dynamic_id"
        const val KEY_TITLE = "title"
        const val KEY_START_AT = "start_at"
    }
}
