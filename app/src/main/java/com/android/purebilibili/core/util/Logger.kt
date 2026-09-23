// 文件路径: core/util/Logger.kt
package com.android.purebilibili.core.util

import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.performance.Android17Diagnostics
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private const val LOG_DIRECTORY_NAME = "logs"
private const val RUNTIME_LOG_FILE_NAME = "runtime.log"
private const val BASIC_LOG_FILE_NAME = "basic.log"
private const val CRASH_SNAPSHOT_FILE_NAME = "last_crash_log.txt"
private const val CRASH_SNAPSHOT_MARKER_FILE_NAME = "pending_crash.marker"
private const val DOWNLOAD_LOG_RELATIVE_PATH = "Download/BiliPai/logs"
internal const val ENHANCED_DIAGNOSTIC_LOG_PREFS_NAME = "diagnostic_logging"
internal const val ENHANCED_DIAGNOSTIC_LOG_PREF_KEY = "enhanced_enabled"

internal fun resolveLogPersistenceDir(baseDir: File): File = File(baseDir, LOG_DIRECTORY_NAME)

internal fun resolveRuntimeLogFile(baseDir: File): File =
    File(resolveLogPersistenceDir(baseDir), RUNTIME_LOG_FILE_NAME)

internal fun resolveBasicLogFile(baseDir: File): File =
    File(resolveLogPersistenceDir(baseDir), BASIC_LOG_FILE_NAME)

internal fun resolveCrashSnapshotFile(baseDir: File): File =
    File(resolveLogPersistenceDir(baseDir), CRASH_SNAPSHOT_FILE_NAME)

internal fun resolveCrashSnapshotMarkerFile(baseDir: File): File =
    File(resolveLogPersistenceDir(baseDir), CRASH_SNAPSHOT_MARKER_FILE_NAME)

internal fun resolvePlayerDiagnosticExportFileName(
    exportedAtMillis: Long
): String {
    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return "player_diagnostic_${formatter.format(Date(exportedAtMillis))}.txt"
}

internal fun shouldEnableVerboseRuntimeLogs(
    isDebugBuild: Boolean,
    verboseDebugLogsEnabled: Boolean,
    enhancedDiagnosticLoggingEnabled: Boolean,
): Boolean = (isDebugBuild && verboseDebugLogsEnabled) || enhancedDiagnosticLoggingEnabled

internal fun shouldEmitVerboseLogcat(
    isDebugBuild: Boolean,
    verboseDebugLogsEnabled: Boolean,
): Boolean = isDebugBuild && verboseDebugLogsEnabled

internal fun shouldCaptureRuntimeLogEntry(
    level: String,
    verboseRuntimeLogsEnabled: Boolean
): Boolean = when (level) {
    "W", "E" -> true
    else -> verboseRuntimeLogsEnabled
}

internal fun shouldPersistRuntimeLogEntry(
    level: String,
    verboseRuntimeLogPersistenceEnabled: Boolean
): Boolean = level == "W" || level == "E" || verboseRuntimeLogPersistenceEnabled

internal fun hasExportableDiagnostics(
    logCount: Int,
    hasCrashSnapshot: Boolean,
    processExitCount: Int,
    profilingArtifactCount: Int
): Boolean = logCount > 0 || hasCrashSnapshot || processExitCount > 0 || profilingArtifactCount > 0

/** Limits bytes, including multibyte UTF-8 messages and a single oversized entry. */
internal fun appendRollingDiagnosticLog(file: File, text: String, maxBytes: Int) {
    require(maxBytes > 0)
    file.parentFile?.mkdirs()
    val incoming = text.toByteArray(Charsets.UTF_8)
    if (file.length() + incoming.size <= maxBytes) {
        file.appendBytes(incoming)
        return
    }
    val existing = if (file.isFile) file.readBytes() else byteArrayOf()
    val combined = existing + incoming
    var start = (combined.size - maxBytes / 2).coerceAtLeast(0)
    // Do not begin a retained file in the middle of a UTF-8 code point.
    while (start < combined.size && (combined[start].toInt() and 0xC0) == 0x80) start++
    file.writeBytes(combined.copyOfRange(start, combined.size))
}

internal fun sanitizeLogMessage(message: String): String =
    LogCollector.sanitizeMessage(message)

internal fun resolveLogArtifactDirsToClear(
    filesDir: File,
    cacheDir: File
): List<File> = listOf(
    resolveLogPersistenceDir(filesDir),
    resolveLogPersistenceDir(cacheDir)
).distinctBy { it.absolutePath }

internal fun hasPendingCrashSnapshot(
    markerExists: Boolean,
    snapshotExists: Boolean
): Boolean = markerExists && snapshotExists

internal fun buildCrashSnapshotContent(
    throwable: Throwable,
    entries: List<LogCollector.LogEntry>,
    exportedAtMillis: Long,
    appVersionName: String,
    versionCode: Int,
    manufacturer: String,
    model: String,
    androidRelease: String,
    apiLevel: Int,
    buildType: String = "unknown",
    buildCommit: String = "unknown"
): String {
    val headerDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    return buildString {
        appendLine("========================================")
        appendLine("BiliPai 崩溃日志快照")
        appendLine("========================================")
        appendLine("生成时间: ${headerDateFormat.format(Date(exportedAtMillis))}")
        appendLine("应用版本: $appVersionName ($versionCode)")
        appendLine("构建: $buildType / $buildCommit")
        appendLine("设备信息: $manufacturer $model")
        appendLine("Android版本: $androidRelease (API $apiLevel)")
        appendLine("异常类型: ${throwable.javaClass.simpleName}")
        appendLine("异常信息: ${sanitizeLogMessage(throwable.message.orEmpty())}")
        appendLine("========================================")
        appendLine()
        appendLine("----- Throwable -----")
        appendLine(sanitizeLogMessage(throwable.stackTraceToString()))
        appendLine("----- Recent Logs -----")
        entries.forEach { appendLine(it.format()) }
    }
}

/**
 *  统一日志工具类
 * 
 * 默认在私有目录滚动保留警告、错误及最小启动诊断；用户主动开启增强诊断后，另行
 * 滚动保存脱敏后的 Debug/Info 日志，支持导出供用户反馈。
 */
object Logger {

    /** Fixed startup stage names only; no links, account identifiers or user input. */
    fun recordStartupStage(stage: String) {
        LogCollector.add(
            level = "I",
            tag = "StartupDiagnostics",
            message = "stage=$stage, app=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}), " +
                "build=${BuildConfig.BUILD_TYPE}/${BuildConfig.BUILD_COMMIT_SHA.take(12)}, " +
                "android=${android.os.Build.VERSION.RELEASE}(API ${android.os.Build.VERSION.SDK_INT}), " +
                "device=${android.os.Build.MANUFACTURER}/${android.os.Build.MODEL}",
            persistToDisk = true,
            basicDiagnostic = true,
        )
    }

    private val debugVerboseLogsEnabled = shouldEmitVerboseLogcat(
        isDebugBuild = BuildConfig.DEBUG,
        verboseDebugLogsEnabled = BuildConfig.ENABLE_VERBOSE_DEBUG_LOGS,
    )
    @Volatile
    private var enhancedDiagnosticLoggingEnabled = false
    @Volatile
    private var diagnosticSessionRecorded = false

    @PublishedApi
    internal fun areVerboseRuntimeLogsEnabled(): Boolean = shouldEnableVerboseRuntimeLogs(
        isDebugBuild = BuildConfig.DEBUG,
        verboseDebugLogsEnabled = BuildConfig.ENABLE_VERBOSE_DEBUG_LOGS,
        enhancedDiagnosticLoggingEnabled = enhancedDiagnosticLoggingEnabled,
    )

    @PublishedApi
    internal fun isVerboseRuntimeLogPersistenceEnabled(): Boolean =
        enhancedDiagnosticLoggingEnabled ||
            (debugVerboseLogsEnabled && BuildConfig.ENABLE_VERBOSE_RUNTIME_LOG_PERSISTENCE)

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        LogCollector.init(applicationContext)
        enhancedDiagnosticLoggingEnabled = applicationContext
            .getSharedPreferences(ENHANCED_DIAGNOSTIC_LOG_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(ENHANCED_DIAGNOSTIC_LOG_PREF_KEY, false)
        if (enhancedDiagnosticLoggingEnabled) {
            recordDiagnosticSessionStart(applicationContext)
        }
    }

    fun configureEnhancedDiagnosticLogging(context: Context, enabled: Boolean) {
        val applicationContext = context.applicationContext
        LogCollector.init(applicationContext)
        enhancedDiagnosticLoggingEnabled = enabled
        if (enabled) {
            recordDiagnosticSessionStart(applicationContext)
        } else {
            diagnosticSessionRecorded = false
            LogCollector.clearRuntimeDiagnostics()
        }
    }

    private fun recordDiagnosticSessionStart(context: Context) {
        if (diagnosticSessionRecorded) return
        synchronized(this) {
            if (diagnosticSessionRecorded) return
            diagnosticSessionRecorded = true
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val configuration = context.resources.configuration
        LogCollector.add(
            level = "I",
            tag = "Diagnostics",
            message = "增强诊断已开启；仅保存在应用私有目录，导出前会再次脱敏，滚动上限=256KB",
            persistToDisk = true,
        )
        LogCollector.add(
            level = "I",
            tag = "Diagnostics",
            message = "app=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}), " +
                "build=${BuildConfig.BUILD_COMMIT_SHA.take(12)}, debug=${BuildConfig.DEBUG}",
            persistToDisk = true,
        )
        LogCollector.add(
            level = "I",
            tag = "Diagnostics",
            message = "device=${android.os.Build.MANUFACTURER}/${android.os.Build.MODEL}, " +
                "android=${android.os.Build.VERSION.RELEASE}(API ${android.os.Build.VERSION.SDK_INT}), " +
                "abis=${android.os.Build.SUPPORTED_ABIS.joinToString()}, locale=${configuration.locales[0]}, " +
                "densityDpi=${configuration.densityDpi}, memoryClassMb=${activityManager?.memoryClass}, " +
                "lowRam=${activityManager?.isLowRamDevice}",
            persistToDisk = true,
        )
    }
    
    /**
     * Debug 日志 - Debug 包输出到 Logcat；正式版仅在用户主动开启增强诊断后收集
     */
    fun d(tag: String, message: String) {
        val verboseRuntimeLogsEnabled = areVerboseRuntimeLogsEnabled()
        if (!verboseRuntimeLogsEnabled) return
        if (debugVerboseLogsEnabled) Log.d(tag, message)
        if (shouldCaptureRuntimeLogEntry("D", verboseRuntimeLogsEnabled)) {
            LogCollector.add(
                level = "D",
                tag = tag,
                message = message,
                persistToDisk = shouldPersistRuntimeLogEntry(
                    "D",
                    isVerboseRuntimeLogPersistenceEnabled(),
                )
            )
        }
    }

    inline fun d(tag: String, message: () -> String) {
        if (!areVerboseRuntimeLogsEnabled()) return
        d(tag, message())
    }
    
    /**
     * Info 日志 - Debug 包输出到 Logcat；正式版仅在用户主动开启增强诊断后收集
     */
    fun i(tag: String, message: String) {
        val verboseRuntimeLogsEnabled = areVerboseRuntimeLogsEnabled()
        if (!verboseRuntimeLogsEnabled) return
        if (debugVerboseLogsEnabled) Log.i(tag, message)
        if (shouldCaptureRuntimeLogEntry("I", verboseRuntimeLogsEnabled)) {
            LogCollector.add(
                level = "I",
                tag = tag,
                message = message,
                persistToDisk = shouldPersistRuntimeLogEntry(
                    "I",
                    isVerboseRuntimeLogPersistenceEnabled(),
                )
            )
        }
    }

    inline fun i(tag: String, message: () -> String) {
        if (!areVerboseRuntimeLogsEnabled()) return
        i(tag, message())
    }
    
    /**
     * Warning 日志 - 始终输出
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else message
        
        if (BuildConfig.DEBUG && throwable != null) {
            Log.w(tag, message, throwable)
        } else if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        } else {
            Log.w(tag, sanitizeLogMessage(fullMessage))
        }
        val verboseRuntimeLogsEnabled = areVerboseRuntimeLogsEnabled()
        if (shouldCaptureRuntimeLogEntry("W", verboseRuntimeLogsEnabled)) {
            LogCollector.add(
                level = "W",
                tag = tag,
                message = fullMessage,
                persistToDisk = shouldPersistRuntimeLogEntry(
                    "W",
                    isVerboseRuntimeLogPersistenceEnabled(),
                )
            )
        }
    }
    
    /**
     * Error 日志 - 始终输出
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else message
        
        if (BuildConfig.DEBUG && throwable != null) {
            Log.e(tag, message, throwable)
        } else if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        } else {
            Log.e(tag, sanitizeLogMessage(fullMessage))
        }
        val verboseRuntimeLogsEnabled = areVerboseRuntimeLogsEnabled()
        if (shouldCaptureRuntimeLogEntry("E", verboseRuntimeLogsEnabled)) {
            LogCollector.add(
                level = "E",
                tag = tag,
                message = fullMessage,
                persistToDisk = shouldPersistRuntimeLogEntry(
                    "E",
                    isVerboseRuntimeLogPersistenceEnabled(),
                )
            )
        }
    }

    fun persistCrashSnapshot(throwable: Throwable) {
        LogCollector.persistCrashSnapshot(throwable)
    }

    fun getPendingCrashSnapshotPath(context: Context): String? {
        init(context)
        return LogCollector.getPendingCrashSnapshotFile()?.absolutePath
    }

    /** Returns whether a crash snapshot is waiting for the user to review/share. */
    fun hasPendingCrashSnapshot(context: Context): Boolean =
        getPendingCrashSnapshotPath(context) != null

    fun clearPendingCrashSnapshot(context: Context) {
        init(context)
        LogCollector.clearPendingCrashSnapshot()
    }

    fun sharePendingCrashSnapshot(context: Context): Boolean {
        init(context)
        return LogCollector.sharePendingCrashSnapshot(context)
    }

    fun getPrivateLogArtifactsSize(context: Context): Long {
        init(context)
        val persistedLogDir = resolveLogPersistenceDir(context.filesDir)
        return if (!persistedLogDir.exists()) {
            0L
        } else {
            persistedLogDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }
    }

    fun clearPrivateLogArtifacts(context: Context) {
        init(context)
        LogCollector.clear()
        LogCollector.clearPendingCrashSnapshot()
        resolveLogArtifactDirsToClear(
            filesDir = context.filesDir,
            cacheDir = context.cacheDir
        ).forEach { dir ->
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    fun exportPlayerDiagnostic(
        context: Context,
        content: String,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String? {
        init(context)
        val fileName = resolvePlayerDiagnosticExportFileName(exportedAtMillis)
        return LogCollector.saveTextArtifact(
            context = context,
            fileName = fileName,
            content = content,
            replaceExisting = false
        )
    }
}

/**
 *  日志收集器
 * 
 * 使用环形缓冲区保留最近 1000 条日志，支持导出分享
 */
object LogCollector {
    
    private const val MAX_ENTRIES = 1000
    private const val DUPLICATE_SUPPRESS_WINDOW_MS = 250L
    private const val MAX_PERSISTED_LOG_BYTES = 256 * 1024
    private const val MAX_BASIC_LOG_BYTES = 64 * 1024
    private const val MAX_LOG_MESSAGE_CHARS = 16 * 1024
    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val diskWriter = Executors.newSingleThreadExecutor()
    private var lastEntryFingerprint: String? = null
    private var lastEntryTimestamp: Long = 0L
    @Volatile
    private var appContext: Context? = null
    
    /**
     * 日志条目
     */
    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        fun format(): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            return "[$time] $level/$tag: $message"
        }
    }
    
    /**
     * 添加日志条目。敏感信息在进入内存和磁盘前即被移除，导出时再做一次纵深防御。
     */
    fun add(
        level: String,
        tag: String,
        message: String,
        persistToDisk: Boolean = true,
        basicDiagnostic: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        val sanitizedTag = sanitizeMessage(tag).take(80)
        val sanitizedMessage = sanitizeMessage(message).let { value ->
            if (value.length <= MAX_LOG_MESSAGE_CHARS) value
            else value.take(MAX_LOG_MESSAGE_CHARS) + "…[truncated]"
        }
        val fingerprint = "$level|$sanitizedTag|$sanitizedMessage"
        var entryToPersist: LogEntry? = null
        synchronized(lock) {
            // 高频重复日志直接抑制，避免日志风暴拖垮主线程
            if (fingerprint == lastEntryFingerprint &&
                now - lastEntryTimestamp <= DUPLICATE_SUPPRESS_WINDOW_MS) {
                lastEntryTimestamp = now
                return
            }

            lastEntryFingerprint = fingerprint
            lastEntryTimestamp = now

            entryToPersist = LogEntry(
                timestamp = now,
                level = level,
                tag = sanitizedTag,
                message = sanitizedMessage
            )
            buffer.addLast(entryToPersist)

            while (buffer.size > MAX_ENTRIES) {
                if (buffer.isNotEmpty()) {
                    buffer.pollFirst()
                } else {
                    break
                }
            }
        }

        if (persistToDisk) {
            entryToPersist?.let { appendEntryToRuntimeFile(it, basicDiagnostic) }
        }
    }

    fun init(context: Context) {
        if (appContext === context.applicationContext) return
        appContext = context.applicationContext
        runCatching {
            resolveLogPersistenceDir(context.filesDir).mkdirs()
        }.onFailure {
            Log.e("LogCollector", "初始化持久化日志目录失败", it)
        }
    }
    
    /**
     *  隐私脱敏：移除敏感信息
     * 
     * 覆盖范围：
     * - Cookie 值 (SESSDATA, bili_jct, DedeUserID 等)
     * - Token / Key (access_token, refresh_token 等)
     * - 用户标识 (mid, uid, 手机号, 邮箱)
     * - 网络信息 (IP 地址, MAC 地址)
     * - 文件路径 (可能包含用户名)
     * - 其他敏感参数
     */
    internal fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // ========== Cookie 脱敏 ==========
        sanitized = sanitized.replace(Regex("SESSDATA=[^;\\s]+"), "SESSDATA=***")
        sanitized = sanitized.replace(Regex("bili_jct=[^;\\s]+"), "bili_jct=***")
        sanitized = sanitized.replace(Regex("DedeUserID=[^;\\s]+"), "DedeUserID=***")
        sanitized = sanitized.replace(Regex("DedeUserID__ckMd5=[^;\\s]+"), "DedeUserID__ckMd5=***")
        sanitized = sanitized.replace(Regex("sid=[^;\\s]+"), "sid=***")
        sanitized = sanitized.replace(Regex("buvid3=[^;\\s]+"), "buvid3=***")
        sanitized = sanitized.replace(Regex("buvid4=[^;\\s]+"), "buvid4=***")
        sanitized = sanitized.replace(Regex("b_nut=[^;\\s]+"), "b_nut=***")
        sanitized = sanitized.replace(Regex("_uuid=[^;\\s]+"), "_uuid=***")
        
        // ========== Token / Key 脱敏 ==========
        sanitized = sanitized.replace(Regex("access_token=[^&\\s]+"), "access_token=***")
        sanitized = sanitized.replace(Regex("refresh_token=[^&\\s]+"), "refresh_token=***")
        sanitized = sanitized.replace(Regex("access_key=[^&\\s]+"), "access_key=***")
        sanitized = sanitized.replace(Regex("appkey=[^&\\s]+"), "appkey=***")
        sanitized = sanitized.replace(Regex("sign=[^&\\s]+"), "sign=***")
        sanitized = sanitized.replace(Regex("csrf=[^&\\s]+"), "csrf=***")
        sanitized = sanitized.replace(Regex("\"token\":\"[^\"]+\""), "\"token\":\"***\"")
        sanitized = sanitized.replace(Regex("\"csrf\":\"[^\"]+\""), "\"csrf\":\"***\"")
        sanitized = sanitized.replace(
            Regex("(?i)Authorization\\s*[:=]\\s*[^\\r\\n]+"),
            "Authorization: ***"
        )
        sanitized = sanitized.replace(Regex("Bearer\\s+[^\\s]+"), "Bearer ***")
        sanitized = sanitized.replace(
            Regex("(?i)(cookie|set-cookie)\\s*[:=]\\s*[^\\r\\n]+"),
            "$1: ***"
        )
        sanitized = sanitized.replace(
            Regex("(?i)(password|passwd|pwd|sms_code|captcha|challenge|validate)[=:]\\s*[^&\\s,}]+"),
            "$1=***"
        )
        
        // ========== 用户 ID 脱敏 ==========
        // Bilibili mid/uid (通常为 6-11 位数字，在特定上下文中)
        sanitized = sanitized.replace(Regex("mid[=:]\\s*\\d{4,}"), "mid=***")
        sanitized = sanitized.replace(Regex("\"mid\":\\s*\\d+"), "\"mid\":***")
        sanitized = sanitized.replace(Regex("uid[=:]\\s*\\d{4,}"), "uid=***")
        sanitized = sanitized.replace(Regex("\"uid\":\\s*\\d+"), "\"uid\":***")
        sanitized = sanitized.replace(Regex("vmid[=:]\\s*\\d+"), "vmid=***")
        
        // ========== 手机号脱敏 (11位中国手机号) ==========
        sanitized = sanitized.replace(Regex("\\b1[3-9]\\d{9}\\b"), "1**********")
        
        // ========== 邮箱脱敏 ==========
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) { 
            val email = it.value
            val atIndex = email.indexOf('@')
            if (atIndex > 2) {
                email.substring(0, 2) + "***" + email.substring(atIndex)
            } else {
                "***" + email.substring(atIndex)
            }
        }
        
        // ========== IP 地址脱敏 ==========
        // IPv4
        sanitized = sanitized.replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            val parts = it.value.split(".")
            if (parts.size == 4 && parts.all { p -> p.toIntOrNull() in 0..255 }) {
                "${parts[0]}.***.***.*"
            } else {
                it.value
            }
        }
        // IPv6 (简化处理)
        sanitized = sanitized.replace(Regex("\\b[0-9a-fA-F:]{15,}\\b"), "***:***:***")
        
        // ========== MAC 地址脱敏 ==========
        sanitized = sanitized.replace(Regex("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}"), "**:**:**:**:**:**")
        
        // ========== 文件路径脱敏 (隐藏用户名) ==========
        // Android 路径
        sanitized = sanitized.replace(Regex("/data/user/\\d+/[^/]+/"), "/data/user/0/***/")
        sanitized = sanitized.replace(Regex("/storage/emulated/\\d+/"), "/storage/emulated/0/")
        // 通用 home 目录
        sanitized = sanitized.replace(Regex("/home/[^/]+/"), "/home/***/")
        sanitized = sanitized.replace(Regex("/Users/[^/]+/"), "/Users/***/")
        
        // ========== 设备标识脱敏 ==========
        sanitized = sanitized.replace(Regex("device_id=[^&\\s]+"), "device_id=***")
        sanitized = sanitized.replace(Regex("\"device_id\":\"[^\"]+\""), "\"device_id\":\"***\"")
        sanitized = sanitized.replace(Regex("android_id=[^&\\s]+"), "android_id=***")
        sanitized = sanitized.replace(Regex("imei=[^&\\s]+"), "imei=***")
        
        // ========== 敏感 JSON 字段脱敏 ==========
        sanitized = sanitized.replace(Regex("\"face\":\"[^\"]+\""), "\"face\":\"***\"")
        sanitized = sanitized.replace(Regex("\"tel\":\"[^\"]+\""), "\"tel\":\"***\"")
        sanitized = sanitized.replace(Regex("\"name\":\"[^\"]{2,}\"")) {
            // 保留名字首字符
            val name = it.value
            val start = name.indexOf(":\"") + 2
            val end = name.lastIndexOf("\"")
            if (end > start + 1) {
                "\"name\":\"${name[start]}***\""
            } else {
                it.value
            }
        }
        
        // ========== 🎬 视频内容脱敏（保护用户观看记录隐私） ==========
        // 视频 BVID
        sanitized = sanitized.replace(Regex("BV[0-9A-Za-z]{10}"), "BV***")
        // 视频 AID/AV 号
        sanitized = sanitized.replace(Regex("\\bav\\d{4,}\\b", RegexOption.IGNORE_CASE), "av***")
        sanitized = sanitized.replace(Regex("\"aid\":\\s*\\d+"), "\"aid\":***")
        // CID
        sanitized = sanitized.replace(Regex("\\bcid[=:]\\s*\\d+"), "cid=***")
        sanitized = sanitized.replace(Regex("\"cid\":\\s*\\d+"), "\"cid\":***")
        // 直播房间号
        sanitized = sanitized.replace(Regex("room_id[=:]\\s*\\d+"), "room_id=***")
        sanitized = sanitized.replace(Regex("roomId[=:]\\s*\\d+"), "roomId=***")
        // Season ID (番剧)
        sanitized = sanitized.replace(Regex("season_id[=:]\\s*\\d+"), "season_id=***")
        sanitized = sanitized.replace(Regex("ep_id[=:]\\s*\\d+"), "ep_id=***")
        
        // ========== 🔍 搜索关键词脱敏 ==========
        sanitized = sanitized.replace(Regex("keyword=[^&\\s]+"), "keyword=***")
        sanitized = sanitized.replace(Regex("\"keyword\":\"[^\"]+\""), "\"keyword\":\"***\"")
        sanitized = sanitized.replace(Regex("Search:\\s*[^\\n]+"), "Search: ***")

        // 私信、评论草稿等用户输入内容不进入诊断日志。
        sanitized = sanitized.replace(
            Regex("(?i)\\b(content|message_text|query)[=:]\\s*[^&\\r\\n]+"),
            "$1=***"
        )
        sanitized = sanitized.replace(
            Regex("(?i)\"(content|message_text|query)\"\\s*:\\s*\"[^\"]*\""),
            "\"$1\":\"***\""
        )
        
        // ========== 📝 视频标题脱敏（仅保留前两个字符） ==========
        sanitized = sanitized.replace(Regex("video_title=[^&\\s]{3,}")) { 
            val title = it.value.substringAfter("=")
            "video_title=${title.take(2)}***"
        }
        sanitized = sanitized.replace(Regex("\"title\":\"[^\"]{3,}\"")) {
            val content = it.value
            val titleStart = content.indexOf(":\"") + 2
            val title = content.substring(titleStart, content.length - 1)
            "\"title\":\"${title.take(2)}***\""
        }
        
        return sanitized
    }
    
    /**
     * 获取所有日志条目
     */
    fun getEntries(): List<LogEntry> = synchronized(lock) { buffer.toList() }
    
    /**
     * 获取日志条目数量
     */
    fun getCount(): Int = synchronized(lock) { buffer.size }
    
    /**
     * 清空日志
     */
    fun clear() {
        synchronized(lock) {
            buffer.clear()
            lastEntryFingerprint = null
            lastEntryTimestamp = 0L
        }
    }

    fun clearRuntimeDiagnostics() {
        synchronized(lock) {
            buffer.removeAll { it.level != "W" && it.level != "E" && it.tag != "StartupDiagnostics" }
            lastEntryFingerprint = null
        }
        val context = appContext ?: return
        diskWriter.execute {
            runCatching {
                resolveRuntimeLogFile(context.filesDir).delete()
            }.onFailure {
                Log.e("LogCollector", "清理增强诊断日志失败", it)
            }
        }
    }

    fun persistCrashSnapshot(throwable: Throwable) {
        val context = appContext ?: return
        val sanitizedEntries = getEntries().map { entry ->
            entry.copy(message = sanitizeMessage(entry.message))
        }
        runCatching {
            val content = buildCrashSnapshotContent(
                throwable = throwable,
                entries = sanitizedEntries,
                exportedAtMillis = System.currentTimeMillis(),
                appVersionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                manufacturer = android.os.Build.MANUFACTURER,
                model = android.os.Build.MODEL,
                androidRelease = android.os.Build.VERSION.RELEASE,
                apiLevel = android.os.Build.VERSION.SDK_INT,
                buildType = BuildConfig.BUILD_TYPE,
                buildCommit = BuildConfig.BUILD_COMMIT_SHA,
            )
            val snapshotFile = resolveCrashSnapshotFile(context.filesDir)
            val markerFile = resolveCrashSnapshotMarkerFile(context.filesDir)
            snapshotFile.parentFile?.mkdirs()
            snapshotFile.writeText(content)
            markerFile.writeText(System.currentTimeMillis().toString())
        }.onFailure {
            Log.e("LogCollector", "写入崩溃快照失败", it)
        }
    }

    fun getPendingCrashSnapshotFile(): File? {
        val context = appContext ?: return null
        val snapshotFile = resolveCrashSnapshotFile(context.filesDir)
        val markerFile = resolveCrashSnapshotMarkerFile(context.filesDir)
        return snapshotFile.takeIf {
            hasPendingCrashSnapshot(
                markerExists = markerFile.exists(),
                snapshotExists = snapshotFile.exists()
            )
        }
    }

    fun clearPendingCrashSnapshot() {
        val context = appContext ?: return
        runCatching {
            resolveCrashSnapshotMarkerFile(context.filesDir).delete()
        }.onFailure {
            Log.e("LogCollector", "清理崩溃快照标记失败", it)
        }
    }

    fun sharePendingCrashSnapshot(context: Context): Boolean {
        val snapshotFile = getPendingCrashSnapshotFile() ?: return false
        return runCatching {
            val cacheDir = File(context.cacheDir, LOG_DIRECTORY_NAME)
            cacheDir.mkdirs()
            val shareFile = File(cacheDir, CRASH_SNAPSHOT_FILE_NAME)
            shareFile.writeText(snapshotFile.readText())
            shareLogFileFromCache(context, shareFile)
            true
        }.getOrElse {
            Log.e("LogCollector", "分享崩溃快照失败", it)
            false
        }
    }
    
    /**
     * 导出日志到文件并通过系统分享
     * 
     * 日志会保存到 Download/BiliPai/logs/ 目录，方便 MT 管理器等工具直接访问
     */
    fun exportAndShare(context: Context, includeSystemDiagnostics: Boolean = true) {
        init(context)
        // Queue behind pending writes: exporting immediately after an error must include it.
        // File reads, MediaStore writes and trace copies must not block the recovery UI.
        diskWriter.execute {
            try {
                val persistedLines = listOf(
                    resolveBasicLogFile(context.filesDir),
                    resolveRuntimeLogFile(context.filesDir),
                ).flatMap { file ->
                    runCatching { if (file.isFile) file.readLines() else emptyList() }
                        .getOrDefault(emptyList())
                }
                val logLines = (persistedLines + getEntries().map { it.format() })
                    .filter { it.isNotBlank() }
                    .distinct()
                // Dismissing the prompt only clears its marker, not the retained evidence.
                val crashContent = resolveCrashSnapshotFile(context.filesDir)
                    .takeIf(File::isFile)?.readText()?.let(::sanitizeMessage)
                val recentProcessExits = if (includeSystemDiagnostics) {
                    Android17Diagnostics.recentProcessExitSummaries(context)
                } else emptyList()
                val retainedProfiles = if (includeSystemDiagnostics) {
                    Android17Diagnostics.retainedArtifacts(context)
                } else emptyList()
                if (!hasExportableDiagnostics(
                        logCount = logLines.size,
                        hasCrashSnapshot = !crashContent.isNullOrBlank(),
                        processExitCount = recentProcessExits.size,
                        profilingArtifactCount = retainedProfiles.size,
                    )) {
                    showExportToast(context, "暂无日志记录")
                    return@execute
                }
                val content = buildString {
                    appendLine("BiliPai 应用日志导出")
                    appendLine("导出时间: ${dateFormat.format(Date())}")
                    appendLine("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("构建: ${BuildConfig.BUILD_TYPE} / ${BuildConfig.BUILD_COMMIT_SHA}")
                    appendLine("设备信息: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    appendLine("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                    appendLine("基础诊断: 默认保留，滚动上限 64KB")
                    appendLine("增强诊断: ${if (Logger.areVerboseRuntimeLogsEnabled()) "已开启" else "未开启"}")
                    appendLine("历史进程退出记录: ${recentProcessExits.size} 条")
                    recentProcessExits.forEach { appendLine(sanitizeMessage(it)) }
                    appendLine("隐私说明: 日志已脱敏，仅在用户主动导出时分享")
                    appendLine("----- 运行日志（${logLines.size} 条）-----")
                    logLines.forEach { appendLine(sanitizeMessage(it)) }
                    if (!crashContent.isNullOrBlank()) {
                        appendLine("----- 最近一次崩溃快照 -----")
                        appendLine(crashContent)
                    }
                }
                val fileName = "bilipai_log_${fileDateFormat.format(Date())}.txt"
                val savedPath = saveToExternalDownload(context, fileName, content)
                val shareCacheDir = File(context.cacheDir, LOG_DIRECTORY_NAME).apply { mkdirs() }
                val shareFile = File(shareCacheDir, fileName).apply { writeText(content) }
                val profilingFiles = if (retainedProfiles.isNotEmpty()) {
                    Android17Diagnostics.copyRetainedArtifactsTo(context, shareCacheDir)
                } else emptyList()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
                        return@post
                    }
                    val message = if (savedPath != null) "日志已保存到 $savedPath" else "日志已准备好，请选择分享方式"
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    shareLogFilesFromCache(context, listOf(shareFile) + profilingFiles)
                }
            } catch (error: Exception) {
                Log.e("LogCollector", "导出日志失败", error)
                showExportToast(context, "日志导出失败，请重试")
            }
        }
    }

    private fun showExportToast(context: Context, message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun saveTextArtifact(
        context: Context,
        fileName: String,
        content: String,
        replaceExisting: Boolean = false
    ): String? {
        return saveToExternalDownload(
            context = context,
            fileName = fileName,
            content = content,
            replaceExisting = replaceExisting
        )
    }
    
    /**
     *  保存日志到外部 Download 目录
     * 
     * 路径: /storage/emulated/0/Download/BiliPai/logs/xxx.txt
     * MT管理器路径: Download/BiliPai/logs/
     */
    private fun saveToExternalDownload(
        context: Context,
        fileName: String,
        content: String,
        replaceExisting: Boolean = false
    ): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (replaceExisting) {
                    val existingUri = findExistingDownloadUri(context, fileName)
                    if (existingUri != null) {
                        context.contentResolver.delete(existingUri, null, null)
                    }
                }
                // Android 10+ 使用 MediaStore API
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, DOWNLOAD_LOG_RELATIVE_PATH)
                }
                
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    "$DOWNLOAD_LOG_RELATIVE_PATH/$fileName"
                }
            } else {
                // Android 9 及以下直接写入
                @Suppress("DEPRECATION")
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val logDir = File(downloadDir, "BiliPai/logs")
                logDir.mkdirs()
                val logFile = File(logDir, fileName)
                logFile.writeText(content)
                logFile.absolutePath
            }
        } catch (e: Exception) {
            Log.w("LogCollector", "无法保存到外部存储", e)
            null
        }
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun findExistingDownloadUri(context: Context, fileName: String): android.net.Uri? {
        val projection = arrayOf(
            android.provider.MediaStore.Downloads._ID
        )
        val selection =
            "${android.provider.MediaStore.Downloads.DISPLAY_NAME}=? AND " +
                "${android.provider.MediaStore.Downloads.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(fileName, DOWNLOAD_LOG_RELATIVE_PATH)
        return context.contentResolver.query(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val idIndex = cursor.getColumnIndex(android.provider.MediaStore.Downloads._ID)
            if (idIndex < 0) return@use null
            val id = cursor.getLong(idIndex)
            android.content.ContentUris.withAppendedId(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                id
            )
        }
    }
    
    /**
     * 分享日志文件（从缓存目录）
     */
    private fun shareLogFileFromCache(context: Context, logFile: File) {
        shareLogFilesFromCache(context, listOf(logFile))
    }

    private fun shareLogFilesFromCache(context: Context, files: List<File>) {
        try {
            val validFiles = files.filter(File::isFile)
            val uris = ArrayList(validFiles.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            })
            if (uris.isEmpty()) return
            val shareIntent = Intent(
                if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            ).apply {
                type = if (uris.size == 1 && validFiles.single().extension == "txt") {
                    "text/plain"
                } else {
                    "application/octet-stream"
                }
                if (uris.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, uris.single())
                } else {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                }
                putExtra(Intent.EXTRA_SUBJECT, "BiliPai 日志反馈")
                putExtra(
                    Intent.EXTRA_TEXT,
                    if (uris.size == 1) "请查看附件中的日志文件"
                    else "请查看附件中的日志与用户主动导出的性能诊断文件"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "分享日志"))
        } catch (e: Exception) {
            Log.e("LogCollector", "分享失败", e)
        }
    }

    private fun appendEntryToRuntimeFile(entry: LogEntry, basicDiagnostic: Boolean = false) {
        val context = appContext ?: return
        val sanitizedEntry = entry.copy(message = sanitizeMessage(entry.message)).format() + "\n"
        diskWriter.execute {
            runCatching {
                val isBasic = basicDiagnostic || entry.level == "W" || entry.level == "E"
                appendRollingDiagnosticLog(
                    file = if (isBasic) resolveBasicLogFile(context.filesDir)
                        else resolveRuntimeLogFile(context.filesDir),
                    text = sanitizedEntry,
                    maxBytes = if (isBasic) MAX_BASIC_LOG_BYTES else MAX_PERSISTED_LOG_BYTES,
                )
            }.onFailure {
                Log.e("LogCollector", "持久化运行日志失败", it)
            }
        }
    }

}
