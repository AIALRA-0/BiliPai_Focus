package com.android.purebilibili.feature.dynamic.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.android.purebilibili.feature.dynamic.components.DynamicReserveAction
import java.util.concurrent.TimeUnit

internal object LiveReserveReminderScheduler {
    private const val REMINDER_LEAD_MILLIS = 10 * 60 * 1000L

    fun schedule(context: Context, action: DynamicReserveAction) {
        if (action.reserveId <= 0L) return
        val startAtMillis = action.startAtMillis
        if (action.dynamicId.isBlank() || startAtMillis == null) {
            cancel(context, action.reserveId)
            return
        }
        val now = System.currentTimeMillis()
        val reminderAtMillis = startAtMillis - REMINDER_LEAD_MILLIS
        if (startAtMillis <= now || reminderAtMillis <= now) {
            cancel(context, action.reserveId)
            return
        }
        val work = OneTimeWorkRequestBuilder<LiveReserveReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(LiveReserveReminderWorker.KEY_RESERVE_ID, action.reserveId)
                    .putString(LiveReserveReminderWorker.KEY_DYNAMIC_ID, action.dynamicId)
                    .putString(LiveReserveReminderWorker.KEY_TITLE, action.title)
                    .putLong(LiveReserveReminderWorker.KEY_START_AT, startAtMillis)
                    .build(),
            )
            .setInitialDelay(reminderAtMillis - now, TimeUnit.MILLISECONDS)
            .addTag(workName(action.reserveId))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(action.reserveId),
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    fun cancel(context: Context, reserveId: Long) {
        if (reserveId <= 0L) return
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(reserveId))
    }

    private fun workName(reserveId: Long) = "live_reserve_reminder_$reserveId"
}
