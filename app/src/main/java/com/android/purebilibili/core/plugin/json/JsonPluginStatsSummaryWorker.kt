package com.android.purebilibili.core.plugin.json

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.purebilibili.MainActivity
import com.android.purebilibili.R
import com.android.purebilibili.app.JSON_PLUGIN_STATS_NOTIFICATION_CHANNEL_ID
import com.android.purebilibili.core.util.Logger
import java.util.concurrent.TimeUnit

private const val TAG = "JsonPluginStatsSummary"
private const val JSON_PLUGIN_STATS_SUMMARY_WORK_NAME = "json_plugin_stats_summary"
private const val JSON_PLUGIN_STATS_SUMMARY_NOTIFICATION_ID = 0x7A105

class JsonPluginStatsSummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        JsonPluginManager.initialize(applicationContext)
        val config = readJsonPluginStatsNotificationConfig(applicationContext)
        val notification = buildJsonPluginStatsSummaryNotification(
            config = config,
            plugins = JsonPluginManager.plugins.value,
            currentStats = JsonPluginManager.filterStats.value,
            lastSummaryStats = readLastJsonPluginStatsSummary(applicationContext)
        ) ?: return Result.success()
        postJsonPluginStatsNotification(
            context = applicationContext,
            content = notification,
            notificationId = JSON_PLUGIN_STATS_SUMMARY_NOTIFICATION_ID
        )
        persistLastJsonPluginStatsSummary(applicationContext, notification.nextSummaryStats)
        return Result.success()
    }
}

internal fun scheduleJsonPluginStatsSummary(
    context: Context,
    enabled: Boolean
) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    if (!enabled) {
        workManager.cancelUniqueWork(JSON_PLUGIN_STATS_SUMMARY_WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<JsonPluginStatsSummaryWorker>(
        repeatInterval = 1,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    ).build()
    workManager.enqueueUniquePeriodicWork(
        JSON_PLUGIN_STATS_SUMMARY_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun postJsonPluginStatsTestNotification(context: Context): Boolean {
    return postJsonPluginStatsNotification(
        context = context.applicationContext,
        content = buildJsonPluginStatsTestNotification(),
        notificationId = JSON_PLUGIN_STATS_SUMMARY_NOTIFICATION_ID
    )
}

@SuppressLint("MissingPermission")
private fun postJsonPluginStatsNotification(
    context: Context,
    content: JsonPluginStatsSummaryNotification,
    notificationId: Int
): Boolean {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return false
    val notification = NotificationCompat.Builder(context, JSON_PLUGIN_STATS_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(content.title)
        .setContentText(content.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(buildPluginsSettingsPendingIntent(context))
        .setAutoCancel(true)
        .build()
    return runCatching {
        manager.notify(notificationId, notification)
        true
    }.onFailure { error ->
        Logger.w(TAG, "发送插件统计通知失败: ${error.message}")
    }.getOrDefault(false)
}

private fun buildPluginsSettingsPendingIntent(context: Context): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bilipai://plugins"), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return PendingIntent.getActivity(
        context,
        JSON_PLUGIN_STATS_SUMMARY_NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
