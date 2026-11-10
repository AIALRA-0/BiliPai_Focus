package com.android.purebilibili.feature.plugin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.NetworkUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

/** Media3 calls this synchronous boundary on its loader thread. OkHttp owns cancellable I/O.
 * Finite ranges use bounded windows; unknown lengths, small reads and failures use the upstream.
 */
@UnstableApi
internal class CdnPlaybackDataSource(
    private val upstream: DataSource,
    private val background: Boolean = false,
    private val client: OkHttpClient? = null,
    private val isWifi: () -> Boolean = { NetworkModule.appContext?.let { NetworkUtils.isWifi(it) } == true }
) : BaseDataSource(true) {
    private var spec: DataSpec? = null
    private var candidates: List<String> = emptyList()
    private var consumed = 0L
    private var window = ByteArray(0)
    private var windowOffset = 0
    private var delegateOpen = false
    private var started = false
    private var parallel = false
    private var monitored = false
    private var delegateHost = ""
    private var delegateBytes = 0L
    private var delegateStartedNs = 0L
    private var delegateFirstByteMs: Long? = null
    @Volatile private var closed = true
    private val calls = ConcurrentHashMap.newKeySet<Call>()

    override fun open(dataSpec: DataSpec): Long {
        closed = false
        spec = dataSpec
        consumed = 0
        window = ByteArray(0)
        windowOffset = 0
        if (CdnTransferRuntime.enabled) {
            val manager = NetworkModule.appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            CdnTransferRuntime.networkChanged(manager?.activeNetwork?.toString().orEmpty())
        }
        candidates = CdnTransferRuntime.candidates(dataSpec.uri.toString())
        val observedMedia = CdnTransferRuntime.observeMediaUrl(dataSpec.uri.toString())
        monitored = candidates.isNotEmpty() || observedMedia
        val wifi = isWifi()
        parallel = candidates.isNotEmpty() && CdnTransferRuntime.parallelEnabled && wifi &&
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_GET && dataSpec.httpBody == null &&
            dataSpec.length >= CDN_MIN_PARALLEL_BYTES &&
            dataSpec.position >= 0 && dataSpec.length - 1 <= Long.MAX_VALUE - dataSpec.position
        transferInitializing(dataSpec)
        val length = if (parallel) dataSpec.length else openDelegate(dataSpec)
        started = true
        transferStarted(dataSpec)
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (closed || Thread.currentThread().isInterrupted) throw IOException("CDN read canceled")
        val current = spec ?: throw IOException("CDN source is not open")
        if (current.length != C.LENGTH_UNSET.toLong() && consumed >= current.length) return C.RESULT_END_OF_INPUT
        val count: Int
        if (!parallel) {
            try {
                count = upstream.read(buffer, offset, length)
            } catch (error: IOException) {
                if (monitored) finishDelegate(false, closed)
                throw error
            }
            if (count > 0 && monitored) recordDelegateBytes(count)
        } else {
            if (windowOffset == window.size) {
                try {
                    if (!CdnTransferRuntime.parallelEnabled ||
                        !isWifi()) {
                        throw IOException("CDN parallel mode disabled")
                    }
                    val startupWindow = if (CdnTransferRuntime.state.value.bufferMs < 5_000) CDN_WINDOW_BYTES / 2 else CDN_WINDOW_BYTES
                    val size = minOf(startupWindow, current.length - consumed)
                    window = downloadWindow(current, current.position + consumed, size)
                    windowOffset = 0
                } catch (error: IOException) {
                    cancelCalls()
                    if (closed || Thread.currentThread().isInterrupted || background) throw error
                    CdnTransferRuntime.fallback()
                    parallel = false
                    // Resume at delivered position, never restart or expose a partial window.
                    openDelegate(current.subrange(consumed))
                    return read(buffer, offset, length)
                }
            }
            count = minOf(length, window.size - windowOffset)
            window.copyInto(buffer, offset, windowOffset, windowOffset + count)
            windowOffset += count
        }
        if (closed) throw IOException("CDN read canceled")
        if (count > 0) {
            consumed += count
            bytesTransferred(count)
            if (monitored) CdnTransferRuntime.delivered(count)
        } else if (count == C.RESULT_END_OF_INPUT && monitored) {
            finishDelegate(true)
        }
        return count
    }

    override fun getUri(): Uri? = if (delegateOpen) upstream.uri else spec?.uri
    override fun getResponseHeaders(): Map<String, List<String>> = if (delegateOpen) upstream.responseHeaders else emptyMap()

    fun cancelPendingRequests() {
        closed = true
        cancelCalls()
        if (delegateOpen) upstream.close()
    }

    override fun close() {
        closed = true
        cancelCalls()
        try {
            if (delegateOpen) upstream.close()
        } finally {
            // CacheWriter can stop at its finite range without an EOF read.
            val current = spec
            val complete = current != null && current.length >= 0 && consumed >= current.length
            if (monitored) finishDelegate(complete, canceled = true)
            delegateOpen = false
            window = ByteArray(0)
            spec = null
            if (started) { started = false; transferEnded() }
        }
    }

    private fun openDelegate(dataSpec: DataSpec): Long {
        delegateHost = hostFromCdnUrl(dataSpec.uri.toString())
        delegateBytes = 0
        delegateStartedNs = System.nanoTime()
        delegateFirstByteMs = null
        if (monitored) CdnTransferRuntime.start(delegateHost)
        // Set before open so failed opens can still release upstream resources in close().
        delegateOpen = true
        return try { upstream.open(dataSpec) } catch (error: IOException) {
            if (monitored) finishDelegate(false, closed)
            throw error
        }
    }

    private fun recordDelegateBytes(count: Int) {
        delegateBytes += count
        val elapsedMs = ((System.nanoTime() - delegateStartedNs) / 1_000_000).coerceAtLeast(1)
        if (delegateFirstByteMs == null) delegateFirstByteMs = elapsedMs
        CdnTransferRuntime.progress(delegateHost, count, delegateBytes * 1_000 / elapsedMs, delegateFirstByteMs)
    }

    private fun finishDelegate(success: Boolean, canceled: Boolean = false) {
        if (delegateHost.isNotEmpty()) {
            CdnTransferRuntime.finish(delegateHost, success, canceled = canceled)
            delegateHost = ""
        }
    }

    private class Attempt(val range: CdnByteRange, val url: String, prefix: ByteArray = ByteArray(0)) {
        val bytes = ByteArray(range.length.toInt()).also { prefix.copyInto(it) }
        @Volatile var received = prefix.size
        @Volatile var lastProgressNs = System.nanoTime()
        val result = CompletableFuture<ByteArray>()
        @Volatile var call: Call? = null
        var status: Int? = null
    }

    private fun downloadWindow(dataSpec: DataSpec, start: Long, length: Long): ByteArray {
        val urls = CdnTransferRuntime.rank(candidates)
        if (background && CdnTransferRuntime.state.value.bufferMs < CDN_PREFETCH_SAFE_BUFFER_MS) {
            throw IOException("CDN prefetch yields to playback")
        }
        val limit = if (background) 1 else CdnTransferRuntime.connectionLimit()
        val count = minOf(limit, CdnTransferRuntime.permits.availablePermits().coerceAtLeast(1))
        val pieces = splitCdnRange(start, length, count)
        val total = AtomicLong(-1)
        if (background && CdnTransferRuntime.permits.availablePermits() <= 1) throw IOException("CDN prefetch yields its slot")
        if (!CdnTransferRuntime.permits.tryAcquire(count)) throw IOException("CDN connection budget exhausted")
        val attempts = pieces.mapIndexed { index, range ->
            val selected = if (CdnTransferRuntime.state.value.preferredHost != null) urls.first() else urls[index % urls.size]
            launchAttempt(dataSpec, range, selected, total, reserved = true)
                ?: throw IOException("CDN connection budget exhausted")
        }
        val output = ByteArray(length.toInt())
        var outputOffset = 0
        for (primary in attempts) {
            var rescue: Attempt? = null
            var retry: Attempt? = null
            var winner: Attempt? = null
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12)
            while (winner == null) {
                if (closed || Thread.currentThread().isInterrupted) throw IOException("CDN read canceled")
                val active = listOfNotNull(primary, rescue, retry)
                winner = active.firstOrNull { it.result.isDone && !it.result.isCompletedExceptionally }
                if (winner != null) break
                if (System.nanoTime() >= deadline) throw IOException("CDN window timed out")
                if (active.all { it.result.isCompletedExceptionally }) {
                    if (retry != null || primary.status == 403 || primary.status == 412 || primary.status == 429) {
                        throw IOException("CDN range failed")
                    }
                    // Only validated contiguous bytes can be carried into a tail retry.
                    val best = active.maxBy { it.received }
                    val nextUrl = urls.firstOrNull { it != best.url } ?: best.url
                    retry = launchAttempt(dataSpec, primary.range, nextUrl, total, best.bytes.copyOf(best.received))
                    if (retry == null) throw IOException("CDN retry budget exhausted")
                    CdnTransferRuntime.resumed()
                } else if (!background && CdnTransferRuntime.state.value.bufferMs < 15_000 &&
                    rescue == null && retry == null && urls.size > 1 &&
                    System.nanoTime() - primary.lastProgressNs > TimeUnit.MILLISECONDS.toNanos(900)) {
                    val nextUrl = urls.firstOrNull { hostFromCdnUrl(it) != hostFromCdnUrl(primary.url) }
                    if (nextUrl != null) {
                        rescue = launchAttempt(
                            dataSpec, primary.range, nextUrl, total,
                            primary.bytes.copyOf(minOf(primary.received, primary.bytes.size - 1))
                        )
                        if (rescue != null) CdnTransferRuntime.rescue()
                    }
                }
                try {
                    CompletableFuture.anyOf(*active.filterNot { it.result.isDone }.map { it.result }.toTypedArray())
                        .get(100, TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    // Wake periodically for cancellation and stalled-piece rescue.
                } catch (_: ExecutionException) {
                    // The loop either finds a valid winner or retries a failed tail.
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("CDN read interrupted", error)
                }
            }
            val completed = winner ?: throw IOException("CDN missing piece")
            listOfNotNull(primary, rescue, retry).filter { it !== completed }.forEach { it.call?.cancel() }
            completed.bytes.copyInto(output, outputOffset)
            outputOffset += completed.bytes.size
        }
        return output
    }

    private fun launchAttempt(
        dataSpec: DataSpec,
        range: CdnByteRange,
        url: String,
        total: AtomicLong,
        prefix: ByteArray = ByteArray(0),
        reserved: Boolean = false
    ): Attempt? {
        if (!reserved && (closed || (background && CdnTransferRuntime.permits.availablePermits() <= 1) ||
                !CdnTransferRuntime.permits.tryAcquire())) return null
        val attempt = Attempt(range, url, prefix)
        if (closed) {
            CdnTransferRuntime.permits.release()
            attempt.result.completeExceptionally(IOException("CDN read canceled"))
            return attempt
        }
        if (prefix.size == attempt.bytes.size) {
            CdnTransferRuntime.permits.release()
            attempt.result.complete(attempt.bytes)
            return attempt
        }
        val requested = CdnByteRange(range.start + prefix.size, range.endInclusive)
        val call = try {
            val request = Request.Builder().url(url)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0")
                .apply { dataSpec.httpRequestHeaders.forEach { (name, value) -> header(name, value) } }
                .header("Range", "bytes=${requested.start}-${requested.endInclusive}")
                .header("Accept-Encoding", "identity")
                .build()
            (client ?: rangeClient).newCall(request)
        } catch (error: IllegalArgumentException) {
            CdnTransferRuntime.permits.release()
            attempt.result.completeExceptionally(IOException("CDN invalid request", error))
            return attempt
        }
        attempt.call = call
        calls += call
        val host = hostFromCdnUrl(url)
        val startedNs = System.nanoTime()
        CdnTransferRuntime.start(host)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                end(call, false, call.isCanceled())
                attempt.result.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                var success = false
                var failure: Exception? = null
                try {
                    response.use {
                        attempt.status = it.code
                        if (!isExactCdnRange(it.code, it.header("Content-Range"), requested)) throw IOException("CDN invalid range")
                        val actual = parseCdnContentRange(it.header("Content-Range")) ?: throw IOException("CDN missing range")
                        total.compareAndSet(-1, actual.total)
                        if (total.get() != actual.total) throw IOException("CDN mirrors disagree on length")
                        val body = it.body ?: throw IOException("CDN empty body")
                        body.byteStream().use { input ->
                            var firstByteMs: Long? = null
                            while (attempt.received < attempt.bytes.size) {
                                if (call.isCanceled() || closed) throw IOException("CDN read canceled")
                                val read = input.read(attempt.bytes, attempt.received, attempt.bytes.size - attempt.received)
                                if (read < 0) throw IOException("CDN truncated range")
                                if (read == 0) continue
                                attempt.received += read
                                attempt.lastProgressNs = System.nanoTime()
                                val elapsedMs = ((attempt.lastProgressNs - startedNs) / 1_000_000).coerceAtLeast(1)
                                if (firstByteMs == null) firstByteMs = elapsedMs
                                CdnTransferRuntime.progress(host, read, (attempt.received - prefix.size) * 1_000L / elapsedMs, firstByteMs)
                            }
                            if (input.read() != -1) throw IOException("CDN oversized range")
                        }
                    }
                    success = true
                } catch (error: Exception) {
                    // Invalid/oversized bodies must not be carried into a resumed request.
                    if (attempt.received == attempt.bytes.size) attempt.received = 0
                    failure = error
                } finally {
                    end(call, success, call.isCanceled())
                }
                // Release the shared slot before the loader schedules another window.
                if (success) attempt.result.complete(attempt.bytes)
                else attempt.result.completeExceptionally(failure ?: IOException("CDN range failed"))
            }

            private fun end(call: Call, success: Boolean, canceled: Boolean) {
                calls -= call
                CdnTransferRuntime.permits.release()
                CdnTransferRuntime.finish(host, success, attempt.status, canceled)
            }
        })
        if (closed) call.cancel()
        return attempt
    }

    private fun cancelCalls() { calls.forEach { it.cancel() } }

    companion object {
        // A shared dispatcher and connection pool; no per-player executor or hidden scope.
        private val rangeClient: OkHttpClient by lazy {
            NetworkModule.playbackOkHttpClient.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(8, TimeUnit.SECONDS)
                .build()
        }
    }
}
