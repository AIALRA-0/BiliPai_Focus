package com.android.purebilibili.feature.download

import android.content.Context
import android.os.Build
import androidx.work.*
import com.android.purebilibili.app.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.Dispatchers
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
        setForeground(getForegroundInfo())
        
        try {
            // 执行下载
            DownloadManager.executeDownload(taskId)
            
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "✅ Download completed: $taskId")
            Result.success()
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "⏸️ Download paused: $taskId")
            Result.success()
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadWorker", "❌ Download failed: $taskId", e)
            
            // 更新任务状态
            DownloadManager.markFailed(taskId, e.message ?: "下载失败")
            Result.success()
        }
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        // 创建前台通知（Android 12+ WorkManager 要求）
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext, 
            DOWNLOAD_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("下载中...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        val serviceType = resolveDownloadForegroundServiceType(Build.VERSION.SDK_INT)
        return if (serviceType != null) {
            ForegroundInfo(notificationId, notification, serviceType)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
