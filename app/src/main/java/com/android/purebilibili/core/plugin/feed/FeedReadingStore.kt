package com.android.purebilibili.core.plugin.feed

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FeedReadingSnapshot(
    val items: List<ParsedFeedItem> = emptyList(),
    val readKeys: List<String> = emptyList(),
    val fullBodies: Map<String, String> = emptyMap(),
    //  [回顾] key→已读时间戳(ms) 与 阅读进度(0-100)，供"今日/七天/一月"回顾与续读。
    val readTimestamps: Map<String, Long> = emptyMap(),
    val readProgress: Map<String, Int> = emptyMap(),
)

internal fun updateReadTimestamps(
    timestamps: Map<String, Long>,
    key: String,
    read: Boolean,
    atMs: Long,
): Map<String, Long> {
    val next = timestamps.filterNot { it.key == key }
    return if (read) (next + (key to atMs)).toList().sortedByDescending { it.second }.take(2_000).toMap()
    else next
}

internal fun updateReadProgress(progress: Map<String, Int>, key: String, percent: Int): Map<String, Int> {
    val clamped = percent.coerceIn(0, 100)
    val next = progress.filterNot { it.key == key }
    if (clamped <= 0) return next
    return (next + (key to clamped)).toList().takeLast(200).toMap()
}

internal fun feedItemKey(item: ParsedFeedItem): String = "${item.sourceId}\u001f${item.id}"

internal fun updateReadKeys(keys: List<String>, key: String, read: Boolean): List<String> =
    (keys.filterNot { it == key } + if (read) listOf(key) else emptyList()).takeLast(2_000)

internal fun mergeCachedFeedItems(
    cached: List<ParsedFeedItem>,
    fresh: List<ParsedFeedItem>,
    enabledSourceIds: Set<String>,
): List<ParsedFeedItem> = (fresh + cached)
    .filter { it.sourceId in enabledSourceIds }
    .distinctBy(::feedItemKey)
    .sortedWith(compareBy<ParsedFeedItem> { it.publishedEpochSec == null }
        .thenByDescending { it.publishedEpochSec ?: 0L })
    .take(120)

object FeedReadingStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun load(context: Context): FeedReadingSnapshot = lockedIo { read(context) }

    suspend fun saveItems(context: Context, items: List<ParsedFeedItem>) = lockedIo {
            val previous = read(context)
            val compact = items.take(120).map { item ->
                if (item.htmlContent.length > 40_000) item.copy(htmlContent = item.summary) else item
            }
            write(context, previous.copy(items = compact))
    }

    suspend fun setRead(context: Context, key: String, read: Boolean) = lockedIo {
            val previous = read(context)
            write(context, previous.copy(readKeys = updateReadKeys(previous.readKeys, key, read)))
    }

    /** 打开/标记已读时记录时间戳，供回顾板块按今日/七天/一月统计。 */
    suspend fun recordRead(context: Context, key: String, atMs: Long = System.currentTimeMillis()) = lockedIo {
        val previous = read(context)
        write(
            context,
            previous.copy(
                readKeys = updateReadKeys(previous.readKeys, key, true),
                readTimestamps = updateReadTimestamps(previous.readTimestamps, key, true, atMs),
            ),
        )
    }

    suspend fun recordProgress(context: Context, key: String, percent: Int) = lockedIo {
        val previous = read(context)
        write(context, previous.copy(readProgress = updateReadProgress(previous.readProgress, key, percent)))
    }

    /** 按时间倒序返回窗口内已读条目（key→readAtMs）。 */
    suspend fun recentReads(context: Context, windowStartMs: Long): List<Pair<String, Long>> = lockedIo {
        read(context).readTimestamps
            .filterValues { it >= windowStartMs }
            .toList()
            .sortedByDescending { it.second }
    }

    suspend fun readProgress(context: Context, key: String): Int = lockedIo {
        read(context).readProgress[key] ?: 0
    }

    suspend fun loadTimestamps(context: Context): Map<String, Long> =
        lockedIo { read(context).readTimestamps }

    suspend fun saveFullBody(context: Context, key: String, html: String) = lockedIo {
        if (html.length <= 100_000) {
            val previous = read(context)
            val bodies = (previous.fullBodies.toList().filterNot { it.first == key } + (key to html))
                .takeLast(20).toMap()
            write(context, previous.copy(fullBodies = bodies))
        }
    }

    private suspend fun <T> lockedIo(block: () -> T): T {
        mutex.lock()
        try {
            return withContext(Dispatchers.IO) { block() }
        } finally {
            mutex.unlock()
        }
    }

    private fun read(context: Context): FeedReadingSnapshot {
        val file = file(context)
        if (!file.baseFile.exists()) return FeedReadingSnapshot()
        return runCatching {
            file.openRead().bufferedReader().use { json.decodeFromString<FeedReadingSnapshot>(it.readText()) }
        }.getOrDefault(FeedReadingSnapshot())
    }

    private fun write(context: Context, data: FeedReadingSnapshot) {
        val file = file(context)
        file.baseFile.parentFile?.mkdirs()
        val stream = file.startWrite()
        try {
            stream.writer(Charsets.UTF_8).apply { write(json.encodeToString(data)); flush() }
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.filesDir, "plugin/subscription_reading.json"))
}
