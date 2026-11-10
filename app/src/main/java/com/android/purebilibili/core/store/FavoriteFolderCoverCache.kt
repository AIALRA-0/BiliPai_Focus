package com.android.purebilibili.core.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 收藏夹预览封面缓存。
 *
 * 收藏夹列表接口（fav/folder/created/list-all）对部分收藏夹不返回 cover，需要额外请求
 * fav/resource/list 取首条资源封面补齐。该请求逐个收藏夹发起，数量随收藏夹数增长，
 * 且 fav/resource/list 对突发并发较敏感（易触发 412/429）。
 *
 * 这里缓存 media_id -> 封面 URL，并以 media_count 作为失效依据：
 * - media_count 未变化 → 认为首条（最新收藏）资源未变，直接复用缓存；
 * - media_count 变化 → 有新内容进入，重新请求；
 * - 超过 TTL → 兜底重取，覆盖“删除一条又新增一条导致数量不变”的极端情况。
 *
 * 缓存的是 URL 而非图片本身；图片字节仍由 Coil 磁盘缓存负责，这里只省掉
 * “每条收藏夹问一次首条资源”的发现请求。
 */
object FavoriteFolderCoverCache {

    private const val PREFS_NAME = "favorite_folder_cover_cache"
    private const val KEY_PAYLOAD = "favorite_folder_cover_payload_v1"
    private const val MAX_ENTRIES = 500

    /** 兜底有效期：media_count 未变化但超过该时长时仍重取一次。 */
    const val DEFAULT_TTL_MS = 24L * 60L * 60L * 1000L

    data class CachedCover(
        val cover: String,
        val mediaCount: Int,
        val updatedAtMs: Long,
    )

    data class Lookup(
        val mediaId: Long,
        val mediaCount: Int,
    )

    data class Resolved(
        val mediaId: Long,
        val mediaCount: Int,
        val cover: String,
    )

    @Serializable
    private data class CoverEntry(
        val cover: String = "",
        val mediaCount: Int = 0,
        val updatedAtMs: Long = 0L,
    )

    @Serializable
    private data class CoverPayload(
        val entries: Map<String, CoverEntry> = emptyMap(),
    )

    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 进程内镜像，避免每次补封面都读盘。 */
    private var memoryEntries: MutableMap<Long, CachedCover>? = null

    /**
     * 批量取命中缓存且未过期的封面，返回 mediaId -> cover。
     * 未命中、media_count 不匹配或已过期的项不会出现在结果中。
     */
    suspend fun lookupAll(
        context: Context,
        lookups: List<Lookup>,
        nowMs: Long = System.currentTimeMillis(),
        ttlMs: Long = DEFAULT_TTL_MS,
    ): Map<Long, String> = withContext(Dispatchers.IO) {
        if (lookups.isEmpty()) return@withContext emptyMap()
        val entries = snapshot(context)
        val result = HashMap<Long, String>(lookups.size)
        lookups.forEach { lookup ->
            val entry = entries[lookup.mediaId] ?: return@forEach
            if (!isFresh(entry, lookup.mediaCount, nowMs, ttlMs)) return@forEach
            result[lookup.mediaId] = entry.cover
        }
        result
    }

    /** 写入一批已解析出的封面；空封面会被忽略。 */
    suspend fun store(
        context: Context,
        resolved: List<Resolved>,
        nowMs: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val accepted = resolved.filter { it.mediaId > 0L && it.cover.isNotBlank() }
        if (accepted.isEmpty()) return@withContext
        synchronized(lock) {
            val entries = snapshot(context).toMutableMap()
            accepted.forEach { item ->
                entries[item.mediaId] = CachedCover(
                    cover = item.cover.trim(),
                    mediaCount = item.mediaCount,
                    updatedAtMs = nowMs,
                )
            }
            val trimmed = trimToBudget(entries)
            memoryEntries = trimmed.toMutableMap()
            persist(context, trimmed)
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            memoryEntries = null
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PAYLOAD)
                .apply()
        }
    }

    private fun isFresh(
        entry: CachedCover,
        mediaCount: Int,
        nowMs: Long,
        ttlMs: Long,
    ): Boolean {
        if (entry.cover.isBlank()) return false
        if (entry.mediaCount != mediaCount) return false
        if (ttlMs > 0L && nowMs - entry.updatedAtMs > ttlMs) return false
        return true
    }

    private fun trimToBudget(entries: Map<Long, CachedCover>): Map<Long, CachedCover> {
        if (entries.size <= MAX_ENTRIES) return entries
        return entries.entries
            .sortedByDescending { it.value.updatedAtMs }
            .take(MAX_ENTRIES)
            .associate { it.key to it.value }
    }

    private fun snapshot(context: Context): Map<Long, CachedCover> = synchronized(lock) {
        memoryEntries ?: readFromDisk(context).toMutableMap().also { memoryEntries = it }
    }

    private fun readFromDisk(context: Context): Map<Long, CachedCover> {
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PAYLOAD, null)
            .orEmpty()
        if (raw.isBlank()) return emptyMap()
        val payload = runCatching {
            json.decodeFromString<CoverPayload>(raw)
        }.getOrNull() ?: return emptyMap()
        val result = HashMap<Long, CachedCover>(payload.entries.size)
        payload.entries.forEach { (key, value) ->
            val mediaId = key.toLongOrNull() ?: return@forEach
            if (value.cover.isBlank()) return@forEach
            result[mediaId] = CachedCover(
                cover = value.cover,
                mediaCount = value.mediaCount,
                updatedAtMs = value.updatedAtMs,
            )
        }
        return result
    }

    private fun persist(context: Context, entries: Map<Long, CachedCover>) {
        val payload = CoverPayload(
            entries = entries.entries.associate { (mediaId, value) ->
                mediaId.toString() to CoverEntry(
                    cover = value.cover,
                    mediaCount = value.mediaCount,
                    updatedAtMs = value.updatedAtMs,
                )
            },
        )
        val raw = json.encodeToString(payload)
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PAYLOAD, raw)
            .apply()
    }
}
