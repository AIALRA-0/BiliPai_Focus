package com.android.purebilibili.feature.settings

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.purebilibili.app.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

private const val APP_UPDATE_WORK_NAME = "app_update_download"
private const val APP_UPDATE_WORK_TAG = "app_update_download"
private const val KEY_ASSET_NAME = "asset_name"
private const val KEY_ASSET_URL = "asset_url"
private const val KEY_ASSET_SIZE = "asset_size"
private const val KEY_EXPECTED_SHA256 = "expected_sha256"
private const val KEY_RELEASE_VERSION = "release_version"
private const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
private const val KEY_TOTAL_BYTES = "total_bytes"
private const val KEY_FILE_PATH = "file_path"
private const val KEY_ERROR_MESSAGE = "error_message"

internal class AppUpdateDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val assetName = inputData.getString(KEY_ASSET_NAME)?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure(errorData("更新安装包名称缺失"))
        val assetUrl = inputData.getString(KEY_ASSET_URL)?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure(errorData("更新下载地址缺失"))
        val expectedSha256 = inputData.getString(KEY_EXPECTED_SHA256)
            ?.lowercase()
            ?.takeIf(String::isNotBlank)
        val declaredSize = inputData.getLong(KEY_ASSET_SIZE, 0L).coerceAtLeast(0L)
        val updateDir = resolveAppUpdateCacheDir(applicationContext.cacheDir).apply { mkdirs() }
        val outputFile = File(updateDir, sanitizeAppUpdateFileName(assetName))
        val partFile = File(updateDir, "${sanitizeAppUpdateFileName(assetName)}.part")

        try {
            setForeground(createForegroundInfo())
            val initialBytes = partFile.length().coerceAtLeast(0L)
            var connection = openDownloadConnection(assetUrl, initialBytes)
            var responseCode = connection.responseCode
            if (responseCode == 416 && initialBytes > 0L) {
                connection.disconnect()
                partFile.delete()
                connection = openDownloadConnection(assetUrl, existingBytes = 0L)
                responseCode = connection.responseCode
            }
            val canResume = responseCode == HttpURLConnection.HTTP_PARTIAL
            if (responseCode !in 200..299) {
                connection.disconnect()
                throw IOException("更新下载失败: HTTP $responseCode")
            }

            val existingBytes = if (canResume) initialBytes else 0L
            if (!canResume && partFile.exists()) partFile.delete()
            val responseBytes = connection.contentLengthLong.coerceAtLeast(0L)
            val totalBytes = when {
                canResume && responseBytes > 0L -> existingBytes + responseBytes
                responseBytes > 0L -> responseBytes
                else -> declaredSize
            }
            setProgress(progressData(existingBytes, totalBytes))

            try {
                connection.inputStream.use { input ->
                    FileOutputStream(partFile, canResume).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloadedBytes = existingBytes
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            setProgress(progressData(downloadedBytes, totalBytes))
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (expectedSha256 != null) {
                val actualSha256 = sha256(partFile)
                if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                    partFile.delete()
                    return@withContext Result.failure(errorData("安装包校验失败，已删除下载文件"))
                }
            }
            if (outputFile.exists()) outputFile.delete()
            check(partFile.renameTo(outputFile)) { "无法完成安装包落盘" }
            Result.success(
                Data.Builder()
                    .putString(KEY_FILE_PATH, outputFile.absolutePath)
                    .putLong(KEY_DOWNLOADED_BYTES, outputFile.length())
                    .putLong(KEY_TOTAL_BYTES, totalBytes.coerceAtLeast(outputFile.length()))
                    .build()
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Result.retry()
        } catch (error: Exception) {
            Result.failure(errorData(error.message ?: "更新下载失败"))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("正在下载应用更新")
            .setContentText("下载完成后可在应用内安装")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        val notificationId = APP_UPDATE_WORK_NAME.hashCode()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun openDownloadConnection(url: String, existingBytes: Long): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "BiliPai-AppUpdate")
            if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
        }
    }

    private fun progressData(downloadedBytes: Long, totalBytes: Long): Data = Data.Builder()
        .putLong(KEY_DOWNLOADED_BYTES, downloadedBytes.coerceAtLeast(0L))
        .putLong(KEY_TOTAL_BYTES, totalBytes.coerceAtLeast(0L))
        .build()

    private fun errorData(message: String): Data = Data.Builder()
        .putString(KEY_ERROR_MESSAGE, message)
        .build()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

internal object AppUpdateDownloadCoordinator {
    fun enqueue(
        context: Context,
        asset: AppUpdateAsset,
        releaseVersion: String,
        expectedSha256: String?
    ) {
        val request = OneTimeWorkRequestBuilder<AppUpdateDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(KEY_ASSET_NAME, asset.name)
                    .putString(KEY_ASSET_URL, asset.downloadUrl)
                    .putLong(KEY_ASSET_SIZE, asset.sizeBytes)
                    .putString(KEY_EXPECTED_SHA256, expectedSha256.orEmpty())
                    .putString(KEY_RELEASE_VERSION, releaseVersion)
                    .build()
            )
            .addTag(APP_UPDATE_WORK_TAG)
            .addTag(releaseTag(releaseVersion))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            APP_UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, assetName: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(APP_UPDATE_WORK_NAME)
        val updateDir = resolveAppUpdateCacheDir(context.cacheDir)
        val safeName = sanitizeAppUpdateFileName(assetName)
        File(updateDir, "$safeName.part").delete()
        File(updateDir, safeName).delete()
    }

    suspend fun readState(
        context: Context,
        releaseVersion: String,
        checksumProvided: Boolean
    ): AppUpdateDownloadState =
        withContext(Dispatchers.IO) {
            val workInfo = runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .getWorkInfosForUniqueWork(APP_UPDATE_WORK_NAME)
                    .get()
                    .firstOrNull()
            }.getOrNull() ?: return@withContext AppUpdateDownloadState()
            if (releaseTag(releaseVersion) !in workInfo.tags) {
                return@withContext AppUpdateDownloadState()
            }
            workInfo.toAppUpdateDownloadState(checksumProvided)
        }
}

private fun releaseTag(releaseVersion: String): String = "$APP_UPDATE_WORK_TAG:$releaseVersion"

private fun WorkInfo.toAppUpdateDownloadState(checksumProvided: Boolean): AppUpdateDownloadState {
    val data = if (state == WorkInfo.State.SUCCEEDED || state == WorkInfo.State.FAILED) outputData else progress
    val downloadedBytes = data.getLong(KEY_DOWNLOADED_BYTES, 0L)
    val totalBytes = data.getLong(KEY_TOTAL_BYTES, 0L)
    val status = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> AppUpdateDownloadStatus.QUEUED
        WorkInfo.State.RUNNING -> AppUpdateDownloadStatus.DOWNLOADING
        WorkInfo.State.SUCCEEDED -> AppUpdateDownloadStatus.COMPLETED
        WorkInfo.State.FAILED -> AppUpdateDownloadStatus.FAILED
        WorkInfo.State.CANCELLED -> AppUpdateDownloadStatus.IDLE
    }
    return AppUpdateDownloadState(
        status = status,
        progress = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        filePath = data.getString(KEY_FILE_PATH),
        errorMessage = data.getString(KEY_ERROR_MESSAGE),
        checksumProvided = checksumProvided,
    )
}

internal fun sanitizeAppUpdateFileName(name: String): String =
    name.replace(Regex("[^A-Za-z0-9._-]"), "_")
