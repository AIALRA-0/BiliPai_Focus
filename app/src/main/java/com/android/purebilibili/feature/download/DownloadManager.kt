package com.android.purebilibili.feature.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.FileOutputStream

private const val DEFAULT_MUXER_SAMPLE_BUFFER_SIZE_BYTES = 1024 * 1024
private val PARTIAL_CONTENT_RANGE_REGEX = Regex("""bytes (\d+)-(\d+)/(?:\d+|\*)""")

internal fun isValidPartialContentResponse(
    responseCode: Int,
    contentRange: String?,
    requestedStart: Long,
    requestedEnd: Long
): Boolean {
    if (responseCode != 206) return false
    val match = PARTIAL_CONTENT_RANGE_REGEX.matchEntire(contentRange?.trim().orEmpty()) ?: return false
    val actualStart = match.groupValues[1].toLongOrNull() ?: return false
    val actualEnd = match.groupValues[2].toLongOrNull() ?: return false
    return actualStart == requestedStart && actualEnd == requestedEnd
}

internal fun resolveMuxerSampleBufferSize(
    maxInputSizes: List<Int>,
    defaultBytes: Int = DEFAULT_MUXER_SAMPLE_BUFFER_SIZE_BYTES
): Int {
    val advertisedMax = maxInputSizes.filter { it > 0 }.maxOrNull() ?: 0
    return maxOf(defaultBytes, advertisedMax)
}

/**
 *  视频下载管理器
 * 
 * 功能：
 * - 管理下载任务队列
 * - 支持断点续传
 * - 音视频分离下载后合并
 * - 持久化存储下载状态
 */
object DownloadManager {
    
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 下载任务状态
    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadTask>> = _tasks.asStateFlow()
    
    // 🔧 [移除] downloadJobs 已被 WorkManager 替代
    
    // 下载目录
    private var downloadDir: File? = null
    private var tasksFile: File? = null
    private var appContext: Context? = null

    private fun safeExternalFilesRoot(context: Context): File? {
        return runCatching { context.getExternalFilesDir(null) }
            .onFailure { error ->
                com.android.purebilibili.core.util.Logger.w(
                    "DownloadManager",
                    "⚠️ External files dir unavailable, falling back to internal storage",
                    error
                )
            }
            .getOrNull()
    }
    
    /**
     * 初始化（在 Application 中调用）
     */
    fun init(context: Context) {
        appContext = context.applicationContext

        val initialPath = com.android.purebilibili.core.store.SettingsManager.getDownloadPathSync(context)
        downloadDir = resolveDownloadDir(context, initialPath)
        tasksFile = File(context.filesDir, "download_tasks.json")
        loadTasks()
        scheduleNextQueuedDownload()

        scope.launch {
            com.android.purebilibili.core.store.SettingsManager.getDownloadPath(context)
                .collect { customPath ->
                    downloadDir = resolveDownloadDir(context, customPath)
                }
        }
    }
    
    /**
     * 获取下载目录
     */
    fun getDownloadDir(): File = downloadDir ?: throw IllegalStateException("DownloadManager not initialized")
    
    /**
     * 添加下载任务
     */
    fun addTask(task: DownloadTask): Boolean {
        val existing = _tasks.value[task.id]
        if (existing != null && existing.status != DownloadStatus.FAILED && existing.status != DownloadStatus.PAUSED) {
            return false // 已在下载中
        }
        
        val newTask = task.copy(status = DownloadStatus.QUEUED, errorMessage = null)
        _tasks.value = _tasks.value + (task.id to newTask)
        saveTasks()
        scheduleNextQueuedDownload()
        return true
    }
    
    /**
     * 开始下载（使用 WorkManager 调度）
     */
    fun startDownload(taskId: String) {
        val task = _tasks.value[taskId] ?: return
        if (isDownloadTaskActive(task) || task.status == DownloadStatus.QUEUED) return
        
        updateTask(taskId) { it.copy(status = DownloadStatus.QUEUED, errorMessage = null) }
        scheduleNextQueuedDownload()
    }
    
    /**
     * 🔧 [新增] 执行下载（由 WorkManager 调用）
     * @throws Exception 下载失败时抛出异常
     */
    suspend fun executeDownload(taskId: String) {
        val task = _tasks.value[taskId] 
            ?: throw IllegalStateException("任务不存在: $taskId")
        if (task.status != DownloadStatus.PENDING && task.status != DownloadStatus.DOWNLOADING) {
            throw CancellationException("任务未处于可执行状态: ${task.status}")
        }
        downloadTask(taskId)
    }
    
    /**
     * 🔧 [新增] 标记下载失败（由 WorkManager 调用）
     */
    fun markFailed(taskId: String, errorMessage: String) {
        updateTask(taskId) {
            it.copy(status = DownloadStatus.FAILED, errorMessage = errorMessage)
        }
        scheduleNextQueuedDownload()
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        val context = appContext ?: return
        // 🔧 [修复] 取消 WorkManager 任务
        DownloadWorker.cancel(context, taskId)
        updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED) }
        scheduleNextQueuedDownload()
    }
    
    /**
     * 删除任务
     */
    fun removeTask(taskId: String) {
        val context = appContext ?: return
        // 🔧 取消 WorkManager 任务
        DownloadWorker.cancel(context, taskId)
        
        val task = _tasks.value[taskId]
        if (task != null) {
            val cleanupTargets = resolveDownloadCleanupTargets(
                taskId = taskId,
                task = task,
                taskDirectoryPath = getTaskDir(taskId).absolutePath
            )
            cleanupTargets.filePaths.forEach { path ->
                runCatching { File(path).delete() }
            }
            task.exportedFileUri?.let { deleteExportedFile(context, it) }
            val taskDir = File(cleanupTargets.taskDirectoryPath)
            if (taskDir.exists() && taskDir.isDirectory && taskDir.listFiles().isNullOrEmpty()) {
                taskDir.delete()
            }
        }
        
        _tasks.value = _tasks.value - taskId
        saveTasks()
        scheduleNextQueuedDownload()
    }

    fun updatePlaybackPosition(taskId: String, positionMs: Long) {
        updateTask(taskId) { current ->
            current.copy(lastPlaybackPositionMs = positionMs.coerceAtLeast(0L))
        }
    }
    
    /**
     * 获取任务状态
     */
    fun getVideoTask(bvid: String, cid: Long): DownloadTask? {
        return _tasks.value.values
            .filter { !it.isAudioOnly && it.bvid == bvid && it.cid == cid }
            .sortedWith(
                compareByDescending<DownloadTask> { it.isComplete }
                    .thenByDescending { it.isDownloading }
                    .thenByDescending { it.createdAt }
            )
            .firstOrNull()
    }
    
    /**
     * 执行下载
     */
    private suspend fun downloadTask(taskId: String) {
        val task = _tasks.value[taskId] ?: throw IllegalStateException("任务不存在: $taskId")
        updateTask(task.id) { it.copy(status = DownloadStatus.DOWNLOADING) }
        
        val videoFile = getVideoFile(task.id)
        val audioFile = getAudioFile(task.id)
        val outputFile = getOutputFile(task.id)
        val coverFile = getCoverFile(task.id)
        
        // 🖼️ 0. 下载封面图片（用于离线显示）
        try {
            if (!coverFile.exists() || coverFile.length() <= 0L) {
                downloadCoverImage(task.cover, coverFile)
                com.android.purebilibili.core.util.Logger.d("DownloadManager", "🖼️ Cover downloaded: ${coverFile.name}")
            }
            updateTask(task.id) { it.copy(localCoverPath = coverFile.absolutePath) }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.w("DownloadManager", "⚠️ Cover download failed, will use network URL", e)
        }
        
        // 1. 下载视频流 (如果不是仅音频模式)
        if (!task.isAudioOnly) {
            downloadFile(task.videoUrl, videoFile, task.id) { progress ->
                // 如果不仅音频，总进度 = (video + audio) / 2
                updateTask(task.id, persist = false) {
                    it.copy(videoProgress = progress, progress = (progress + it.audioProgress) / 2)
                }
            }
        } else {
             updateTask(task.id, persist = false) { it.copy(videoProgress = 1f) }
        }
        
        // 2. 下载音频流
        downloadFile(task.audioUrl, audioFile, task.id) { progress ->
            updateTask(task.id, persist = false) {
                val totalProgress = if (task.isAudioOnly) progress else (it.videoProgress + progress) / 2
                it.copy(audioProgress = progress, progress = totalProgress)
            }
        }
        
        // 3. 合并音视频 (或直接处理音频)
        updateTask(task.id) { it.copy(status = DownloadStatus.MERGING, progress = 0.95f) }
        if (outputFile.exists()) {
            outputFile.delete()
        }
        
        if (task.isAudioOnly) {
            // 仅音频模式：直接将音频文件作为输出（或者是转换为 m4a/mp3，这里直接用音频流）
            // B站音频流通常是 m4s (AAC) 或 m4a。直接改名或复制。
            // 为了兼容性，封装进 MP4 容器（即使只有音频轨）通常更安全，或者直接复制。
            // 简单起见，尝试直接复制。注意后缀名问题。现在 outputFile 是 .mp4。
            // 使用 MediaMuxer 仅封装音频轨也是一种方法，能保证 metadata 正确。
            mergeVideoAudio(null, audioFile, outputFile)
        } else {
            mergeVideoAudio(videoFile, audioFile, outputFile)
        }
        
        // 4. 清理临时文件
        videoFile.delete()
        audioFile.delete()
        
        val exportedUri = appContext?.let { context ->
            exportToUserSelectedDirectory(
                context = context,
                sourceFile = outputFile,
                title = task.title,
                qualityDesc = task.qualityDesc,
                isAudioOnly = task.isAudioOnly
            )
        }

        // 5. 更新状态
        updateTask(task.id) { 
            it.copy(
                status = DownloadStatus.COMPLETED, 
                progress = 1f,
                filePath = outputFile.absolutePath,
                exportedFileUri = exportedUri,
                fileSize = outputFile.length()
            ) 
        }
        scheduleNextQueuedDownload()
        
        com.android.purebilibili.core.util.Logger.d("DownloadManager", "✅ Download completed: ${task.title} (AudioOnly: ${task.isAudioOnly})")
    }
    
    /**
     * 单线程下载单个文件，支持基于已有临时文件的续传。
     */
    private suspend fun downloadFile(
        url: String, 
        file: File, 
        taskId: String,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        ensureTaskCanRun(taskId)

        // 获取用户 Cookie
        val sessData = com.android.purebilibili.core.store.TokenManager.sessDataCache ?: ""
        val biliJct = com.android.purebilibili.core.store.TokenManager.csrfCache ?: ""
        val buvid3 = com.android.purebilibili.core.store.TokenManager.buvid3Cache ?: ""
        val cookieString = buildString {
            if (sessData.isNotEmpty()) append("SESSDATA=$sessData; ")
            if (biliJct.isNotEmpty()) append("bili_jct=$biliJct; ")
            if (buvid3.isNotEmpty()) append("buvid3=$buvid3; ")
        }
        
        // 首先获取文件大小
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .header("Cookie", cookieString)
            .build()
        
        val headResponse = client.newCall(headRequest).execute()
        val totalBytes = headResponse.header("Content-Length")?.toLongOrNull() ?: 0L
        val acceptRanges = headResponse.header("Accept-Ranges").equals("bytes", ignoreCase = true)
        headResponse.close()

        val plan = resolveResumableDownloadPlan(
            existingBytes = file.takeIf(File::exists)?.length() ?: 0L,
            totalBytes = totalBytes,
            acceptsRanges = acceptRanges
        )
        if (plan.alreadyComplete) {
            onProgress(1f)
            return@withContext
        }

        downloadFileSingleThread(url, file, cookieString, taskId, plan, onProgress)
    }
    
    /**
     * 单线程下载（降级方案）
     */
    private suspend fun downloadFileSingleThread(
        url: String,
        file: File,
        cookieString: String,
        taskId: String,
        plan: ResumableDownloadPlan,
        onProgress: (Float) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        if (!plan.append && file.exists() && file.length() > 0L) {
            file.delete()
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://www.bilibili.com")
            .header("Cookie", cookieString)
        if (plan.append) {
            requestBuilder.header("Range", "bytes=${plan.rangeStartBytes}-")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }

        if (plan.append && response.code != 206) {
            response.close()
            file.delete()
            val restartedPlan = plan.copy(
                append = false,
                rangeStartBytes = 0L,
                initialDownloadedBytes = 0L
            )
            downloadFileSingleThread(url, file, cookieString, taskId, restartedPlan, onProgress)
            return@withContext
        }

        val body = response.body ?: throw Exception("Empty response")
        val totalResponseBytes = plan.totalBytes.takeIf { it > 0L } ?: run {
            val bodyBytes = body.contentLength()
            if (bodyBytes > 0L) bodyBytes + plan.initialDownloadedBytes else 0L
        }
        var downloadedBytes = plan.initialDownloadedBytes

        if (totalResponseBytes > 0L) {
            onProgress(downloadedBytes.toFloat() / totalResponseBytes.toFloat())
        }

        FileOutputStream(file, plan.append).use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    ensureTaskCanRun(taskId)
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalResponseBytes > 0L) {
                        onProgress(downloadedBytes.toFloat() / totalResponseBytes.toFloat())
                    }
                }
            }
        }

        if (totalResponseBytes > 0L && downloadedBytes < totalResponseBytes) {
            throw IOException("下载未完成: $downloadedBytes/$totalResponseBytes")
        }
    }

    
    /**
     * 使用 Android MediaMuxer 合并音视频
     * 将分离的视频流和音频流合并为完整的 MP4 文件
     */
    @android.annotation.SuppressLint("WrongConstant")
    private suspend fun mergeVideoAudio(video: File?, audio: File, output: File) = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("DownloadManager", " Starting MediaMuxer merge...")
            
            // 创建 MediaMuxer
            val muxer = android.media.MediaMuxer(
                output.absolutePath,
                android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            
            // 提取视频轨道
            // 提取视频轨道 (仅当 video 不为空时)
            val videoExtractor = android.media.MediaExtractor()
            var videoTrackMaxInputSize = -1
            if (video != null) {
                videoExtractor.setDataSource(video.absolutePath)
            }
            var videoTrackIndex = -1
            var videoMuxerTrackIndex = -1
            
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoMuxerTrackIndex = muxer.addTrack(format)
                    videoTrackIndex = i
                    if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        videoTrackMaxInputSize = format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                    }
                    break
                }
            }
            
            // 如果仅音频且 video 为空，跳过视频轨检查
            if (video != null && videoTrackIndex == -1) {
                 // Video file exists but no track found?
            }
            
            // 提取音频轨道
            val audioExtractor = android.media.MediaExtractor()
            audioExtractor.setDataSource(audio.absolutePath)
            var audioTrackIndex = -1
            var audioMuxerTrackIndex = -1
            var audioTrackMaxInputSize = -1
            
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioMuxerTrackIndex = muxer.addTrack(format)
                    audioTrackIndex = i
                    if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        audioTrackMaxInputSize = format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                    }
                    break
                }
            }
            
            if ((video != null && videoTrackIndex == -1) || audioTrackIndex == -1) {
                com.android.purebilibili.core.util.Logger.e("DownloadManager", " Failed to find video or audio track")
                // 降级：直接复制视频
                if (video != null) video.copyTo(output, overwrite = true)
                videoExtractor.release()
                audioExtractor.release()
                return@withContext
            }
            
            // 开始合并
            muxer.start()
            
            val buffer = java.nio.ByteBuffer.allocate(
                resolveMuxerSampleBufferSize(
                    maxInputSizes = listOf(videoTrackMaxInputSize, audioTrackMaxInputSize)
                )
            )
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            
            // 写入视频数据 (如果有)
            if (video != null && videoTrackIndex != -1 && videoMuxerTrackIndex != -1) {
                while (true) {
                    buffer.clear()
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                    bufferInfo.flags = videoExtractor.sampleFlags
                    
                    muxer.writeSampleData(videoMuxerTrackIndex, buffer, bufferInfo)
                    videoExtractor.advance()
                }
            }
            
            // 写入音频数据
            while (true) {
                buffer.clear()
                val sampleSize = audioExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                bufferInfo.flags = audioExtractor.sampleFlags
                
                muxer.writeSampleData(audioMuxerTrackIndex, buffer, bufferInfo)
                audioExtractor.advance()
            }
            
            // 清理
            videoExtractor.release()
            audioExtractor.release()
            muxer.stop()
            muxer.release()
            
            com.android.purebilibili.core.util.Logger.d("DownloadManager", " MediaMuxer merge completed: ${output.name}")
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", " MediaMuxer merge failed", e)
            // 降级：直接复制视频
            video?.copyTo(output, overwrite = true)
        }
    }
    
    private fun getTaskDir(taskId: String): File {
        val task = _tasks.value[taskId]
        val externalRoot = appContext?.let(::safeExternalFilesRoot)
        val appScopedRoot = externalRoot?.absolutePath
        val legacyCustomPath = if (appScopedRoot != null) {
            sanitizeLegacyCustomPath(task?.customSaveDir, appScopedRoot)
        } else {
            null
        }

        val dir = legacyCustomPath?.let { File(it) } ?: getDownloadDir()
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getVideoFile(taskId: String) = File(getTaskDir(taskId), "${taskId}_video.m4s")
    private fun getAudioFile(taskId: String) = File(getTaskDir(taskId), "${taskId}_audio.m4s")
    private fun getOutputFile(taskId: String): File {
        val task = _tasks.value[taskId]
        val extension = if (task?.isAudioOnly == true) "m4a" else "mp4"
        return File(getTaskDir(taskId), "${taskId}.$extension")
    }
    private fun getCoverFile(taskId: String) = File(getTaskDir(taskId), "${taskId}_cover.jpg")
    
    /**
     * 🖼️ [新增] 下载封面图片
     */
    private suspend fun downloadCoverImage(coverUrl: String, file: File) = withContext(Dispatchers.IO) {
        val url = coverUrl.let {
            if (it.startsWith("http://")) it.replace("http://", "https://") else it
        }
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Referer", "https://www.bilibili.com")
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }
        
        response.body?.byteStream()?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Empty response body")
    }
    
    private fun updateTask(
        taskId: String,
        persist: Boolean = true,
        update: (DownloadTask) -> DownloadTask
    ) {
        val current = _tasks.value[taskId] ?: return
        val updated = update(current)
        _tasks.value = _tasks.value + (taskId to updated)
        if (persist && shouldPersistDownloadTaskUpdate(current, updated)) {
            saveTasks()
        }
    }

    private fun enqueueDownload(taskId: String) {
        val context = appContext ?: return
        updateTask(taskId) { it.copy(status = DownloadStatus.PENDING, errorMessage = null) }
        DownloadWorker.enqueue(context, taskId)
    }

    private fun scheduleNextQueuedDownload() {
        val nextTaskId = resolveNextQueuedDownloadTaskId(_tasks.value.values) ?: return
        enqueueDownload(nextTaskId)
    }

    private fun ensureTaskCanRun(taskId: String) {
        val task = _tasks.value[taskId] ?: throw CancellationException("任务已不存在")
        if (!isDownloadTaskActive(task)) {
            throw CancellationException("任务已停止: ${task.status}")
        }
    }
    
    private fun loadTasks() {
        try {
            val file = tasksFile ?: return
            if (file.exists()) {
                val content = file.readText()
                val list = json.decodeFromString<List<DownloadTask>>(content)
                _tasks.value = list
                    .map(::normalizeRestoredDownloadTask)
                    .associateBy { it.id }
            }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "Failed to load tasks", e)
        }
    }
    
    private fun saveTasks() {
        try {
            val file = tasksFile ?: return
            val content = json.encodeToString(_tasks.value.values.toList())
            val parent = file.parentFile ?: return
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val tempFile = File(parent, "${file.name}.tmp")
            tempFile.writeText(content)
            if (!tempFile.renameTo(file)) {
                file.writeText(content)
                tempFile.delete()
            }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "Failed to save tasks", e)
        }
    }
    
    /**
     * 解析下载目录 (已提取为辅助方法)
     */
    private fun resolveDownloadDir(context: Context, customPath: String?): File {
        return try {
            val resolvedDir = resolveManagedDownloadDirectory(
                filesDir = context.filesDir,
                externalFilesRoot = safeExternalFilesRoot(context),
                customPath = customPath
            )
            if (customPath.isNullOrBlank()) {
                com.android.purebilibili.core.util.Logger.d(
                    "DownloadManager",
                    "✅ Resolved managed dir: ${resolvedDir.absolutePath}"
                )
            } else if (resolvedDir.absolutePath == customPath) {
                com.android.purebilibili.core.util.Logger.d(
                    "DownloadManager",
                    "✅ Resolved custom dir: ${resolvedDir.absolutePath}"
                )
            } else {
                com.android.purebilibili.core.util.Logger.w(
                    "DownloadManager",
                    "⚠️ Custom path unavailable/writable, using fallback: $customPath -> ${resolvedDir.absolutePath}"
                )
            }
            resolvedDir
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "❌ Failed to resolve custom path: $customPath", e)
            File(context.filesDir, "downloads").apply { mkdirs() }
        }
    }
    
    /**
     * 🖼️ [新增] 保存图片到相册
     */
    suspend fun saveImageToGallery(context: Context, url: String, title: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 下载图片
             val request = Request.Builder()
                .url(url.replace("http://", "https://"))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext false
            val bytes = body.bytes()
            
            // 2. 插入 MediaStore
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$title.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
                }
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) 
                ?: return@withContext false
                
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            
            return@withContext true
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadManager", "Failed to save image", e)
            return@withContext false
        }
    }

    private fun deleteExportedFile(context: Context, exportedUri: String) {
        runCatching {
            DocumentFile.fromSingleUri(context, Uri.parse(exportedUri))?.delete()
        }.onFailure { e ->
            com.android.purebilibili.core.util.Logger.w(
                "DownloadManager",
                "⚠️ Failed to delete exported file: $exportedUri",
                e
            )
        }
    }

    private fun exportToUserSelectedDirectory(
        context: Context,
        sourceFile: File,
        title: String,
        qualityDesc: String,
        isAudioOnly: Boolean
    ): String? {
        val treeUriString = com.android.purebilibili.core.store.SettingsManager
            .getDownloadExportTreeUriSync(context)
            ?: return null

        return runCatching {
            val treeUri = Uri.parse(treeUriString)
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@runCatching null

            val extension = if (isAudioOnly) "m4a" else "mp4"
            val mimeType = if (isAudioOnly) "audio/mp4" else "video/mp4"
            val displayName = buildSafeExportDisplayName(
                title = title,
                qualityDesc = qualityDesc,
                extension = extension
            )

            treeDoc.findFile(displayName)?.delete()
            val target = treeDoc.createFile(mimeType, displayName)
                ?: return@runCatching null

            sourceFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(target.uri, "w")?.use { output ->
                    input.copyTo(output)
                } ?: return@runCatching null
            }
            target.uri.toString()
        }.onFailure { e ->
            com.android.purebilibili.core.util.Logger.w(
                "DownloadManager",
                "⚠️ Failed to export to SAF directory",
                e
            )
        }.getOrNull()
    }
}
