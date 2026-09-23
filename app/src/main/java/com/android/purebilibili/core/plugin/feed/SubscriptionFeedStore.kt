package com.android.purebilibili.core.plugin.feed

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SavedSubscriptionFeed(
    val id: String,
    val title: String,
    val url: String,
    val enabled: Boolean = true,
)

object SubscriptionFeedStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun list(context: Context): List<SavedSubscriptionFeed> {
        val file = file(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<SavedSubscriptionFeed>>(file.readText())
        }.getOrDefault(emptyList())
    }

    fun addAll(context: Context, imported: List<ImportedSubscription>): Int {
        if (imported.isEmpty()) return 0
        val current = list(context).toMutableList()
        val seen = current.map { it.url }.toMutableSet()
        var added = 0
        imported.forEach { item ->
            val url = item.url.trim()
            if (!isHttpFeedUrl(url) || !seen.add(url)) return@forEach
            current += SavedSubscriptionFeed(
                id = url.hashCode().toUInt().toString(16),
                title = item.title.trim().ifBlank { url },
                url = url,
            )
            added += 1
        }
        if (added > 0) write(context, current)
        return added
    }

    fun add(context: Context, title: String, url: String): Result<SavedSubscriptionFeed> {
        val trimmedUrl = url.trim()
        if (!isHttpFeedUrl(trimmedUrl)) {
            return Result.failure(IllegalArgumentException("只接受 http 或 https 订阅地址"))
        }
        val feed = SavedSubscriptionFeed(
            id = trimmedUrl.hashCode().toUInt().toString(16),
            title = title.trim().ifBlank { trimmedUrl },
            url = trimmedUrl,
        )
        val current = list(context).filterNot { it.url == feed.url }
        write(context, current + feed)
        return Result.success(feed)
    }

    fun remove(context: Context, id: String) {
        removeAll(context, setOf(id))
    }

    fun removeAll(context: Context, ids: Set<String>): Int {
        if (ids.isEmpty()) return 0
        val current = list(context)
        val remaining = current.filterNot { it.id in ids }
        val removed = current.size - remaining.size
        if (removed > 0) write(context, remaining)
        return removed
    }

    fun setEnabled(context: Context, id: String, enabled: Boolean) {
        write(context, list(context).map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    private fun write(context: Context, feeds: List<SavedSubscriptionFeed>) {
        val file = file(context)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(feeds))
        _revision.value += 1
    }

    private fun file(context: Context): File {
        return File(context.filesDir, "plugin/subscription_feeds.json")
    }
}
