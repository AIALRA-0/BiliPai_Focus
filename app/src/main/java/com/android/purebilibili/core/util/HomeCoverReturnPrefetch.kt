package com.android.purebilibili.core.util

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest

/**
 * 首页封面「返回预热」注册表。
 *
 * 详情页停留期间首页 scene 被 Nav3 卸掉，Coil 可能因内存压力逐出首页封面缓存；
 * 返回落位瞬间首页 live 恢复，封面若需重新解码（网络/磁盘）会出现
 * 「冻结帧有图 → live 占位 → 图加载完成」的闪变窗口。
 *
 * 方案：首页 [VideoCard] 每次组合（即可见）时上报 (bvid, url, cacheKey)，
 * 返回时用与 AsyncImage 完全相同的 url + memoryCacheKey/diskCacheKey 做 prefetch，
 * 落位时命中缓存零等待。
 */
internal data class HomeCoverReturnPrefetchEntry(
    val bvid: String,
    val url: String,
    val cacheKey: String,
)

internal object HomeCoverReturnPrefetchRegistry {

    private const val MAX_REGISTERED_ENTRIES = 64

    // 插入序 = 最近可见序（LRU 语义）。dispose 不清除：详情打开后首页 scene
    // 被卸掉，返回时注册表必须仍保有「打开详情前可视」的快照。
    private val entries = LinkedHashMap<String, HomeCoverReturnPrefetchEntry>()

    /** 首页卡片可见时上报。幂等：同 cacheKey 重复上报只刷新顺序，不产生重复项。 */
    fun onCardVisible(entry: HomeCoverReturnPrefetchEntry) {
        if (entry.url.isBlank() || entry.cacheKey.isBlank()) return
        // 最新可见的排最前：返回预热候选按注册顺序截断时优先保留最近看到的卡片。
        val rebuilt = LinkedHashMap<String, HomeCoverReturnPrefetchEntry>(entries.size + 1)
        rebuilt[entry.cacheKey] = entry
        for ((key, value) in entries) {
            if (key != entry.cacheKey) rebuilt[key] = value
        }
        entries.clear()
        entries.putAll(rebuilt)
        while (entries.size > MAX_REGISTERED_ENTRIES) {
            entries.remove(entries.keys.last())
        }
    }

    fun snapshot(): List<HomeCoverReturnPrefetchEntry> = entries.values.toList()

    internal fun clearForTest() {
        entries.clear()
    }
}

/**
 * 返回预热候选决策：源卡封面优先，其余按最近可见序，去重并按 [maxCount] 截断。
 * 纯函数，便于单测锁定。
 */
internal fun resolveHomeCoverReturnPrefetchCandidates(
    visibleEntries: List<HomeCoverReturnPrefetchEntry>,
    sourceBvid: String?,
    maxCount: Int = 24,
): List<HomeCoverReturnPrefetchEntry> {
    if (maxCount <= 0) return emptyList()
    val normalizedSourceBvid = sourceBvid?.trim()?.takeIf { it.isNotEmpty() }
    val result = LinkedHashMap<String, HomeCoverReturnPrefetchEntry>()
    val sourceEntry = visibleEntries.firstOrNull { it.bvid == normalizedSourceBvid }
    if (sourceEntry != null) {
        result[sourceEntry.cacheKey] = sourceEntry
    }
    for (entry in visibleEntries) {
        if (entry.url.isBlank() || entry.cacheKey.isBlank()) continue
        if (result.size >= maxCount) break
        if (result.containsKey(entry.cacheKey)) continue
        result[entry.cacheKey] = entry
    }
    return result.values.toList()
}

/**
 * 执行封面预热：与首页 [coil3.compose.AsyncImage] 完全一致的 data + memoryCacheKey +
 * diskCacheKey，保证命中同一缓存条目。Coil 对相同 key 的并发请求会合并，
 * 重复触发幂等；内存/磁盘命中时不会产生网络请求。
 */
internal fun prefetchHomeCoverImages(
    context: Context,
    entries: List<HomeCoverReturnPrefetchEntry>,
) {
    if (entries.isEmpty()) return
    val imageLoader = SingletonImageLoader.get(context)
    entries.forEach { entry ->
        imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(entry.url)
                .memoryCacheKey(entry.cacheKey)
                .diskCacheKey(entry.cacheKey)
                .build()
        )
    }
}
