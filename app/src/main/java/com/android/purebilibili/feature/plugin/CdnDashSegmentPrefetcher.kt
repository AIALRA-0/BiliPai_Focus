package com.android.purebilibili.feature.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.android.purebilibili.core.player.PlaybackMediaCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal data class CdnDashPrefetchRequest(
    val candidates: List<String>,
    val indexRange: CdnByteRange,
    val trackCacheKey: String,
    val bufferedDurationMs: Long,
    val frontierPositionMs: Long
)

internal data class CdnDashPrefetchResult(
    val plannedSegments: Int,
    val cachedSegments: Int,
    val selectedHosts: List<String>
)

/**
 * Stateless, caller-owned DASH prefetch operation. It is deliberately suspend-only so player
 * sessions control cancellation on seeks, low buffer, replacement, and teardown.
 */
internal class CdnDashSegmentPrefetcher(
    private val context: Context,
    private val client: OkHttpClient
) {
    // OkHttpDataSource 与 CacheWriter 属 media3 unstable API：预取链路在应用层封装后消费，opt-in 标记会级联污染全部调用方。
    @SuppressLint("UnsafeOptInUsageError")
    suspend fun prefetch(request: CdnDashPrefetchRequest): CdnDashPrefetchResult = withContext(Dispatchers.IO) {
        val targetCount = resolveCdnPrefetchSegmentCount(request.bufferedDurationMs)
        if (targetCount == 0 || request.candidates.isEmpty()) {
            return@withContext CdnDashPrefetchResult(0, 0, emptyList())
        }
        val index = loadIndex(request.candidates, request.indexRange) ?: return@withContext CdnDashPrefetchResult(0, 0, emptyList())
        val frontierUs = request.frontierPositionMs.coerceAtLeast(0L) * 1_000L
        val segments = index.segments
            .filter { it.startTimeUs >= frontierUs }
            .take(targetCount)
        val upstreamFactory = OkHttpDataSource.Factory(client).setDefaultRequestProperties(PLAYBACK_HEADERS)
        val selectedHosts = mutableListOf<String>()
        segments.forEach { segment ->
            currentCoroutineContext().ensureActive()
            val ranked = request.candidates
                .mapNotNull { url -> probe(url, segment.range)?.let { url to it } }
                .sortedBy { (_, elapsedMs) -> elapsedMs }
            val winner = ranked.firstOrNull()?.first ?: return@forEach
            try {
                PlaybackMediaCache.prefetchRange(
                    context = context,
                    upstreamFactory = upstreamFactory,
                    url = Uri.parse(winner),
                    cacheKey = request.trackCacheKey,
                    position = segment.range.start,
                    length = segment.range.length
                )
                selectedHosts += hostFromCdnUrl(winner)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
            }
        }
        CdnDashPrefetchResult(
            plannedSegments = segments.size,
            cachedSegments = selectedHosts.size,
            selectedHosts = selectedHosts
        )
    }

    private fun loadIndex(candidates: List<String>, range: CdnByteRange): CdnDashIndex? {
        candidates.forEach { url ->
            val bytes = readRange(url, range) ?: return@forEach
            parseCdnSidx(bytes, range.start)?.let { return it }
        }
        return null
    }

    private fun probe(url: String, segment: CdnByteRange): Long? {
        val probeEnd = minOf(segment.endInclusive, segment.start + CDN_PREFETCH_PROBE_BYTES - 1L)
        val startedAt = System.nanoTime()
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=${segment.start}-$probeEnd")
            .header("Referer", "https://www.bilibili.com")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code != 206 && !response.isSuccessful) return null
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    if (input.read(buffer) <= 0) return null
                }
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt).coerceAtLeast(1L)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            null
        }
    }

    private fun readRange(url: String, range: CdnByteRange): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=${range.start}-${range.endInclusive}")
            .header("Referer", "https://www.bilibili.com")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code != 206 && !response.isSuccessful) return null
                response.body.bytes()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            null
        }
    }

    private companion object {
        val PLAYBACK_HEADERS = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
    }
}
