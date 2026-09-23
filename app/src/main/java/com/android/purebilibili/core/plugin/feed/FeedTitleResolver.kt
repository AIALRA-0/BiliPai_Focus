package com.android.purebilibili.core.plugin.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
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

suspend fun resolveImportedSubscriptionTitles(
    subscriptions: List<ImportedSubscription>,
): List<ImportedSubscription> = supervisorScope {
    val gate = Semaphore(4)
    subscriptions.map { subscription ->
        async(Dispatchers.IO) {
            if (subscription.title.isNotBlank() && subscription.title != subscription.url) subscription
            else gate.withPermit {
                resolveSubscriptionTitle(subscription.url).getOrNull()
                    ?.let { subscription.copy(title = it) }
            }
        }
    }.awaitAll().filterNotNull()
}
