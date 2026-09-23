package com.android.purebilibili.core.plugin.feed

import com.android.purebilibili.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

private const val FEED_BODY_LIMIT_BYTES = 2 * 1024 * 1024
private const val FEED_REQUEST_TIMEOUT_MS = 8_000L

private val feedHttpClient: OkHttpClient by lazy {
    NetworkModule.okHttpClient.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()
}

suspend fun fetchArticleHtml(url: String): Result<String> = withContext(Dispatchers.IO) {
    val call = if (isHttpFeedUrl(url)) {
        feedHttpClient.newCall(Request.Builder().url(url.trim()).header("User-Agent", "Mozilla/5.0 BiliPai Feed").build())
    } else null
    if (call == null) return@withContext Result.failure(IllegalArgumentException("原文地址无效"))
    coroutineContext[Job]?.invokeOnCompletion { call.cancel() }
    runCatching {
        call.execute().use { response ->
            if (!response.isSuccessful) error("原文请求失败 ${response.code}")
            val stream = response.body.byteStream()
            val buffer = ByteArray(8 * 1024)
            val output = ByteArrayOutputStream()
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (output.size() + read > FEED_BODY_LIMIT_BYTES) error("原文内容超过 2MB")
                output.write(buffer, 0, read)
            }
            val charset = response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val page = output.toString(charset.name())
            extractArticleBody(page) ?: error("暂时无法提取原文正文")
        }
    }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
}

suspend fun fetchFeedXml(url: String): Result<String> = withContext(Dispatchers.IO) {
    if (!isHttpFeedUrl(url)) {
        return@withContext Result.failure(IllegalArgumentException("只接受 http 或 https 订阅地址"))
    }
    val request = Request.Builder()
        .url(url.trim())
        .header("User-Agent", "BiliPai Feed")
        .build()
    val call = feedHttpClient.newCall(request)
    coroutineContext[Job]?.invokeOnCompletion { call.cancel() }
    try {
        withTimeout(FEED_REQUEST_TIMEOUT_MS) {
            runCatching {
                call.execute().use { response ->
                    if (!response.isSuccessful) error("订阅请求失败 ${response.code}")
                    val stream = response.body.byteStream()
                    val buffer = ByteArray(8 * 1024)
                    val output = ByteArrayOutputStream()
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        if (output.size() + read > FEED_BODY_LIMIT_BYTES) error("订阅内容超过 2MB")
                        output.write(buffer, 0, read)
                    }
                    val bytes = output.toByteArray()
                    val declaration = bytes.copyOfRange(0, minOf(bytes.size, 256)).toString(Charsets.ISO_8859_1)
                    val declaredCharset = Regex("""encoding\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        .find(declaration)?.groupValues?.getOrNull(1)
                        ?.let { runCatching { Charset.forName(it) }.getOrNull() }
                    val charset = response.body.contentType()?.charset() ?: declaredCharset ?: Charsets.UTF_8
                    String(bytes, charset).removePrefix("\uFEFF")
                }
            }
        }
    } catch (_: TimeoutCancellationException) {
        call.cancel()
        Result.failure(IllegalStateException("连接超时"))
    }
}

internal fun friendlyFeedError(sourceTitle: String, error: Throwable): String {
    val raw = error.message.orEmpty()
    val reason = when {
        raw.contains("超时") || raw.contains("timeout", ignoreCase = true) -> "连接超时"
        raw.contains("订阅请求失败") -> "请求失败 ${raw.substringAfter("订阅请求失败").trim()}"
        raw.contains("failed to connect", ignoreCase = true) ||
            raw.contains("unable to resolve", ignoreCase = true) -> "连不上"
        raw.isBlank() -> "加载失败"
        else -> "暂时打不开"
    }
    return "$sourceTitle：$reason"
}

suspend fun loadFeedSources(
    sources: List<FeedSource>,
    onUpdate: (FeedLoadSnapshot) -> Unit = {},
): FeedLoadSnapshot = supervisorScope {
    val items = mutableListOf<ParsedFeedItem>()
    val errors = mutableListOf<String>()
    fun publish(): FeedLoadSnapshot {
        val sorted = items.sortedWith(
            compareBy<ParsedFeedItem> { it.publishedEpochSec == null }
                .thenByDescending { it.publishedEpochSec ?: 0L }
        )
        return FeedLoadSnapshot(items = sorted.toList(), errors = errors.toList())
    }
    val gate = Semaphore(4)
    sources.map { source ->
        async(Dispatchers.IO) {
            val outcome = gate.withPermit {
                fetchFeedXml(source.url).mapCatching { xml ->
                    parseFeedDocument(xml, source.id, source.title, source.url)
                }
            }
            val snapshot = synchronized(items) {
                outcome
                    .onSuccess { feed -> items += feed.items }
                    .onFailure { error -> errors += friendlyFeedError(source.title, error) }
                publish()
            }
            withContext(Dispatchers.Main) { onUpdate(snapshot) }
        }
    }.awaitAll()
    synchronized(items) { publish() }
}
