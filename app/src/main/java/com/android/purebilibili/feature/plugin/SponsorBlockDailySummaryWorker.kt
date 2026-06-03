package com.android.purebilibili.feature.plugin

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
import com.android.purebilibili.app.SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

private const val TAG = "SponsorBlockDailySummary"
private const val SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME = "sponsor_block_daily_summary"
private const val SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID = 0x5B10C

class SponsorBlockDailySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val config = readConfig() ?: return Result.success()
        val records = SponsorBlockInsightStore.readRecords(applicationContext)
        val notification = buildSponsorBlockDailySummaryNotification(
            config = config,
            records = records,
            dayStartMs = currentLocalDayStartMs()
        ) ?: return Result.success()
        postNotification(notification)
        return Result.success()
    }

    private suspend fun readConfig(): SponsorBlockConfig? {
        return runCatching {
            val json = PluginStore.getConfigJson(applicationContext, SPONSOR_BLOCK_PLUGIN_ID)
                ?: return SponsorBlockConfig.default()
            Json.decodeFromString<SponsorBlockConfig>(json).normalized()
        }.onFailure { error ->
            Logger.w(TAG, "读取空降助手配置失败: ${error.message}")
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(content: SponsorBlockDailySummaryNotification) {
        postSponsorBlockSummaryNotification(
            context = applicationContext,
            content = content,
            notificationId = SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID
        )
    }
}

fun postSponsorBlockTestNotification(
    context: Context,
    config: SponsorBlockConfig
): Boolean {
    return postSponsorBlockSummaryNotification(
        context = context.applicationContext,
        content = buildSponsorBlockTestNotification(config),
        notificationId = SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID
    )
}

@SuppressLint("MissingPermission")
private fun postSponsorBlockSummaryNotification(
    context: Context,
    content: SponsorBlockDailySummaryNotification,
    notificationId: Int
): Boolean {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return false
    val notification = NotificationCompat.Builder(
        context,
        SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID
    )
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(content.title)
        .setContentText(content.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(buildSponsorBlockPendingIntent(context))
        .setAutoCancel(true)
        .build()
    return runCatching {
        manager.notify(notificationId, notification)
        true
    }.onFailure { error ->
        Logger.w(TAG, "发送空降助手汇总通知失败: ${error.message}")
    }.getOrDefault(false)
}

private fun buildSponsorBlockPendingIntent(context: Context): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bilipai://plugins"), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return PendingIntent.getActivity(
        context,
        SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

internal fun scheduleSponsorBlockDailySummary(
    context: Context,
    enabled: Boolean
) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    if (!enabled) {
        workManager.cancelUniqueWork(SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<SponsorBlockDailySummaryWorker>(
        repeatInterval = 1,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    ).build()
    workManager.enqueueUniquePeriodicWork(
        SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}
