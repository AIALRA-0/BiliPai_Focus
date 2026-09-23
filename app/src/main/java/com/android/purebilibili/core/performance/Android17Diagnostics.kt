package com.android.purebilibili.core.performance

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.ProfilingManager
import android.os.ProfilingResult
import android.os.ProfilingTrigger
import androidx.annotation.RequiresApi
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.Logger
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

internal data class ProfilingArtifactSnapshot(
    val path: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)

internal fun resolveProcessExitReasonLabel(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_EXIT_SELF -> "应用主动退出"
    ApplicationExitInfo.REASON_SIGNALED -> "收到系统信号"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "系统内存不足"
    ApplicationExitInfo.REASON_CRASH -> "Java/Kotlin 崩溃"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native 崩溃"
    ApplicationExitInfo.REASON_ANR -> "应用无响应（ANR）"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "初始化失败"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "权限变更"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "资源占用过高"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "用户请求停止"
    ApplicationExitInfo.REASON_USER_STOPPED -> "用户强制停止"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "依赖进程退出"
    ApplicationExitInfo.REASON_OTHER -> "其他系统原因"
    ApplicationExitInfo.REASON_FREEZER -> "系统冻结进程"
    ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "应用状态变更"
    ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "应用更新"
    else -> "未知原因($reason)"
}

internal fun isAbnormalProcessExitReason(reason: Int): Boolean = reason in setOf(
    ApplicationExitInfo.REASON_CRASH,
    ApplicationExitInfo.REASON_CRASH_NATIVE,
    ApplicationExitInfo.REASON_ANR,
    ApplicationExitInfo.REASON_LOW_MEMORY,
    ApplicationExitInfo.REASON_SIGNALED,
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
)

/**
 * 解析 API 31+ 的 ApplicationExitInfo 子原因（subReason）。
 */
internal fun resolveProcessExitSubReasonLabel(subReason: Int): String = when (subReason) {
    0 -> "无"
    1 -> "等待调试器"
    2 -> "缓存进程过多"
    3 -> "空进程过多"
    4 -> "清理空进程"
    5 -> "缓存占用过大"
    6 -> "系统内存压力"
    7 -> "CPU 占用过高"
    8 -> "系统更新完成"
    9 -> "清理后台进程"
    10 -> "应用包更新"
    11 -> "广播未及时交付"
    12 -> "冻结中 Binder IOCTL 违规"
    13 -> "冻结中 Binder 事务违规"
    14 -> "强制停止"
    15 -> "移除任务卡片"
    16 -> "停止应用"
    17 -> "终止 PID"
    18 -> "终止 UID"
    19 -> "空闲强制待机"
    20 -> "隔离进程无需求"
    else -> "子原因($subReason)"
}

/**
 * ApplicationExitInfo.getSubReason() 在公版 SDK 中被 @hide，通过反射在 Android 12+ (API 31+) 安全提取。
 */
internal fun extractApplicationExitSubReason(exitInfo: ApplicationExitInfo): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
    return runCatching {
        val method = exitInfo.javaClass.getMethod("getSubReason")
        (method.invoke(exitInfo) as? Number)?.toInt() ?: 0
    }.getOrDefault(0)
}

/**
 * 读取系统记录的历史退出 Trace（含 Native 崩溃的 Tombstone 或 ANR 堆栈）。
 */
@RequiresApi(Build.VERSION_CODES.R)
internal fun readProcessExitTrace(exitInfo: ApplicationExitInfo, maxLines: Int = 120): String? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            exitInfo.reason == ApplicationExitInfo.REASON_CRASH_NATIVE
        ) {
            // Android 12+ returns protobuf here, not a UTF-8 tombstone. Preserve the bytes
            // so debuggerd's tombstone schema can decode the crashing thread offline.
            return@runCatching exitInfo.traceInputStream?.use { stream ->
                encodeNativeExitTrace(stream)
            }
        }
        exitInfo.traceInputStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val lines = mutableListOf<String>()
            var count = 0
            while (count < maxLines) {
                val line = reader.readLine() ?: break
                lines.add(line)
                count++
            }
            if (lines.isEmpty()) null else lines.joinToString("\n")
        }
    }.getOrNull()
}

/**
 * 包装系统记录的历史进程异常退出（Native 崩溃、ANR、系统信号等）。
 * 清空在当前启动进程中产生的无意义虚假 Java 栈（如 PureApplication.onCreate），
 * 并挂载真实的系统 Tombstone 或 ANR Trace。
 */
class AbnormalProcessExitException(
    message: String,
    val nativeTrace: String? = null
) : RuntimeException(message) {
    init {
        // 清空由当前进程启动合成异常时产生的虚假 Java 堆栈，避免排查时误导用户
        stackTrace = emptyArray()
    }

    override fun printStackTrace(s: PrintWriter) {
        s.println(super.toString())
        if (!nativeTrace.isNullOrBlank()) {
            s.println()
            s.println("----- 系统异常回溯 (Tombstone / Trace) -----")
            s.println(nativeTrace)
        }
    }

    override fun printStackTrace(s: PrintStream) {
        s.println(super.toString())
        if (!nativeTrace.isNullOrBlank()) {
            s.println()
            s.println("----- 系统异常回溯 (Tombstone / Trace) -----")
            s.println(nativeTrace)
        }
    }
}

internal fun selectProfilingArtifactPathsToKeep(
    artifacts: List<ProfilingArtifactSnapshot>,
    maxArtifacts: Int,
    maxTotalBytes: Long
): Set<String> {
    var keptBytes = 0L
    return artifacts
        .sortedByDescending(ProfilingArtifactSnapshot::lastModifiedMillis)
        .filter { artifact ->
            val canKeep = artifact.sizeBytes >= 0L &&
                artifact.sizeBytes <= maxTotalBytes - keptBytes
            if (canKeep) keptBytes += artifact.sizeBytes
            canKeep
        }
        .take(maxArtifacts.coerceAtLeast(0))
        .mapTo(linkedSetOf(), ProfilingArtifactSnapshot::path)
}

internal object Android17Diagnostics {
    private const val TAG = "Android17Diagnostics"
    private const val PROFILE_DIRECTORY = "android17_profiles"
    private const val MAX_ARTIFACTS = 3
    private const val MAX_TOTAL_BYTES = 128L * 1024L * 1024L
    private const val PREFS_NAME = "android17_diagnostics"
    private const val KEY_LAST_EXIT_TIMESTAMP = "last_exit_timestamp"
    private const val KEY_LAST_CRASH_SNAPSHOT_TIMESTAMP = "last_crash_snapshot_timestamp"

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var initialized = false
    private var resultConsumer: Consumer<ProfilingResult>? = null

    fun initialize(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT < 37) return
        updateEnabled(context.applicationContext, enabled)
    }

    fun updateEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT < 37) {
            if (!enabled) deleteRetainedArtifacts(appContext)
            return
        }
        if (enabled) {
            enableApi37(appContext)
        } else {
            disableApi37(appContext)
        }
    }

    fun retainedArtifacts(context: Context): List<File> =
        profileDirectory(context).listFiles()
            ?.filter(File::isFile)
            ?.sortedByDescending(File::lastModified)
            .orEmpty()

    fun copyRetainedArtifactsTo(context: Context, targetDirectory: File): List<File> {
        targetDirectory.mkdirs()
        return retainedArtifacts(context).mapNotNull { source ->
            runCatching {
                val target = File(targetDirectory, source.name)
                source.inputStream().buffered().use { input ->
                    target.outputStream().buffered().use(input::copyTo)
                }
                target
            }.onFailure { Logger.w(TAG, "Failed to prepare profiling artifact for export", it) }
                .getOrNull()
        }
    }

    /**
     * 返回最近的进程退出记录，补足 Java 未捕获异常处理器看不到的 Native 崩溃、
     * ANR、LMK 和信号退出。只包含系统诊断字段，不读取或导出用户内容。
     */
    fun recentProcessExitSummaries(context: Context, maxCount: Int = 3): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || maxCount <= 0) return emptyList()
        val manager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return runCatching {
            manager.getHistoricalProcessExitReasons(context.packageName, 0, maxCount)
                .take(maxCount)
                .mapIndexed { index, exitInfo ->
                    buildString {
                        append(index + 1)
                        append(". ")
                        append(formatter.format(Date(exitInfo.timestamp)))
                        append(" · ")
                        append(resolveProcessExitReasonLabel(exitInfo.reason))
                        append(" · status=")
                        append(exitInfo.status)
                        append(" · importance=")
                        append(exitInfo.importance)
                        append(" · pss=")
                        append(exitInfo.pss)
                        append("KB · rss=")
                        append(exitInfo.rss)
                        append("KB")
                        val subReasonCode = extractApplicationExitSubReason(exitInfo)
                        if (subReasonCode != 0) {
                            append(" · subReason=")
                            append(resolveProcessExitSubReasonLabel(subReasonCode))
                        }
                        exitInfo.description
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?.take(240)
                            ?.let { description ->
                                append(" · description=")
                                append(description.replace('\n', ' ').replace('\r', ' '))
                            }
                    }
                }
        }.onFailure { error ->
            Logger.w(TAG, "Failed to read historical process exit reasons", error)
        }.getOrDefault(emptyList())
    }

    /**
     * Persists the previous process' abnormal termination when the Java uncaught handler could
     * not run (native crash, ANR, LMK, or a signal). ApplicationExitInfo is available to ordinary
     * apps through ActivityManager; root is not required. The timestamp gate prevents repeating
     * the same snapshot on every subsequent launch.
     */
    fun persistLatestAbnormalExitSnapshot(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val appContext = context.applicationContext
        // A Java uncaught handler may already have written a richer snapshot. Preserve it and
        // only synthesize one when the process died before that handler could run.
        if (Logger.hasPendingCrashSnapshot(appContext)) return
        val manager = appContext.getSystemService(ActivityManager::class.java) ?: return
        runCatching {
            val exitInfo = manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 8)
                .firstOrNull { info ->
                    isAbnormalProcessExitReason(info.reason)
                } ?: return
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getLong(KEY_LAST_CRASH_SNAPSHOT_TIMESTAMP, 0L) == exitInfo.timestamp) return
            prefs.edit().putLong(KEY_LAST_CRASH_SNAPSHOT_TIMESTAMP, exitInfo.timestamp).apply()

            val reason = resolveProcessExitReasonLabel(exitInfo.reason)
            val subReasonCode = extractApplicationExitSubReason(exitInfo)
            val subReason = if (subReasonCode != 0) {
                resolveProcessExitSubReasonLabel(subReasonCode)
            } else null
            val trace = readProcessExitTrace(exitInfo)

            val details = buildString {
                append("系统记录的上次异常退出：")
                append(reason)
                append("；status=")
                append(exitInfo.status)
                append("；importance=")
                append(exitInfo.importance)
                append("；pss=")
                append(exitInfo.pss)
                append("KB；rss=")
                append(exitInfo.rss)
                append("KB")
                subReason?.let {
                    append("；subReason=")
                    append(it)
                }
                exitInfo.description?.trim()?.takeIf(String::isNotEmpty)?.let {
                    append("；description=")
                    append(it.take(240))
                }
            }
            Logger.persistCrashSnapshot(
                AbnormalProcessExitException(
                    message = details,
                    nativeTrace = trace
                )
            )
        }.onFailure { error ->
            Logger.w(TAG, "Failed to persist historical process exit snapshot", error)
        }
    }

    @RequiresApi(37)
    private fun enableApi37(context: Context) {
        if (initialized) return
        val manager = context.getSystemService(ProfilingManager::class.java) ?: return
        val consumer = Consumer<ProfilingResult> { result ->
            executor.execute { handleProfilingResult(context, result) }
        }
        resultConsumer = consumer
        manager.registerForAllProfilingResults(executor, consumer)
        manager.clearProfilingTriggers()
        manager.addProfilingTriggers(
            listOf(
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_OOM)
                    .setRateLimitingPeriodHours(24)
                    .build(),
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_ANOMALY)
                    .setRateLimitingPeriodHours(24)
                    .build(),
                ProfilingTrigger.Builder(ProfilingTrigger.TRIGGER_TYPE_KILL_EXCESSIVE_CPU_USAGE)
                    .setRateLimitingPeriodHours(24)
                    .build()
            )
        )
        initialized = true
        executor.execute { recordMemoryLimiterExitIfPresent(context) }
        Logger.i(TAG, "Android 17 profiling triggers enabled")
    }

    @RequiresApi(37)
    private fun disableApi37(context: Context) {
        context.getSystemService(ProfilingManager::class.java)?.let { manager ->
            resultConsumer?.let(manager::unregisterForAllProfilingResults)
            manager.clearProfilingTriggers()
        }
        resultConsumer = null
        initialized = false
        executor.execute { deleteRetainedArtifacts(context) }
        Logger.i(TAG, "Android 17 profiling triggers disabled and local artifacts cleared")
    }

    @RequiresApi(37)
    private fun handleProfilingResult(context: Context, result: ProfilingResult) {
        if (result.errorCode != ProfilingResult.ERROR_NONE) {
            Logger.w(TAG, "Profiling failed: code=${result.errorCode}, trigger=${result.triggerType}")
            return
        }
        val sourcePath = result.resultFilePath?.takeIf(String::isNotBlank) ?: return
        val source = File(sourcePath).takeIf(File::isFile) ?: return
        val safeExtension = source.extension.take(16).filter { it.isLetterOrDigit() || it == '-' }
        val targetName = buildString {
            append("profile_")
            append(result.triggerType)
            append('_')
            append(System.currentTimeMillis())
            if (safeExtension.isNotBlank()) append('.').append(safeExtension)
        }
        val directory = profileDirectory(context).apply { mkdirs() }
        val temporary = File(directory, "$targetName.tmp")
        val target = File(directory, targetName)
        runCatching {
            source.inputStream().buffered().use { input ->
                temporary.outputStream().buffered().use(input::copyTo)
            }
            check(temporary.renameTo(target)) { "Unable to finalize profiling artifact" }
            if (source.absolutePath != target.absolutePath) source.delete()
            trimRetainedArtifacts(directory)
            CrashReporter.setCustomKey("android17_profile_trigger", result.triggerType)
            CrashReporter.setCustomKey("android17_profile_timestamp_ms", target.lastModified())
            CrashReporter.setCustomKey("android17_profile_size_bytes", target.length())
            CrashReporter.setCustomKey("android17_profile_app_version", BuildConfig.VERSION_NAME)
            val activityManager = context.getSystemService(ActivityManager::class.java)
            CrashReporter.setCustomKey(
                "android17_profile_memory_class_mb",
                activityManager?.memoryClass ?: 0
            )
            Logger.i(TAG, "Stored private profiling artifact: trigger=${result.triggerType}, bytes=${target.length()}")
        }.onFailure { error ->
            temporary.delete()
            Logger.w(TAG, "Failed to retain profiling artifact", error)
        }
    }

    @RequiresApi(37)
    private fun recordMemoryLimiterExitIfPresent(context: Context) {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val latest = manager.getHistoricalProcessExitReasons(context.packageName, 0, 10)
            .asSequence()
            .filter { it.reason == ApplicationExitInfo.REASON_OTHER }
            .firstOrNull { it.description?.contains("MemoryLimiter:AnonSwap", ignoreCase = true) == true }
            ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (latest.timestamp <= prefs.getLong(KEY_LAST_EXIT_TIMESTAMP, 0L)) return
        prefs.edit().putLong(KEY_LAST_EXIT_TIMESTAMP, latest.timestamp).apply()
        CrashReporter.setCustomKey("android17_memory_limiter_exit", true)
        CrashReporter.setCustomKey("android17_memory_limiter_pss_kb", latest.pss)
        CrashReporter.setCustomKey("android17_memory_limiter_rss_kb", latest.rss)
        Logger.w(
            TAG,
            "Previous process was stopped by Android 17 memory limiter; pssKb=${latest.pss}, rssKb=${latest.rss}"
        )
    }

    private fun trimRetainedArtifacts(directory: File) {
        val files = directory.listFiles()?.filter(File::isFile).orEmpty()
        val pathsToKeep = selectProfilingArtifactPathsToKeep(
            artifacts = files.map { file ->
                ProfilingArtifactSnapshot(file.absolutePath, file.length(), file.lastModified())
            },
            maxArtifacts = MAX_ARTIFACTS,
            maxTotalBytes = MAX_TOTAL_BYTES
        )
        files.forEach { file ->
            if (file.absolutePath !in pathsToKeep) file.delete()
        }
    }

    private fun deleteRetainedArtifacts(context: Context) {
        profileDirectory(context).listFiles()?.forEach(File::delete)
        profileDirectory(context).delete()
    }

    private fun profileDirectory(context: Context): File = File(context.filesDir, PROFILE_DIRECTORY)
}
