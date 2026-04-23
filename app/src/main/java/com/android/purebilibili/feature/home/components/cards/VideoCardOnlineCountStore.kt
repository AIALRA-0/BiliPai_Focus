package com.android.purebilibili.feature.home.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val VIDEO_CARD_ONLINE_COUNT_TTL_MS = 60_000L

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
    private val ttlMs: Long = VIDEO_CARD_ONLINE_COUNT_TTL_MS
) {
    private val states = ConcurrentHashMap<String, MutableStateFlow<String>>()
    private val cache = ConcurrentHashMap<String, VideoCardOnlineCountCacheEntry>()
    private val inFlight = ConcurrentHashMap<String, Unit>()

    fun observe(bvid: String, cid: Long): StateFlow<String> {
        val key = resolveVideoCardOnlineCountKey(bvid = bvid, cid = cid)
        return states.computeIfAbsent(key) {
            MutableStateFlow(cache[key]?.text.orEmpty())
        }.asStateFlow()
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
            } else {
                cache.remove(key)
            }
            state.value = text
        } catch (_: Exception) {
            state.value = ""
        } finally {
            inFlight.remove(key)
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
    showOnlineCount: Boolean
): String {
    val onlineCountFlow = remember(video.bvid, video.cid) {
        defaultVideoCardOnlineCountStore.observe(
            bvid = video.bvid,
            cid = video.cid
        )
    }
    val onlineCount by onlineCountFlow.collectAsState()

    LaunchedEffect(showOnlineCount, video.bvid, video.cid) {
        if (shouldLoadVideoCardOnlineCount(showOnlineCount, video.bvid, video.cid)) {
            defaultVideoCardOnlineCountStore.refreshIfNeeded(
                bvid = video.bvid,
                cid = video.cid
            )
        }
    }

    return remember(showOnlineCount, onlineCount) {
        resolveVideoCardOnlineCountText(
            showOnlineCount = showOnlineCount,
            onlineCount = onlineCount
        )
    }
}
