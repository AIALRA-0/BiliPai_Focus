@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.download

import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

internal data class HttpDownloadAssetRequest(
    val url: String,
    val outputFile: File,
    val headers: Map<String, String>,
    val parallelThresholdBytes: Long = 8L * 1024L * 1024L,
    val maxParallelChunks: Int = 4
)

internal data class HttpDownloadAssetResult(
    val totalBytes: Long,
    val downloadedBytes: Long,
    val segmentCount: Int
)

internal class ResumableAssetDownloader(
    private val client: OkHttpClient
) {
    suspend fun download(
        request: HttpDownloadAssetRequest,
        ensureActive: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): HttpDownloadAssetResult = withContext(Dispatchers.IO) {
        ensureActive()
        request.outputFile.parentFile?.mkdirs()
        val probe = probe(request)
        if (probe.totalBytes > 0L && request.outputFile.exists()) {
            val outputBytes = request.outputFile.length()
            if (shouldReuseCompletedAsset(outputBytes, probe.totalBytes)) {
                onProgress(probe.totalBytes, probe.totalBytes)
                return@withContext HttpDownloadAssetResult(
                    totalBytes = probe.totalBytes,
                    downloadedBytes = probe.totalBytes,
                    segmentCount = 1
                )
            }
            if (shouldDiscardCompletedAsset(outputBytes, probe.totalBytes)) {
                request.outputFile.delete()
            }
        }

        if (shouldUseParallelChunks(
                totalBytes = probe.totalBytes,
                supportsRanges = probe.supportsRanges,
                thresholdBytes = request.parallelThresholdBytes,
                maxChunks = request.maxParallelChunks
            )
        ) {
            downloadChunked(request, probe, ensureActive, onProgress)
        } else {
            downloadSingle(request, probe, ensureActive, onProgress)
        }
    }

    private fun probe(request: HttpDownloadAssetRequest): DownloadRangeProbe {
        val head = dataSource(request)
        try {
            head.open(DataSpec.Builder().setUri(request.url).setHttpMethod(DataSpec.HTTP_METHOD_HEAD).build())
            val totalBytes = head.header("Content-Length")?.toLongOrNull() ?: 0L
            if (totalBytes > 0L) {
                return DownloadRangeProbe(totalBytes, head.header("Accept-Ranges").equals("bytes", ignoreCase = true))
            }
        } catch (_: HttpDataSource.InvalidResponseCodeException) {
            // 部分源站不支持 HEAD，改用一个字节的 GET 探测。
        } finally {
            head.close()
        }
        val probe = dataSource(request)
        try {
            probe.open(DataSpec.Builder().setUri(request.url).setLength(1).build())
            parseDownloadContentRange(probe.header("Content-Range"))?.let { return it }
            return DownloadRangeProbe(
                totalBytes = probe.header("Content-Length")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                supportsRanges = false,
            )
        } finally {
            probe.close()
        }
    }

    private fun downloadSingle(
        request: HttpDownloadAssetRequest,
        probe: DownloadRangeProbe,
        ensureActive: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): HttpDownloadAssetResult {
        val partFile = partFileFor(request.outputFile)
        val existingBytes = partFile.takeIf(File::exists)?.length() ?: 0L
        val plan = resolveResumableDownloadPlan(
            existingBytes = existingBytes,
            totalBytes = probe.totalBytes,
            acceptsRanges = probe.supportsRanges
        )
        if (plan.alreadyComplete) {
            promotePartFile(partFile, request.outputFile)
            onProgress(plan.totalBytes, plan.totalBytes)
            return HttpDownloadAssetResult(plan.totalBytes, plan.totalBytes, 1)
        }

        if (!plan.append && partFile.exists()) {
            partFile.delete()
        }

        val source = dataSource(request)
        try {
            // DataSpec 负责 Range 及源站忽略 Range 时的跳过，保留本地临时文件布局。
            val remainingBytes = source.open(DataSpec.Builder().setUri(request.url)
                .setPosition(plan.rangeStartBytes).build())
            val totalBytes = probe.totalBytes.takeIf { it > 0L }
                ?: (remainingBytes.takeIf { it != C.LENGTH_UNSET.toLong() }?.plus(plan.initialDownloadedBytes) ?: 0L)
            var downloadedBytes = plan.initialDownloadedBytes
            onProgress(downloadedBytes, totalBytes)
            FileOutputStream(partFile, plan.append).use { output ->
                val buffer = ByteArray(DEFAULT_DOWNLOAD_BUFFER_BYTES)
                while (true) {
                    ensureActive()
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    onProgress(downloadedBytes, totalBytes)
                }
            }
            if (totalBytes > 0L && downloadedBytes != totalBytes) {
                throw IOException("下载长度不匹配: $downloadedBytes/$totalBytes")
            }
            promotePartFile(partFile, request.outputFile)
            return HttpDownloadAssetResult(totalBytes, downloadedBytes, 1)
        } finally {
            source.close()
        }
    }

    private suspend fun downloadChunked(
        request: HttpDownloadAssetRequest,
        probe: DownloadRangeProbe,
        ensureActive: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): HttpDownloadAssetResult = coroutineScope {
        val partFile = partFileFor(request.outputFile)
        val ranges = resolveDownloadChunkRanges(
            totalBytes = probe.totalBytes,
            maxChunks = request.maxParallelChunks
        )
        val progressBytes = AtomicLong(
            ranges.sumOf { range ->
                completedChunkBytes(chunkFileFor(partFile, range.index), range.sizeBytes)
            }
        )
        val semaphore = Semaphore(request.maxParallelChunks)
        ranges.map { range ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    downloadChunk(request, partFile, range, probe.totalBytes, progressBytes, ensureActive, onProgress)
                }
            }
        }.awaitAll()

        ensureActive()
        ranges.forEach { range ->
            validateChunkFile(chunkFileFor(partFile, range.index), range.sizeBytes)
        }
        FileOutputStream(partFile, false).use { output ->
            ranges.forEach { range ->
                chunkFileFor(partFile, range.index).inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
        ranges.forEach { chunkFileFor(partFile, it.index).delete() }
        promotePartFile(partFile, request.outputFile)
        onProgress(probe.totalBytes, probe.totalBytes)
        HttpDownloadAssetResult(
            totalBytes = probe.totalBytes,
            downloadedBytes = probe.totalBytes,
            segmentCount = ranges.size
        )
    }

    private fun downloadChunk(
        request: HttpDownloadAssetRequest,
        partFile: File,
        range: DownloadChunkRange,
        totalBytes: Long,
        progressBytes: AtomicLong,
        ensureActive: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        val chunkFile = chunkFileFor(partFile, range.index)
        val rawExistingBytes = chunkFile.takeIf(File::exists)?.length() ?: 0L
        val existingBytes = if (rawExistingBytes > range.sizeBytes) {
            chunkFile.delete()
            0L
        } else {
            rawExistingBytes
        }
        if (existingBytes >= range.sizeBytes) {
            onProgress(progressBytes.get(), totalBytes)
            return
        }

        val start = range.startBytes + existingBytes
        val source = dataSource(request)
        try {
            source.open(DataSpec.Builder().setUri(request.url).setPosition(start)
                .setLength(range.endBytesInclusive - start + 1L).build())
            if (!isValidPartialContentResponse(
                    responseCode = source.responseCode,
                    contentRange = source.header("Content-Range"),
                    requestedStart = start,
                    requestedEnd = range.endBytesInclusive,
                )
            ) throw IOException("分片响应不匹配: HTTP ${source.responseCode}")
            FileOutputStream(chunkFile, existingBytes > 0L).use { output ->
                val buffer = ByteArray(DEFAULT_DOWNLOAD_BUFFER_BYTES)
                while (true) {
                    ensureActive()
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    output.write(buffer, 0, read)
                    onProgress(progressBytes.addAndGet(read.toLong()), totalBytes)
                }
            }
            validateChunkFile(chunkFile, range.sizeBytes)
        } finally {
            source.close()
        }
    }

    private fun dataSource(request: HttpDownloadAssetRequest): HttpDataSource =
        OkHttpDataSource.Factory(client).setDefaultRequestProperties(request.headers).createDataSource()

    private fun HttpDataSource.header(name: String): String? =
        responseHeaders.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

    private fun partFileFor(outputFile: File): File = File(outputFile.parentFile, "${outputFile.name}.part")

    private fun chunkFileFor(partFile: File, index: Int): File = File(partFile.parentFile, "${partFile.name}.chunk$index")

    private fun completedChunkBytes(chunkFile: File, expectedBytes: Long): Long {
        val length = chunkFile.takeIf(File::exists)?.length() ?: return 0L
        return resolveCompletedChunkBytes(length, expectedBytes)
    }

    private fun validateChunkFile(chunkFile: File, expectedBytes: Long) {
        val actualBytes = chunkFile.takeIf(File::exists)?.length() ?: 0L
        if (!isDownloadChunkComplete(actualBytes, expectedBytes)) {
            throw IOException("分片下载未完成: ${chunkFile.name} $actualBytes/$expectedBytes")
        }
    }

    private fun promotePartFile(partFile: File, outputFile: File) {
        if (outputFile.exists()) outputFile.delete()
        if (!partFile.renameTo(outputFile)) {
            partFile.copyTo(outputFile, overwrite = true)
            partFile.delete()
        }
    }

    private companion object {
        private const val DEFAULT_DOWNLOAD_BUFFER_BYTES = 64 * 1024
    }
}
