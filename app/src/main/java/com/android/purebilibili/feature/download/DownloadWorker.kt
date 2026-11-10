package com.android.purebilibili.feature.download

import android.content.Context
import android.os.Build
import androidx.work.*
import com.android.purebilibili.app.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

/**
 * 🔧 WorkManager Worker for background downloads
 * 
 * This worker handles video downloads in a way that survives app backgrounding
 * and process death. WorkManager automatically reschedules work if the process dies.
 */
class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val TAG_DOWNLOAD = "video_download"

        // 通知 id 按任务派生：同一任务反复 setForeground 复用同一个 id，避免堆积；
        // 并发下载时各任务各自一条通知
        fun foregroundNotificationId(taskId: String?): Int = (taskId ?: "download").hashCode()
        
        /**
         * 调度下载任务
         */
        fun enqueue(context: Context, taskId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val inputData = Data.Builder()
                .putString(KEY_TASK_ID, taskId)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(TAG_DOWNLOAD)
                .addTag(taskId) // 用于取消特定任务
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30_000L, // 30 秒初始退避
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    taskId,
                    ExistingWorkPolicy.KEEP, // 如果已存在则保留
                    workRequest
                )
            
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "📥 Enqueued download: $taskId")
        }
        
        /**
         * 取消下载任务
         */
        fun cancel(context: Context, taskId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(taskId)
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "⏹️ Cancelled download: $taskId")
        }
        
        /**
         * 取消所有下载任务
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG_DOWNLOAD)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) 
            ?: return@withContext Result.failure()
        
        com.android.purebilibili.core.util.Logger.d("DownloadWorker", "🚀 Starting download: $taskId")
        DownloadManager.init(applicationContext)
        setForeground(getForegroundInfo())

        // 周期性把任务进度刷到前台通知上（进度条 + 百分比）
        val progressJob = launch {
            while (isActive) {
                delay(2_000L)
                val progress = DownloadManager.tasks.value[taskId]?.progress ?: 0f
                runCatching { setForeground(getForegroundInfo(progress)) }
            }
        }

        try {
            val foregroundWorkTimeoutMs = resolveDownloadForegroundWorkTimeoutMs(Build.VERSION.SDK_INT)
            if (foregroundWorkTimeoutMs != null) {
                withTimeout(foregroundWorkTimeoutMs) {
                    DownloadManager.executeDownload(taskId)
                }
            } else {
                DownloadManager.executeDownload(taskId)
            }
            
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "✅ Download completed: $taskId")
            Result.success()
            
        } catch (e: TimeoutCancellationException) {
            com.android.purebilibili.core.util.Logger.w("DownloadWorker", "⏱️ Download paused before Android foreground-service timeout: $taskId", e)
            DownloadManager.pauseForSystemForegroundTimeout(taskId)
            Result.success()

        } catch (e: kotlinx.coroutines.CancellationException) {
            if (DownloadManager.shouldTreatWorkerCancellationAsFinished(taskId)) {
                com.android.purebilibili.core.util.Logger.d("DownloadWorker", "⏸️ Download paused: $taskId")
                Result.success()
            } else {
                com.android.purebilibili.core.util.Logger.w("DownloadWorker", "🔁 Download interrupted, will retry: $taskId", e)
                withContext(NonCancellable) {
                    DownloadManager.markInterruptedForRetry(taskId, e.message ?: "下载被系统中断，等待重试")
                }
                Result.retry()
            }
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadWorker", "❌ Download failed: $taskId", e)
            
            // 更新任务状态
            DownloadManager.markFailed(taskId, e.message ?: "下载失败")
            Result.success()
        } finally {
            progressJob.cancel()
        }
    }
    
    // CoroutineWorker 要求的抽象实现；实际渲染走带进度的重载
    override suspend fun getForegroundInfo(): ForegroundInfo = getForegroundInfo(0f)

    private suspend fun getForegroundInfo(progress: Float): ForegroundInfo {
        val task = DownloadManager.tasks.value[inputData.getString(KEY_TASK_ID)]
        val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(task?.title?.takeIf { it.isNotBlank() } ?: "下载中...")
            .setContentText("下载中 $percent%")
            .setProgress(100, percent, percent <= 0)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val serviceType = resolveDownloadForegroundServiceType(Build.VERSION.SDK_INT)
        return if (serviceType != null) {
            ForegroundInfo(foregroundNotificationId(inputData.getString(KEY_TASK_ID)), notification, serviceType)
        } else {
            ForegroundInfo(foregroundNotificationId(inputData.getString(KEY_TASK_ID)), notification)
        }
    }
}
