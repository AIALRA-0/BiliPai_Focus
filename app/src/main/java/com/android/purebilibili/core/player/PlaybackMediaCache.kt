package com.android.purebilibili.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.android.purebilibili.feature.plugin.CdnPlaybackDataSource
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.net.URI
import java.util.LinkedHashSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "PlaybackMediaCache"
private const val PLAYBACK_MEDIA_CACHE_DIR = "playback_media_cache"

internal data class PlaybackMediaCacheStats(
    val upstreamBytes: Long,
    val cachedBytes: Long,
    val ignoredCount: Int
)

// 128MB：跨会话分片缓存只服务「回看刚看过的片段」，不需要装下整个视频；
// 512MB 上限曾让 cache 目录轻松涨到 500MB+（用户反馈缓存过大、跳看流量浪费）。
internal fun resolvePlaybackMediaCacheMaxBytes(): Long = 128L * 1024L * 1024L

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

    private var simpleCache: SimpleCache? = null
    private val cacheLock = ReentrantLock()
    private val cacheStateChanged: Condition = cacheLock.newCondition()
    private var activeCacheLeases = 0
    private var cacheClearPending = false

    fun buildCachedDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory
    ): DataSource.Factory {
        val appContext = context.applicationContext
        val monitoredUpstreamFactory = DataSource.Factory {
            CdnPlaybackDataSource(upstreamFactory.createDataSource()).apply {
                addTransferListener(upstreamTransferListener)
            }
        }
        return createLazyCacheDataSourceFactory(upstreamFactory) {
            val lease = acquireCache(appContext)
            if (lease == null) {
                null
            } else {
                try {
                    val cachedDataSource = CacheDataSource.Factory()
                        .setCache(lease.cache)
                        .setUpstreamDataSourceFactory(monitoredUpstreamFactory)
                        .setCacheKeyFactory(playbackCacheKeyFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        .setEventListener(cacheEventListener)
                        .createDataSource()
                    CacheLeaseDataSource(cachedDataSource, lease)
                } catch (error: Throwable) {
                    lease.close()
                    throw error
                }
            }
        }
    }

    /**
     * Gives mirrors of one authorized DASH track the same cache spans without changing the URL
     * used for the actual request. Unknown URLs retain the normal cache-key behavior.
     */
    fun buildCdnOptimizedDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory,
        cacheKeysByUrl: Map<String, String>
    ): DataSource.Factory {
        val cachedFactory = buildCachedDataSourceFactory(context, upstreamFactory)
        if (cacheKeysByUrl.isEmpty()) return cachedFactory
        return ResolvingDataSource.Factory(cachedFactory) { dataSpec ->
            val cacheKey = cacheKeysByUrl[dataSpec.uri.toString()]
            if (cacheKey.isNullOrBlank()) dataSpec else dataSpec.buildUpon().setKey(cacheKey).build()
        }
    }

    /** Caller-owned operation; cancellation closes active media requests and stops cache writes. */
    suspend fun prefetchRange(
        context: Context,
        upstreamFactory: DataSource.Factory,
        url: Uri,
        cacheKey: String,
        position: Long,
        length: Long
    ) {
        if (length <= 0L) return
        val lease = acquireCache(context.applicationContext) ?: return
        try {
            val activeUpstream = AtomicReference<CdnPlaybackDataSource?>()
            val cancelableFactory = DataSource.Factory {
                CdnPlaybackDataSource(upstreamFactory.createDataSource(), background = true)
                    .also(activeUpstream::set)
            }
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(lease.cache)
                .setUpstreamDataSourceFactory(cancelableFactory)
                .setCacheKeyFactory(playbackCacheKeyFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSourceForDownloading()
            val writer = CacheWriter(
                cacheDataSource,
                DataSpec.Builder()
                    .setUri(url)
                    .setKey(cacheKey)
                    .setPosition(position)
                    .setLength(length)
                    .build(),
                null,
                null
            )
            coroutineScope {
                val reader = async(Dispatchers.IO) { writer.cache() }
                try {
                    reader.await()
                } finally {
                    writer.cancel()
                    // CacheWriter closes the cache data source; interrupt its active transport on cancellation.
                    activeUpstream.get()?.cancelPendingRequests()
                }
            }
        } finally {
            lease.close()
        }
    }

    fun estimateBytes(context: Context): Long {
        return cacheDir(context).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun clear(context: Context): Result<Unit> {
        val clearNow = cacheLock.withLock {
            while (cacheClearPending) cacheStateChanged.awaitUninterruptibly()
            cacheClearPending = true
            if (activeCacheLeases == 0) {
                true to simpleCache.also { simpleCache = null }
            } else {
                false to null
            }
        }
        if (!clearNow.first) {
            Logger.d(TAG, "播放器媒体缓存将在当前读取结束后清理")
            return Result.success(Unit)
        } else {
            return finishClear(context.applicationContext, clearNow.second)
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

    private fun acquireCache(context: Context): CacheLease? = cacheLock.withLock {
        while (cacheClearPending) cacheStateChanged.awaitUninterruptibly()
        val cache = simpleCache ?: runCatching {
            cacheDir(context).mkdirs()
            @Suppress("DEPRECATION")
            SimpleCache(
                cacheDir(context),
                LeastRecentlyUsedCacheEvictor(resolvePlaybackMediaCacheMaxBytes())
            )
        }.onFailure { error ->
            Logger.w(TAG, "播放器媒体缓存初始化失败，降级为直接播放: ${error.message}")
        }.getOrNull()
        if (cache == null) return null
        simpleCache = cache
        activeCacheLeases += 1
        CacheLease(cache, context.applicationContext)
    }

    private fun releaseCacheLease(context: Context) {
        val clearNow = cacheLock.withLock {
            check(activeCacheLeases > 0) { "Playback cache lease released more than once" }
            activeCacheLeases -= 1
            if (activeCacheLeases == 0 && cacheClearPending) {
                true to simpleCache.also { simpleCache = null }
            } else {
                false to null
            }
        }
        if (clearNow.first) {
            finishClear(context, clearNow.second)
        }
    }

    private fun finishClear(context: Context, cache: SimpleCache?): Result<Unit> {
        val result = runCatching {
            cache?.release()
            val directory = cacheDir(context)
            check(!directory.exists() || directory.deleteRecursively()) { "部分媒体分片缓存无法清理" }
            Logger.d(TAG, "播放器媒体缓存已清理")
        }
        result.onFailure { error ->
            Logger.w(TAG, "播放器媒体缓存清理失败: ${error.message}")
        }
        try {
            return result
        } finally {
            cacheLock.withLock {
                cacheClearPending = false
                cacheStateChanged.signalAll()
            }
        }
    }

    private class CacheLease(
        val cache: SimpleCache,
        private val context: Context
    ) : AutoCloseable {
        private var closed = false

        override fun close() {
            val shouldRelease = synchronized(this) {
                if (closed) false else {
                    closed = true
                    true
                }
            }
            if (shouldRelease) releaseCacheLease(context)
        }
    }

    private class CacheLeaseDataSource(
        private val delegate: DataSource,
        private val lease: CacheLease
    ) : DataSource {
        private val closed = AtomicBoolean(false)

        override fun open(dataSpec: DataSpec): Long = delegate.open(dataSpec)

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length)

        override fun getUri(): Uri? = delegate.uri

        override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

        override fun addTransferListener(transferListener: TransferListener) {
            delegate.addTransferListener(transferListener)
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            try {
                delegate.close()
            } finally {
                lease.close()
            }
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

/**
 * Creates a data source without resolving its cache delegate until Media3 opens it. Media3 opens
 * playback data sources from its loading path, keeping SimpleCache construction off Compose/Main.
 */
@UnstableApi
internal fun createLazyCacheDataSourceFactory(
    upstreamFactory: DataSource.Factory,
    cachedDataSourceProvider: () -> DataSource?
): DataSource.Factory = DataSource.Factory {
    LazyCacheDataSource(upstreamFactory, cachedDataSourceProvider)
}

@UnstableApi
private class LazyCacheDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val cachedDataSourceProvider: () -> DataSource?
) : DataSource {
    private val lock = Any()
    private val transferListeners = LinkedHashSet<TransferListener>()
    private var delegate: DataSource? = null
    private var opening = false

    override fun open(dataSpec: DataSpec): Long {
        synchronized(lock) {
            check(delegate == null && !opening) { "DataSource must be closed before it is reopened" }
            opening = true
        }

        val selectedDataSource = try {
            val cached = try {
                cachedDataSourceProvider()
            } catch (error: Exception) {
                Logger.w(TAG, "播放器缓存不可用，降级为直接播放: ${error.message}")
                null
            }
            cached ?: upstreamFactory.createDataSource()
        } catch (error: Throwable) {
            synchronized(lock) { opening = false }
            throw error
        }

        try {
            synchronized(lock) {
                transferListeners.forEach(selectedDataSource::addTransferListener)
                delegate = selectedDataSource
                opening = false
            }
        } catch (error: Throwable) {
            synchronized(lock) { opening = false }
            runCatching { selectedDataSource.close() }
            throw error
        }

        return try {
            selectedDataSource.open(dataSpec)
        } catch (error: Throwable) {
            runCatching { close() }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        activeDataSource().read(buffer, offset, length)

    override fun getUri(): Uri? = synchronized(lock) { delegate?.uri }

    override fun getResponseHeaders(): Map<String, List<String>> =
        synchronized(lock) { delegate?.responseHeaders ?: emptyMap() }

    override fun addTransferListener(transferListener: TransferListener) {
        synchronized(lock) {
            if (transferListeners.add(transferListener)) {
                delegate?.addTransferListener(transferListener)
            }
        }
    }

    override fun close() {
        val current = synchronized(lock) {
            delegate.also { delegate = null }
        }
        current?.close()
    }

    private fun activeDataSource(): DataSource = synchronized(lock) {
        checkNotNull(delegate) { "DataSource must be opened before it is read" }
    }
}
