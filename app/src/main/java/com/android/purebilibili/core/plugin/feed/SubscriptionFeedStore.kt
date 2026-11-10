package com.android.purebilibili.core.plugin.feed

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SavedSubscriptionFeed(
    val id: String,
    val title: String,
    val url: String,
    val enabled: Boolean = true,
    /** 分组名，空串表示未分组；OPML 导入时取父级文件夹名。 */
    val group: String = "",
)

object SubscriptionFeedStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()
    private val mutex = Mutex()

    suspend fun list(context: Context): List<SavedSubscriptionFeed> = mutex.withLock {
        withContext(Dispatchers.IO) { read(context) }
    }

    /** 同步读取：仅供无法改为挂起调用的同步路径（如 FeedSourceCatalog）使用；写路径一律走 suspend。 */
    fun listBlocking(context: Context): List<SavedSubscriptionFeed> = read(context)

    suspend fun addAll(context: Context, imported: List<ImportedSubscription>): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val current = read(context).toMutableList()
            val seen = current.map { it.url }.toMutableSet()
            val usedIds = current.map { it.id }.toMutableSet()
            var added = 0
            imported.forEach { item ->
                val url = item.url.trim()
                if (!isHttpFeedUrl(url) || !seen.add(url)) return@forEach
                val feed = SavedSubscriptionFeed(
                    id = uniqueFeedId(url, usedIds),
                    title = item.title.trim().ifBlank { url },
                    url = url,
                    group = item.group.trim(),
                )
                usedIds += feed.id
                current += feed
                added += 1
            }
            if (added > 0) write(context, current)
            added
        }
    }

    suspend fun add(context: Context, title: String, url: String): Result<SavedSubscriptionFeed> {
        val trimmedUrl = url.trim()
        if (!isHttpFeedUrl(trimmedUrl)) {
            return Result.failure(IllegalArgumentException("只接受 http 或 https 订阅地址"))
        }
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = read(context)
                val feed = SavedSubscriptionFeed(
                    id = uniqueFeedId(trimmedUrl, current.map { it.id }.toSet()),
                    title = title.trim().ifBlank { trimmedUrl },
                    url = trimmedUrl,
                )
                write(context, current.filterNot { it.url == feed.url } + feed)
                Result.success(feed)
            }
        }
    }

    suspend fun remove(context: Context, id: String) {
        removeAll(context, setOf(id))
    }

    suspend fun removeAll(context: Context, ids: Set<String>): Int {
        if (ids.isEmpty()) return 0
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = read(context)
                val remaining = current.filterNot { it.id in ids }
                val removed = current.size - remaining.size
                if (removed > 0) write(context, remaining)
                removed
            }
        }
    }

    suspend fun setEnabled(context: Context, id: String, enabled: Boolean) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                write(context, read(context).map { if (it.id == id) it.copy(enabled = enabled) else it })
            }
        }
    }

    /** 后台标题补全用：仅在标题确实变化时写入，避免 revision 空转。 */
    suspend fun updateTitle(context: Context, id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = read(context)
                if (current.none { it.id == id && it.title != trimmed }) return@withContext
                write(context, current.map { if (it.id == id) it.copy(title = trimmed) else it })
            }
        }
    }

    /** 设置分组；空串表示移出分组。仅变化时写入。 */
    suspend fun setGroup(context: Context, id: String, group: String) {
        val normalized = group.trim()
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = read(context)
                if (current.none { it.id == id && it.group != normalized }) return@withContext
                write(context, current.map { if (it.id == id) it.copy(group = normalized) else it })
            }
        }
    }

    /** url 的 32 位哈希；与现有条目冲突时追加序号，避免两个订阅共用 id 造成连删/筛选串台。 */
    private fun uniqueFeedId(url: String, usedIds: Set<String>): String {
        val base = url.hashCode().toUInt().toString(16)
        if (base !in usedIds) return base
        var suffix = 1
        while (true) {
            val candidate = "$base-$suffix"
            if (candidate !in usedIds) return candidate
            suffix += 1
        }
    }

    private fun write(context: Context, feeds: List<SavedSubscriptionFeed>) {
        val file = file(context)
        file.baseFile.parentFile?.mkdirs()
        val stream = file.startWrite()
        try {
            stream.writer(Charsets.UTF_8).apply { write(json.encodeToString(feeds)); flush() }
            file.finishWrite(stream)
            _revision.value += 1
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun read(context: Context): List<SavedSubscriptionFeed> {
        val file = file(context)
        if (!file.baseFile.exists()) return emptyList()
        return runCatching {
            file.openRead().bufferedReader().use {
                json.decodeFromString<List<SavedSubscriptionFeed>>(it.readText())
            }
        }.getOrElse { error ->
            // 损坏文件不能静默当作空列表处理：那会把下一次写入固化成“全部订阅消失”。
            // 保留坏文件以便排查，返回空并给出显式日志。
            com.android.purebilibili.core.util.Logger.w(
                "SubscriptionFeedStore",
                "订阅列表文件解析失败，暂按空列表处理: ${error.message}"
            )
            emptyList()
        }
    }

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.filesDir, "plugin/subscription_feeds.json"))
}
