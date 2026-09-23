package com.android.purebilibili.feature.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val APP_UPDATE_DOWNLOAD_CONNECT_TIMEOUT_MS = 10_000
private const val APP_UPDATE_DOWNLOAD_READ_TIMEOUT_MS = 30_000

internal fun resolveAppUpdateCacheDir(cacheDir: File): File = File(cacheDir, "app_update")

internal suspend fun downloadAppUpdateApk(
    context: Context,
    asset: AppUpdateAsset,
    onStateChange: (AppUpdateDownloadState) -> Unit = {}
): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
        var state = startAppUpdateDownload(totalBytes = asset.sizeBytes)
        onStateChange(state)

        val updateDir = resolveAppUpdateCacheDir(context.cacheDir).apply { mkdirs() }
        val outputFile = File(updateDir, sanitizeAppUpdateFileName(asset.name.ifBlank { "BiliPai-update.apk" }))

        val connection = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = APP_UPDATE_DOWNLOAD_CONNECT_TIMEOUT_MS
            readTimeout = APP_UPDATE_DOWNLOAD_READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "BiliPai-AppUpdate")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("更新下载失败: HTTP $responseCode")
            }

            val resolvedTotalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: asset.sizeBytes
            state = startAppUpdateDownload(totalBytes = resolvedTotalBytes)
            onStateChange(state)

            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        state = updateAppUpdateDownloadProgress(
                            current = state,
                            downloadedBytes = downloadedBytes
                        )
                        onStateChange(state)
                    }
                }
            }

            val completedState = completeAppUpdateDownload(
                current = state,
                filePath = outputFile.absolutePath
            )
            onStateChange(completedState)
            outputFile
        } catch (t: CancellationException) {
            throw t
        } catch (t: Exception) {
            onStateChange(
                failAppUpdateDownload(
                    current = state,
                    errorMessage = t.message ?: "更新下载失败"
                )
            )
            throw t
        } finally {
            connection.disconnect()
        }
    }
}
