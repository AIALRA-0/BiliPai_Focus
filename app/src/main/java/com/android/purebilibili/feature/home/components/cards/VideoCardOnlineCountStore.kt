package com.android.purebilibili.feature.home.components.cards

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.ui.components.LocalPageImageLoadingAllowed
import com.android.purebilibili.data.model.response.VideoItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CancellationException

private const val VIDEO_CARD_ONLINE_COUNT_TTL_MS = 60_000L

// 有界化上限：正常一屏可见卡片不到 20 张，512 条足够覆盖一次长会话的所有滚动历史。
internal const val VIDEO_CARD_ONLINE_COUNT_MAX_ENTRIES = 512

internal fun shouldLoadVideoCardOnlineCount(
    showOnlineCount: Boolean,
    bvid: String,
    cid: Long
): Boolean {
    return showOnlineCount && bvid.isNotBlank() && cid > 0L
}

internal fun resolveVideoCardOnlineCountText(
    showOnlineCount: Boolean,
    onlineCount: String
): String {
    return if (showOnlineCount) onlineCount.trim() else ""
}

internal fun resolveVideoCardOnlineCountKey(bvid: String, cid: Long): String {
    return "${bvid.trim()}#$cid"
}

private data class VideoCardOnlineCountCacheEntry(
    val text: String,
    val fetchedAtMs: Long
)

internal class VideoCardOnlineCountStore(
    private val fetchOnlineCount: suspend (String, Long) -> String,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = VIDEO_CARD_ONLINE_COUNT_TTL_MS,
    private val maxEntries: Int = VIDEO_CARD_ONLINE_COUNT_MAX_ENTRIES
) {
    private val states = ConcurrentHashMap<String, MutableStateFlow<String>>()
    private val cache = ConcurrentHashMap<String, VideoCardOnlineCountCacheEntry>()
    private val inFlight = ConcurrentHashMap<String, Unit>()
    // A composed card keeps its flow even when scroll/transition budgets pause collection.
    private val owners = ConcurrentHashMap<String, Int>()
    internal val stateEntryCount: Int get() = states.size

    fun retain(bvid: String, cid: Long) {
        owners.merge(resolveVideoCardOnlineCountKey(bvid, cid), 1) { count, increment -> count + increment }
    }

    fun release(bvid: String, cid: Long) {
        owners.computeIfPresent(resolveVideoCardOnlineCountKey(bvid, cid)) { _, count ->
            (count - 1).takeIf { it > 0 }
        }
        sweepCachesIfOverCap()
    }

    fun observe(bvid: String, cid: Long): StateFlow<String> {
        val key = resolveVideoCardOnlineCountKey(bvid = bvid, cid = cid)
        val state = states.computeIfAbsent(key) {
            MutableStateFlow(cache[key]?.text.orEmpty())
        }
        // Observation alone can allocate thousands of entries without a successful request.
        sweepCachesIfOverCap(protectedKey = key)
        return state.asStateFlow()
    }

    suspend fun refreshIfNeeded(bvid: String, cid: Long) {
        if (!shouldLoadVideoCardOnlineCount(showOnlineCount = true, bvid = bvid, cid = cid)) {
            return
        }

        val normalizedBvid = bvid.trim()
        val key = resolveVideoCardOnlineCountKey(bvid = normalizedBvid, cid = cid)
        val state = states.computeIfAbsent(key) {
            MutableStateFlow(cache[key]?.text.orEmpty())
        }
        val currentTimeMs = nowMs()
        val cached = cache[key]
        if (cached != null && currentTimeMs - cached.fetchedAtMs < ttlMs) {
            if (state.value != cached.text) {
                state.value = cached.text
            }
            return
        }
        if (inFlight.putIfAbsent(key, Unit) != null) {
            return
        }

        try {
            val text = fetchOnlineCount(normalizedBvid, cid).trim()
            if (text.isNotEmpty()) {
                cache[key] = VideoCardOnlineCountCacheEntry(
                    text = text,
                    fetchedAtMs = nowMs()
                )
                sweepCachesIfOverCap()
            } else {
                cache.remove(key)
            }
            state.value = text
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state.value = ""
        } finally {
            inFlight.remove(key)
            sweepCachesIfOverCap(protectedKey = key)
        }
    }

    /**
     * 长时间刷首页会为每张卡片累积一个 StateFlow + 缓存条目且永不释放。
     * 超过容量上限后：过期条目直接清 cache，未被订阅（卡片已不可见）的 StateFlow 一并清；
     * 仍超限再按最旧 fetchedAtMs 淘汰。仍被订阅的 StateFlow 保留，避免冻结可见卡片。
     */
    @Synchronized
    private fun sweepCachesIfOverCap(protectedKey: String? = null) {
        if (cache.size > maxEntries) {
            val now = nowMs()
            val staleIterator = cache.entries.iterator()
            while (staleIterator.hasNext()) {
                val entry = staleIterator.next()
                if (now - entry.value.fetchedAtMs >= ttlMs) {
                    staleIterator.remove()
                    removeUnsubscribedState(entry.key, protectedKey)
                }
            }
        }
        if (cache.size > maxEntries) {
            cache.entries.asSequence()
                .sortedBy { it.value.fetchedAtMs }
                .take(cache.size - maxEntries)
                .toList()
                .forEach { entry ->
                    cache.remove(entry.key)
                    removeUnsubscribedState(entry.key, protectedKey)
                }
        }
        if (states.size > maxEntries) {
            for (key in states.keys) {
                if (states.size <= maxEntries) break
                removeUnsubscribedState(key, protectedKey)
            }
        }
    }

    private fun removeUnsubscribedState(key: String, protectedKey: String? = null) {
        if (key == protectedKey || owners.containsKey(key) || inFlight.containsKey(key)) return
        val flow = states[key] ?: return
        if (flow.subscriptionCount.value == 0) {
            states.remove(key, flow)
        }
    }
}

private val defaultVideoCardOnlineCountStore by lazy {
    VideoCardOnlineCountStore(
        fetchOnlineCount = { bvid, cid ->
            val response = NetworkModule.api.getOnlineCount(bvid = bvid, cid = cid)
            if (response.code == 0) {
                response.data?.total.orEmpty()
            } else {
                ""
            }
        }
    )
}

@Composable
internal fun rememberVideoCardOnlineCount(
    video: VideoItem,
    showOnlineCount: Boolean,
    isVisibleInWindow: Boolean = true,
): String {
    if (!shouldLoadVideoCardOnlineCount(showOnlineCount, video.bvid, video.cid)) {
        return ""
    }
    return key(video.bvid, video.cid) {
        rememberActiveVideoCardOnlineCount(bvid = video.bvid, cid = video.cid, isVisibleInWindow = isVisibleInWindow)
    }
}

@Composable
private fun rememberActiveVideoCardOnlineCount(
    bvid: String,
    cid: Long,
    isVisibleInWindow: Boolean,
): String {
    val onlineCountFlow = remember(bvid, cid) {
        defaultVideoCardOnlineCountStore.observe(
            bvid = bvid,
            cid = cid
        )
    }
    DisposableEffect(bvid, cid, onlineCountFlow) {
        defaultVideoCardOnlineCountStore.retain(bvid, cid)
        onDispose { defaultVideoCardOnlineCountStore.release(bvid, cid) }
    }
    val workAllowed = isVisibleInWindow && LocalPageImageLoadingAllowed.current && LocalHomeCardAuxiliaryWorkAllowed.current
    // Keep the last displayed text while home is covered or morphing. Dropping only the
    // subscription avoids applying another visible card's shared request result mid-flight.
    val updates = remember(onlineCountFlow, workAllowed) {
        if (workAllowed) onlineCountFlow else emptyFlow()
    }
    val onlineCount by updates.collectAsStateWithLifecycle(initialValue = onlineCountFlow.value)

    LaunchedEffect(bvid, cid, workAllowed) {
        if (!workAllowed) return@LaunchedEffect
        defaultVideoCardOnlineCountStore.refreshIfNeeded(
            bvid = bvid,
            cid = cid
        )
    }

    return resolveVideoCardOnlineCountText(
        showOnlineCount = true,
        onlineCount = onlineCount
    )
}
