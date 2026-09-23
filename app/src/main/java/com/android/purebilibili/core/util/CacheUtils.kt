@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.core.util

import android.content.Context
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.android.purebilibili.core.cache.PlayUrlCache
import com.android.purebilibili.core.cooldown.PlaybackCooldownManager
import com.android.purebilibili.core.player.PlaybackMediaCache
import com.android.purebilibili.core.store.FollowingCacheStore
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.repository.DanmakuRepository
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import java.io.File

enum class CacheClearTarget {
    PLAYBACK_QUALITY,
    NETWORK,
    IMAGE_PREVIEW,
    SUBTITLE_DANMAKU,
    TEMP_FILES_AND_LOGS,
    APP_METADATA
}

internal fun shouldAutomaticallyClearCache(
    interval: SettingsManager.AutoCacheClearInterval,
    lastClearAtMillis: Long,
    nowMillis: Long,
    reclaimableDiskBytes: Long,
    thresholdBytes: Long
): Boolean {
    val intervalDue = interval != SettingsManager.AutoCacheClearInterval.NEVER &&
        (lastClearAtMillis <= 0L || nowMillis - lastClearAtMillis >= interval.days * 24L * 60L * 60L * 1000L)
    val thresholdReached = thresholdBytes > 0L && reclaimableDiskBytes >= thresholdBytes
    return intervalDue || thresholdReached
}

/**
 *  缓存工具类 - 优化版
 * 
 * 改进点:
 * 1. 使用 walkTopDown() 惰性序列替代递归遍历
 * 2. 分类统计缓存大小（图片/HTTP/视频URL/其他）
 * 3. 正确的清理顺序避免冲突
 * 4. 完整的内存缓存清理（包含 PlayUrlCache）
 */
@OptIn(ExperimentalCoilApi::class)
object CacheUtils {

    private const val TAG = "CacheUtils"
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    suspend fun clearCacheAutomaticallyIfDue(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val interval = SettingsManager.getAutoCacheClearInterval(context).first()
        val thresholdGb = SettingsManager.getAutoCacheClearThresholdGb(context).first()
        val lastClearAt = SettingsManager.getLastAutoCacheClearAt(context)
        val intervalDue = interval != SettingsManager.AutoCacheClearInterval.NEVER &&
            (lastClearAt <= 0L || nowMillis - lastClearAt >= interval.days * DAY_MILLIS)
        val thresholdBytes = thresholdGb.toLong() * 1024L * 1024L * 1024L
        val reclaimableDiskBytes = getCacheBreakdown(context).reclaimableDiskSize
        val thresholdReached = reclaimableDiskBytes >= thresholdBytes
        if (!shouldAutomaticallyClearCache(
                interval = interval,
                lastClearAtMillis = lastClearAt,
                nowMillis = nowMillis,
                reclaimableDiskBytes = reclaimableDiskBytes,
                thresholdBytes = thresholdBytes
            )
        ) return false
        Logger.i(
            TAG,
            "Auto cache clear triggered: intervalDue=$intervalDue, " +
                "thresholdReached=$thresholdReached, diskBytes=$reclaimableDiskBytes, " +
                "thresholdGb=$thresholdGb"
        )
        val result = clearCache(context, CacheClearTarget.entries.toSet())
        if (result.isSuccess) SettingsManager.setLastAutoCacheClearAt(context, nowMillis)
        return result.isSuccess
    }

    /**
     * 缓存详情数据类
     */
    data class CacheBreakdown(
        val imageDiskCache: Long = 0L,  // Coil 图片磁盘缓存
        val imageMemoryCache: Long = 0L, // Coil 图片内存缓存
        val httpCache: Long = 0L,       // OkHttp HTTP 缓存
        val playbackMediaCache: Long = 0L, // 播放器媒体分片缓存
        val otherCache: Long = 0L,      // 其他文件缓存
        val playUrlMemoryCache: Long = 0L, // PlayUrl 内存缓存
        val subtitleDanmakuMemoryCache: Long = 0L // 字幕/弹幕内存缓存
    ) {
        val imageCache: Long get() = imageDiskCache + imageMemoryCache
        val memoryCache: Long get() = imageMemoryCache + playUrlMemoryCache + subtitleDanmakuMemoryCache
        val networkCache: Long get() = httpCache + playbackMediaCache
        /** 与清理操作一致的、实际可回收磁盘字节数；不混入运行时内存估算。 */
        val reclaimableDiskSize: Long get() = imageDiskCache + networkCache + otherCache
        val totalSize: Long get() = reclaimableDiskSize + memoryCache
        
        fun format(): String = formatSize(reclaimableDiskSize.toDouble())
        
        fun formatBreakdown(): String = buildString {
            append("图片: ${formatSize(imageCache.toDouble())}")
            append(" | 网络: ${formatSize(networkCache.toDouble())}")
            append(" | 其他: ${formatSize(otherCache.toDouble())}")
            if (memoryCache > 0) {
                append(" | 内存: ${formatSize(memoryCache.toDouble())}")
            }
        }
    }

    /**
     *  获取总缓存大小（格式化字符串）
     */
    suspend fun getTotalCacheSize(context: Context): String = withContext(Dispatchers.IO) {
        getCacheBreakdown(context).format()
    }

    /**
     *  获取详细缓存统计
     */
    suspend fun getCacheBreakdown(context: Context): CacheBreakdown = withContext(Dispatchers.IO) {
        var imageDiskCache = 0L
        var imageMemoryCache = 0L
        var httpCache = 0L
        var otherCache = 0L
        var playUrlMemoryCache = 0L
        var subtitleDanmakuMemoryCache = 0L

        // 1. Coil 内存缓存
        context.imageLoader.memoryCache?.size?.let { imageMemoryCache += it }
        
        // 2. PlayUrlCache 内存缓存（估算：每条约 2KB）
        val playUrlCacheSize = PlayUrlCache.size()
        playUrlMemoryCache += playUrlCacheSize * 2048L

        // 3. 字幕与弹幕内存缓存
        subtitleDanmakuMemoryCache += VideoRepository.getSubtitleCueCacheStats().estimatedBytes
        subtitleDanmakuMemoryCache += DanmakuRepository.getDanmakuCacheStats().totalBytes

        // 4. 内部缓存目录分类统计
        context.cacheDir?.let { cacheDir ->
            cacheDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val size = file.length()
                    when {
                        // Coil 图片缓存目录
                        file.absolutePath.contains("image_cache") -> imageDiskCache += size
                        file.absolutePath.contains("playback_media_cache") -> Unit
                        // OkHttp 缓存目录
                        file.absolutePath.contains("http_cache") ||
                        file.absolutePath.contains("okhttp") -> httpCache += size
                        // 其他缓存
                        else -> otherCache += size
                    }
                }
        }
        val playbackMediaCache = PlaybackMediaCache.estimateBytes(context)

        // 5. 应用私有日志文件
        otherCache += Logger.getPrivateLogArtifactsSize(context)

        // 6. 外部缓存目录
        context.externalCacheDir?.let { extCacheDir ->
            otherCache += getDirSizeFast(extCacheDir)
        }

        CacheBreakdown(
            imageDiskCache = imageDiskCache,
            imageMemoryCache = imageMemoryCache,
            httpCache = httpCache,
            playbackMediaCache = playbackMediaCache,
            otherCache = otherCache,
            playUrlMemoryCache = playUrlMemoryCache,
            subtitleDanmakuMemoryCache = subtitleDanmakuMemoryCache
        )
    }

    /**
     *  清除所有缓存（优化顺序，避免冲突）
     */
    suspend fun clearAllCache(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        clearCache(context, CacheClearTarget.entries.toSet())
    }

    suspend fun clearCache(
        context: Context,
        targets: Set<CacheClearTarget>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (CacheClearTarget.IMAGE_PREVIEW in targets) {
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
                Logger.d(TAG, " Image preview cache cleared")
            }

            if (CacheClearTarget.PLAYBACK_QUALITY in targets) {
                PlayUrlCache.clear()
                PlaybackCooldownManager.clearAll()
                Logger.d(TAG, " Playback quality cache cleared")
            }

            if (CacheClearTarget.SUBTITLE_DANMAKU in targets) {
                VideoRepository.clearSubtitleCueCache()
                DanmakuRepository.clearDanmakuCache()
                Logger.d(TAG, " Subtitle and danmaku cache cleared")
            }

            if (CacheClearTarget.NETWORK in targets) {
                try {
                    com.android.purebilibili.core.network.NetworkModule.okHttpClient.cache?.evictAll()
                    PlaybackMediaCache.clear(context)
                    Logger.d(TAG, " Network cache cleared")
                } catch (e: Exception) {
                    Logger.w(TAG, "OkHttp cache clear failed: ${e.message}")
                }
            }

            if (CacheClearTarget.TEMP_FILES_AND_LOGS in targets) {
                val excludePatterns = buildList {
                    if (CacheClearTarget.IMAGE_PREVIEW !in targets) add("image_cache")
                    if (CacheClearTarget.NETWORK !in targets) {
                        add("okhttp")
                        add("http_cache")
                        add("playback_media_cache")
                    }
                }
                context.cacheDir?.let { cacheDir ->
                    clearDirContentsSelective(cacheDir, excludePatterns = excludePatterns)
                }
                context.externalCacheDir?.let { clearDirContents(it) }
                Logger.clearPrivateLogArtifacts(context)
                Logger.d(TAG, " Temp files and log artifacts cleared")
            }

            if (CacheClearTarget.APP_METADATA in targets) {
                FollowingCacheStore.clear(context)
                com.android.purebilibili.core.network.WbiKeyManager.invalidateCache()
                Logger.d(TAG, " App metadata cache cleared")
            }
        }.onFailure { e ->
            Logger.e(TAG, "Error clearing cache", e)
        }
    }

    /**
     *  清除缓存并返回进度 Flow
     */
    fun clearAllCacheWithProgress(context: Context): Flow<ClearProgress> = flow {
        emit(ClearProgress(0, "正在清除内存缓存..."))
        
        // 内存缓存
        context.imageLoader.memoryCache?.clear()
        PlayUrlCache.clear()
        VideoRepository.clearSubtitleCueCache()
        DanmakuRepository.clearDanmakuCache()
        emit(ClearProgress(20, "内存缓存已清除"))
        
        // 磁盘缓存
        emit(ClearProgress(30, "正在清除图片缓存..."))
        context.imageLoader.diskCache?.clear()
        emit(ClearProgress(50, "图片缓存已清除"))
        
        emit(ClearProgress(60, "正在清除网络缓存..."))
        try {
            com.android.purebilibili.core.network.NetworkModule.okHttpClient.cache?.evictAll()
            PlaybackMediaCache.clear(context)
        } catch (_: Exception) {}
        emit(ClearProgress(70, "网络缓存已清除"))
        
        // 文件缓存
        emit(ClearProgress(80, "正在清除临时文件..."))
        context.cacheDir?.let { clearDirContentsSelective(it, listOf("image_cache", "okhttp", "playback_media_cache")) }
        context.externalCacheDir?.let { clearDirContents(it) }
        emit(ClearProgress(90, "临时文件已清除"))
        
        // 应用缓存
        FollowingCacheStore.clear(context)
        com.android.purebilibili.core.network.WbiKeyManager.invalidateCache()
        PlaybackCooldownManager.clearAll()
        Logger.clearPrivateLogArtifacts(context)
        
        emit(ClearProgress(100, "清理完成"))
    }.flowOn(Dispatchers.IO)

    /**
     * 清理进度数据类
     */
    data class ClearProgress(
        val percent: Int,
        val message: String
    )
    
    /**
     *  清除缓存并返回进度 Flow (增强版 - 支持动画)
     * 返回已清理的字节数和总字节数
     */
    data class ClearProgressV2(
        val cleared: Long,       // 已清理字节数
        val total: Long,         // 总字节数
        val isComplete: Boolean, // 是否完成
        val message: String      // 状态消息
    ) {
        fun formatCleared(): String = formatSizeStatic(cleared.toDouble())
        
        companion object {
            private fun formatSizeStatic(size: Double): String {
                val kiloByte = size / 1024
                if (kiloByte < 1) return "0 KB"
                val megaByte = kiloByte / 1024
                if (megaByte < 1) return String.format("%.1f KB", kiloByte)
                val gigaByte = megaByte / 1024
                if (gigaByte < 1) return String.format("%.1f MB", megaByte)
                return String.format("%.2f GB", gigaByte)
            }
        }
    }

    fun clearAllCacheWithProgressV2(context: Context): Flow<ClearProgressV2> = flow {
        // 首先获取总大小
        val breakdown = getCacheBreakdown(context)
        val totalSize = breakdown.totalSize
        var clearedSize = 0L
        
        emit(ClearProgressV2(0, totalSize, false, "正在清除内存缓存..."))
        
        // 内存缓存
        val memorySize = breakdown.memoryCache
        context.imageLoader.memoryCache?.clear()
        PlayUrlCache.clear()
        VideoRepository.clearSubtitleCueCache()
        DanmakuRepository.clearDanmakuCache()
        clearedSize += memorySize
        emit(ClearProgressV2(clearedSize, totalSize, false, "内存缓存已清除"))
        kotlinx.coroutines.delay(100)
        
        // 磁盘图片缓存
        emit(ClearProgressV2(clearedSize, totalSize, false, "正在清除图片缓存..."))
        val imageSize = breakdown.imageCache
        context.imageLoader.diskCache?.clear()
        clearedSize += imageSize
        emit(ClearProgressV2(clearedSize, totalSize, false, "图片缓存已清除"))
        kotlinx.coroutines.delay(100)
        
        // 网络缓存
        emit(ClearProgressV2(clearedSize, totalSize, false, "正在清除网络缓存..."))
        val httpSize = breakdown.networkCache
        try {
            com.android.purebilibili.core.network.NetworkModule.okHttpClient.cache?.evictAll()
            PlaybackMediaCache.clear(context)
        } catch (_: Exception) {}
        clearedSize += httpSize
        emit(ClearProgressV2(clearedSize, totalSize, false, "网络缓存已清除"))
        kotlinx.coroutines.delay(100)
        
        // 文件缓存
        emit(ClearProgressV2(clearedSize, totalSize, false, "正在清除临时文件..."))
        val otherSize = breakdown.otherCache
        context.cacheDir?.let { clearDirContentsSelective(it, listOf("image_cache", "okhttp", "playback_media_cache")) }
        context.externalCacheDir?.let { clearDirContents(it) }
        clearedSize += otherSize
        emit(ClearProgressV2(clearedSize, totalSize, false, "临时文件已清除"))
        kotlinx.coroutines.delay(100)
        
        // 应用缓存
        FollowingCacheStore.clear(context)
        com.android.purebilibili.core.network.WbiKeyManager.invalidateCache()
        PlaybackCooldownManager.clearAll()
        Logger.clearPrivateLogArtifacts(context)
        
        emit(ClearProgressV2(totalSize, totalSize, true, "清理完成"))
    }.flowOn(Dispatchers.IO)

    /**
     *  使用 walkTopDown 惰性序列快速计算目录大小
     */
    private fun getDirSizeFast(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return try {
            dir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            Logger.w(TAG, "Error calculating dir size: ${e.message}")
            0L
        }
    }

    /**
     * 清空目录内容（保留目录本身）
     */
    private fun clearDirContents(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        return try {
            dir.walkTopDown()
                .filter { it != dir }  // 排除根目录
                .sortedByDescending { it.absolutePath.length }  // 深度优先删除
                .forEach { it.delete() }
            true
        } catch (e: Exception) {
            Logger.w(TAG, "Error clearing directory: ${dir.path}")
            false
        }
    }

    /**
     *  选择性清空目录（排除指定模式的子目录）
     */
    private fun clearDirContentsSelective(dir: File, excludePatterns: List<String>): Boolean {
        if (!dir.exists()) return false
        return try {
            dir.walkTopDown()
                .filter { file ->
                    file != dir && excludePatterns.none { pattern -> 
                        file.absolutePath.contains(pattern) 
                    }
                }
                .sortedByDescending { it.absolutePath.length }
                .forEach { it.delete() }
            true
        } catch (e: Exception) {
            Logger.w(TAG, "Error clearing directory selectively: ${dir.path}")
            false
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) return "0 KB"
        val megaByte = kiloByte / 1024
        if (megaByte < 1) return String.format("%.1f KB", kiloByte)
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) return String.format("%.1f MB", megaByte)
        return String.format("%.2f GB", gigaByte)
    }
}
