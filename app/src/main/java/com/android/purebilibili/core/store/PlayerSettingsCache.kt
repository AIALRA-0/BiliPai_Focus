package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.core.util.Logger

/**
 * 播放器设置缓存
 * 
 * 在 Application 启动时初始化，避免每次创建播放器时读取 SharedPreferences
 * 
 * 性能优化:
 * - 内存缓存硬件解码设置
 * - 避免重复 I/O 读取
 */
object PlayerSettingsCache {
    private const val TAG = "PlayerSettingsCache"
    private const val PREFS_NAME = "player_settings_cache"
    private const val KEY_HW_DECODE = "hw_decode_enabled"
    private const val KEY_SEEK_FAST = "seek_fast_enabled"
    private const val KEY_PLAYER_DIAGNOSTIC_LOGGING = "player_diagnostic_logging_enabled"
    private const val LEGACY_HW_DECODE_PREFS_NAME = "hw_decode_cache"
    
    // 内存缓存
    @Volatile
    private var hwDecodeEnabled: Boolean? = null
    
    @Volatile
    private var seekFastEnabled: Boolean? = null

    @Volatile
    private var playerDiagnosticLoggingEnabled: Boolean? = null
    
    /**
     * 初始化缓存（在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hwDecodeEnabled = readAndMigrateHwDecodeEnabled(context, prefs)
        seekFastEnabled = prefs.getBoolean(KEY_SEEK_FAST, true)
        playerDiagnosticLoggingEnabled = prefs.getBoolean(
            KEY_PLAYER_DIAGNOSTIC_LOGGING,
            DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
        )
        Logger.d(
            TAG,
            "✅ 初始化完成: hwDecode=$hwDecodeEnabled, seekFast=$seekFastEnabled, " +
                "playerDiagnosticLogging=$playerDiagnosticLoggingEnabled"
        )
    }
    
    /**
     * 获取硬件解码设置（优先使用内存缓存）
     */
    fun isHwDecodeEnabled(context: Context): Boolean {
        return hwDecodeEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = readAndMigrateHwDecodeEnabled(context, prefs)
            hwDecodeEnabled = value
            value
        }
    }
    
    /**
     * 设置硬件解码开关
     */
    fun setHwDecodeEnabled(context: Context, enabled: Boolean) {
        hwDecodeEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HW_DECODE, enabled)
            .apply()
        Logger.d(TAG, "💾 硬件解码设置已更新: $enabled")
    }
    
    /**
     * 获取快速 Seek 设置
     */
    fun isSeekFastEnabled(context: Context): Boolean {
        return seekFastEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(KEY_SEEK_FAST, true)
            seekFastEnabled = value
            value
        }
    }
    
    /**
     * 设置快速 Seek 开关
     */
    fun setSeekFastEnabled(context: Context, enabled: Boolean) {
        seekFastEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEK_FAST, enabled)
            .apply()
        Logger.d(TAG, "💾 快速 Seek 设置已更新: $enabled")
    }

    /**
     * 获取播放器诊断日志开关
     */
    fun isPlayerDiagnosticLoggingEnabled(context: Context): Boolean {
        return playerDiagnosticLoggingEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(
                KEY_PLAYER_DIAGNOSTIC_LOGGING,
                DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
            )
            playerDiagnosticLoggingEnabled = value
            value
        }
    }

    /**
     * 获取播放器诊断日志开关（仅读取内存缓存）
     */
    fun isPlayerDiagnosticLoggingEnabled(): Boolean {
        return playerDiagnosticLoggingEnabled ?: DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
    }

    /**
     * 设置播放器诊断日志开关
     */
    fun setPlayerDiagnosticLoggingEnabled(context: Context, enabled: Boolean) {
        playerDiagnosticLoggingEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PLAYER_DIAGNOSTIC_LOGGING, enabled)
            .apply()
        Logger.d(TAG, "💾 播放器诊断日志设置已更新: $enabled")
    }
    
    /**
     * 强制刷新缓存（设置页面修改后调用）
     */
    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hwDecodeEnabled = readAndMigrateHwDecodeEnabled(context, prefs)
        seekFastEnabled = prefs.getBoolean(KEY_SEEK_FAST, true)
        playerDiagnosticLoggingEnabled = prefs.getBoolean(
            KEY_PLAYER_DIAGNOSTIC_LOGGING,
            DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
        )
        Logger.d(
            TAG,
            "🔄 缓存已刷新: hwDecode=$hwDecodeEnabled, seekFast=$seekFastEnabled, " +
                "playerDiagnosticLogging=$playerDiagnosticLoggingEnabled"
        )
    }

    private fun readAndMigrateHwDecodeEnabled(
        context: Context,
        prefs: android.content.SharedPreferences
    ): Boolean {
        val legacyPrefs = context.getSharedPreferences(LEGACY_HW_DECODE_PREFS_NAME, Context.MODE_PRIVATE)
        val currentExists = prefs.contains(KEY_HW_DECODE)
        val legacyExists = legacyPrefs.contains(KEY_HW_DECODE)
        val value = resolveMigratedHwDecodeValue(
            currentExists = currentExists,
            currentValue = prefs.getBoolean(KEY_HW_DECODE, true),
            legacyExists = legacyExists,
            legacyValue = legacyPrefs.getBoolean(KEY_HW_DECODE, true)
        )

        // 老版本写入过 hw_decode_cache；迁移到播放器实际读取的缓存，保留用户选择。
        if (!currentExists && legacyExists) {
            prefs.edit()
                .putBoolean(KEY_HW_DECODE, value)
                .apply()
            Logger.d(TAG, "🔁 已迁移硬件解码缓存: hwDecode=$value")
        }

        return value
    }
}

internal fun resolveMigratedHwDecodeValue(
    currentExists: Boolean,
    currentValue: Boolean,
    legacyExists: Boolean,
    legacyValue: Boolean
): Boolean {
    return when {
        currentExists -> currentValue
        legacyExists -> legacyValue
        else -> true
    }
}
