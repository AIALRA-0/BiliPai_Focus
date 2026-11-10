package com.android.purebilibili.core.plugin.feed

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.net.URI

internal fun chooseSubscriptionTitle(manualTitle: String, parsedTitle: String?, url: String): String {
    val address = url.trim()
    manualTitle.trim().takeIf { it.isNotEmpty() && it != address }?.let { return it }
    parsedTitle?.trim()?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotEmpty() && it != address }?.let { return it }
    return runCatching {
        val uri = URI(address)
        val host = uri.host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: return@runCatching address
        val path = uri.path.orEmpty().trim('/')
        if (path.isBlank() || path.lowercase() in setOf("feed", "rss", "atom.xml", "index.xml", "feed.xml")) {
            host
        } else {
            "$host/$path"
        }
    }.getOrDefault(address)
}

suspend fun resolveSubscriptionTitle(url: String, manualTitle: String = ""): Result<String> {
    val address = url.trim()
    if (manualTitle.isNotBlank() && manualTitle.trim() != address) return Result.success(manualTitle.trim())
    return fetchFeedXml(address).fold(
        onSuccess = { xml ->
            runCatching {
                val feed = parseFeedDocument(xml, sourceId = address, sourceTitle = "", sourceUrl = address)
                chooseSubscriptionTitle(manualTitle, feed.title, address)
            }.recoverCatching { throw IllegalArgumentException("这不是可识别的 RSS 或 Atom 地址") }
        },
        onFailure = { Result.success(chooseSubscriptionTitle(manualTitle, null, address)) },
    )
}

/**
 * 占位标题：导入时没有真实标题的条目以 url 作为标题，等待后台补全。
 */
fun needsTitleResolution(feed: SavedSubscriptionFeed): Boolean =
    feed.title.isBlank() || feed.title == feed.url

private val titleRefreshMutex = Mutex()

/**
 * 批量导入不再逐源抓标题（几百个源会卡住导入几分钟）。导入即时完成，
 * 之后再调用本函数后台补全占位标题：抓取成功才覆盖，失败的条目保留
 * 占位标题，下次触发（再次导入或进入订阅设置页）自动重试。
 */
suspend fun refreshMissingSubscriptionTitles(context: Context) {
    titleRefreshMutex.withLock {
        val pending = SubscriptionFeedStore.list(context).filter(::needsTitleResolution)
        if (pending.isEmpty()) return
        val gate = Semaphore(3)
        coroutineScope {
            pending.map { feed ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        val parsedTitle = fetchFeedXml(feed.url).mapCatching { xml ->
                            parseFeedDocument(xml, sourceId = feed.id, sourceTitle = "", sourceUrl = feed.url).title
                        }.getOrNull()
                        val title = parsedTitle?.let { chooseSubscriptionTitle("", it, feed.url) }
                        if (!title.isNullOrBlank() && title != feed.url) {
                            SubscriptionFeedStore.updateTitle(context, feed.id, title)
                        }
                    }
                }
            }.awaitAll()
        }
    }
}
