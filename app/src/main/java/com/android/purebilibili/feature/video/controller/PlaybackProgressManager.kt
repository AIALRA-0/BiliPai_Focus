// File: feature/video/controller/PlaybackProgressManager.kt
package com.android.purebilibili.feature.video.controller

import android.content.Context
import android.content.SharedPreferences
import com.android.purebilibili.core.util.Logger

/**
 * Playback Progress Manager
 * 
 * Handles playback progress save/restore with persistent storage:
 * - Cache playback position for each video
 * - Save current position when switching videos
 * - Restore position when returning to a video
 * - Persist progress to SharedPreferences
 * 
 * Requirement Reference: AC1.4 - Progress managed by PlaybackProgressManager
 */
class PlaybackProgressManager {
    
    companion object {
        private const val TAG = "PlaybackProgressManager"
        private const val PREFS_NAME = "video_progress"
        private const val MAX_CACHE_SIZE = 100
        private const val MIN_PROGRESS_TO_SAVE = 5000L // 5秒以上才保存
        private const val MAX_PERCENT_TO_RESTORE = 0.95f // 超过95%不恢复（已看完）
        
        @Volatile
        private var instance: PlaybackProgressManager? = null
        
        fun getInstance(context: Context): PlaybackProgressManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackProgressManager().also { 
                    it.init(context) 
                    instance = it
                }
            }
        }
    }
    
    private var prefs: SharedPreferences? = null
    
    // Memory cache for fast access
    private val memoryCache = LinkedHashMap<String, Long>(MAX_CACHE_SIZE, 0.75f, true)

    private fun buildProgressKey(bvid: String, cid: Long): String {
        return if (cid > 0L) "$bvid#$cid" else bvid
    }
    
    /**
     * Initialize with context (for SharedPreferences)
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPrefs()
            Logger.d(TAG, "Initialized with ${memoryCache.size} cached positions")
        }
    }
    
    /**
     * Save playback position
     */
    fun savePosition(bvid: String, cid: Long, positionMs: Long, durationMs: Long = 0L) {
        if (bvid.isEmpty() || positionMs < MIN_PROGRESS_TO_SAVE) return
        val key = buildProgressKey(bvid, cid)
        
        // 如果已播放超过95%，视为已看完，清除记录
        if (durationMs > 0 && positionMs.toFloat() / durationMs > MAX_PERCENT_TO_RESTORE) {
            clearPosition(bvid, cid)
            Logger.d(TAG, "Video $key completed (${positionMs}ms / ${durationMs}ms), cleared progress")
            return
        }
        
        // Remove oldest entry if cache is full
        if (memoryCache.size >= MAX_CACHE_SIZE) {
            val oldestKey = memoryCache.keys.firstOrNull()
            if (oldestKey != null) {
                memoryCache.remove(oldestKey)
                prefs?.edit()?.remove(oldestKey)?.apply()
            }
        }
        
        memoryCache[key] = positionMs
        prefs?.edit()?.apply {
            putLong(key, positionMs)
            if (cid > 0L) {
                // 空间页等入口拿不到 cid 时，用 bvid 级进度兜底；播放器加载后仍优先 cid 精确进度。
                putLong(buildProgressKey(bvid, cid = 0L), positionMs)
                memoryCache[buildProgressKey(bvid, cid = 0L)] = positionMs
            }
        }?.apply()
        Logger.d(TAG, "Saved position for $key: ${positionMs}ms")
    }

    fun savePosition(bvid: String, positionMs: Long, durationMs: Long = 0L) {
        savePosition(bvid, cid = 0L, positionMs = positionMs, durationMs = durationMs)
    }
    
    /**
     * Save position (without duration check)
     */
    fun savePosition(bvid: String, positionMs: Long) {
        savePosition(bvid, cid = 0L, positionMs = positionMs, durationMs = 0L)
    }
    
    /**
     * Get cached playback position
     */
    fun getCachedPosition(bvid: String, cid: Long): Long {
        val key = buildProgressKey(bvid, cid)
        // First check memory cache
        var position = memoryCache[key]
        
        // If not in memory, check SharedPreferences
        if (position == null) {
            position = prefs?.getLong(key, 0L) ?: 0L
            if (position > 0) {
                memoryCache[key] = position
            }
        }
        
        if (position > 0) {
            Logger.d(TAG, "Retrieved position for $key: ${position}ms")
            return position
        }
        // cid 精确查询用于分 P/明确页面恢复，不能回退到 bvid 级进度，
        // 否则切到未看过的分 P 会继承上一分 P 刚保存的位置。
        return 0L
    }

    fun getCachedPosition(bvid: String): Long {
        return getCachedPosition(bvid, cid = 0L)
    }
    
    /**
     * Clear position cache for a specific video
     */
    fun clearPosition(bvid: String, cid: Long) {
        val key = buildProgressKey(bvid, cid)
        memoryCache.remove(key)
        prefs?.edit()?.apply {
            remove(key)
            if (cid > 0L) {
                val bvidKey = buildProgressKey(bvid, cid = 0L)
                remove(bvidKey)
                memoryCache.remove(bvidKey)
            }
        }?.apply()
        Logger.d(TAG, "Cleared position for $key")
    }

    fun clearPosition(bvid: String) {
        clearPosition(bvid, cid = 0L)
    }
    
    /**
     * Clear all position caches
     */
    fun clearAll() {
        memoryCache.clear()
        prefs?.edit()?.clear()?.apply()
        Logger.d(TAG, "Cleared all positions")
    }
    
    /**
     * Check if there is cached position for a video
     */
    fun hasPosition(bvid: String, cid: Long): Boolean {
        return getCachedPosition(bvid, cid) > 0
    }

    fun hasPosition(bvid: String): Boolean {
        return hasPosition(bvid, cid = 0L)
    }
    
    /**
     * Get the number of cached positions
     */
    fun getCacheSize(): Int {
        return memoryCache.size
    }
    
    /**
     * Load positions from SharedPreferences to memory cache
     */
    private fun loadFromPrefs() {
        prefs?.all?.forEach { (key, value) ->
            if (value is Long && value > 0) {
                memoryCache[key] = value
            }
        }
    }
}
