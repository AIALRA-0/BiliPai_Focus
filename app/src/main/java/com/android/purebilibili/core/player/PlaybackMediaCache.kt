package com.android.purebilibili.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.android.purebilibili.core.util.Logger
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "PlaybackMediaCache"
private const val PLAYBACK_MEDIA_CACHE_DIR = "playback_media_cache"

internal data class PlaybackMediaCacheStats(
    val upstreamBytes: Long,
    val cachedBytes: Long,
    val ignoredCount: Int
)

internal fun resolvePlaybackMediaCacheMaxBytes(): Long = 512L * 1024L * 1024L

internal fun shouldUsePlaybackMediaCache(uri: Uri): Boolean {
    return shouldUsePlaybackMediaCache(uri.toString())
}

internal fun shouldUsePlaybackMediaCache(rawUri: String): Boolean {
    val scheme = runCatching { URI(rawUri).scheme }.getOrNull()
    return scheme.equals("http", ignoreCase = true) ||
        scheme.equals("https", ignoreCase = true)
}

internal fun buildPlaybackCacheKey(uri: Uri, explicitKey: String?): String {
    if (!explicitKey.isNullOrBlank()) return explicitKey
    return buildPlaybackCacheKey(rawUri = uri.toString(), explicitKey = null)
}

internal fun buildPlaybackCacheKey(rawUri: String, explicitKey: String?): String {
    if (!explicitKey.isNullOrBlank()) return explicitKey
    val parsed = runCatching { URI(rawUri) }.getOrNull()
    val scheme = parsed?.scheme
    val host = parsed?.host
    val path = parsed?.rawPath
    return if (!scheme.isNullOrBlank() && !host.isNullOrBlank() && !path.isNullOrBlank()) {
        "$scheme://$host$path"
    } else {
        rawUri
    }
}

@UnstableApi
internal object PlaybackMediaCache {
    private val upstreamBytes = AtomicLong(0L)
    private val cachedBytes = AtomicLong(0L)
    private val ignoredCount = AtomicLong(0L)

    @Volatile
    private var simpleCache: SimpleCache? = null

    fun buildCachedDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory
    ): DataSource.Factory {
        val cache = getOrCreateCache(context) ?: return upstreamFactory
        val monitoredUpstreamFactory = DataSource.Factory {
            upstreamFactory.createDataSource().apply {
                addTransferListener(upstreamTransferListener)
            }
        }
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(monitoredUpstreamFactory)
            .setCacheKeyFactory(playbackCacheKeyFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(cacheEventListener)
    }

    fun estimateBytes(context: Context): Long {
        return cacheDir(context).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun clear(context: Context) {
        runCatching {
            simpleCache?.release()
            simpleCache = null
            cacheDir(context).deleteRecursively()
            Logger.d(TAG, "播放器媒体缓存已清理")
        }.onFailure { error ->
            Logger.w(TAG, "播放器媒体缓存清理失败: ${error.message}")
        }
    }

    fun snapshotStats(): PlaybackMediaCacheStats {
        return PlaybackMediaCacheStats(
            upstreamBytes = upstreamBytes.get(),
            cachedBytes = cachedBytes.get(),
            ignoredCount = ignoredCount.get().toInt()
        )
    }

    fun logSeek(
        targetPositionMs: Long,
        currentPositionMs: Long,
        bufferedPositionMs: Long,
        durationMs: Long
    ) {
        val stats = snapshotStats()
        Logger.d(
            TAG,
            "seek target=$targetPositionMs current=$currentPositionMs buffered=$bufferedPositionMs " +
                "duration=$durationMs upstreamBytes=${stats.upstreamBytes} " +
                "cachedBytes=${stats.cachedBytes} ignored=${stats.ignoredCount}"
        )
    }

    private fun getOrCreateCache(context: Context): SimpleCache? {
        simpleCache?.let { return it }
        return synchronized(this) {
            simpleCache ?: runCatching {
                cacheDir(context).mkdirs()
                @Suppress("DEPRECATION")
                SimpleCache(
                    cacheDir(context),
                    LeastRecentlyUsedCacheEvictor(resolvePlaybackMediaCacheMaxBytes())
                )
            }.onSuccess {
                simpleCache = it
            }.onFailure { error ->
                Logger.w(TAG, "播放器媒体缓存初始化失败，降级为直接播放: ${error.message}")
            }.getOrNull()
        }
    }

    private fun cacheDir(context: Context): File {
        return File(context.cacheDir, PLAYBACK_MEDIA_CACHE_DIR)
    }

    private val playbackCacheKeyFactory = CacheKeyFactory { dataSpec ->
        val uri = dataSpec.uri
        if (shouldUsePlaybackMediaCache(uri)) {
            buildPlaybackCacheKey(uri = uri, explicitKey = dataSpec.key)
        } else {
            dataSpec.key ?: uri.toString()
        }
    }

    private val cacheEventListener = object : CacheDataSource.EventListener {
        override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
            cachedBytes.addAndGet(cachedBytesRead)
            Logger.d(TAG, "cache-hit bytes=$cachedBytesRead cacheSize=$cacheSizeBytes")
        }

        override fun onCacheIgnored(reason: Int) {
            ignoredCount.incrementAndGet()
            Logger.d(TAG, "cache-ignored reason=$reason")
        }
    }

    private val upstreamTransferListener = object : TransferListener {
        override fun onTransferInitializing(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean
        ) = Unit

        override fun onTransferStart(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean
        ) = Unit

        override fun onBytesTransferred(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean,
            bytesTransferred: Int
        ) {
            if (isNetwork) {
                upstreamBytes.addAndGet(bytesTransferred.toLong())
            }
        }

        override fun onTransferEnd(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean
        ) = Unit
    }
}
