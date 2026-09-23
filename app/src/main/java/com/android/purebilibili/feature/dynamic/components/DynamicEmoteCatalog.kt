package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.EmotePackage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cached emote shortcode → image URL map for dynamic rich text.
 *
 * Loads `business=dynamic` first (see bilibili-API-collect emoji docs), then merges
 * `business=reply` so common yellow-face shortcodes still resolve when a feed only
 * returns plain text with `[表情]` tokens.
 */
internal object DynamicEmoteCatalog {
    private val mutex = Mutex()

    @Volatile
    private var cached: Map<String, String> = emptyMap()

    @Volatile
    private var loadedSessionKey: DynamicEmoteCatalogSessionKey? = null

    @Volatile
    private var fullyLoaded = false

    @Volatile
    private var lastAttemptMs = 0L

    fun snapshot(): Map<String, String> = cached.takeIf {
        loadedSessionKey == currentSessionKey()
    }.orEmpty()

    fun currentSessionKey(): DynamicEmoteCatalogSessionKey = DynamicEmoteCatalogSessionKey(
        mid = TokenManager.midCache ?: 0L,
        authenticated = !TokenManager.sessDataCache.isNullOrBlank(),
    )

    suspend fun ensureLoaded(): Map<String, String> {
        val requestedSessionKey = currentSessionKey()
        val nowMs = System.currentTimeMillis()
        if (!shouldReloadDynamicEmoteCatalog(
                requestedSessionKey = requestedSessionKey,
                loadedSessionKey = loadedSessionKey,
                fullyLoaded = fullyLoaded,
                lastAttemptMs = lastAttemptMs,
                nowMs = nowMs,
            )
        ) {
            return cached
        }
        return mutex.withLock {
            val lockedNowMs = System.currentTimeMillis()
            if (!shouldReloadDynamicEmoteCatalog(
                    requestedSessionKey = requestedSessionKey,
                    loadedSessionKey = loadedSessionKey,
                    fullyLoaded = fullyLoaded,
                    lastAttemptMs = lastAttemptMs,
                    nowMs = lockedNowMs,
                )
            ) {
                return@withLock cached
            }
            if (loadedSessionKey != requestedSessionKey) {
                cached = emptyMap()
                fullyLoaded = false
            }
            val loadResult = loadEmoteUrlMap()
            cached = loadResult.entries
            fullyLoaded = loadResult.complete
            loadedSessionKey = requestedSessionKey
            lastAttemptMs = lockedNowMs
            cached
        }
    }

    private suspend fun loadEmoteUrlMap(): DynamicEmoteCatalogLoadResult {
        val result = linkedMapOf<String, String>()
        // Seed a few common shortcodes so first paint is not empty if the panel request fails.
        result["[doge]"] =
            "https://i0.hdslb.com/bfs/emote/6f8743c3c13009f4705307b2750e32f5068225e3.png"
        var completedBusinessCount = 0
        listOf("dynamic", "reply").forEach { business ->
            runCatching {
                fetchPackages(business).forEach { pkg ->
                    pkg.emote.orEmpty().forEach { emote ->
                        val text = emote.text.trim()
                        val url = emote.url.trim()
                        if (text.isNotEmpty() && url.isNotEmpty()) {
                            result.putIfAbsent(text, normalizeEmoteUrl(url))
                        }
                    }
                }
            }.onSuccess { completedBusinessCount += 1 }
        }
        return DynamicEmoteCatalogLoadResult(
            entries = result,
            complete = completedBusinessCount == DYNAMIC_EMOTE_BUSINESS_COUNT,
        )
    }

    private suspend fun fetchPackages(business: String): List<EmotePackage> {
        val response = NetworkModule.api.getEmotes(mapOf("business" to business))
        if (response.code != 0) {
            error(response.message?.takeIf(String::isNotBlank) ?: "表情目录加载失败")
        }
        return response.data?.packages ?: response.data?.all_packages.orEmpty()
    }

    private fun normalizeEmoteUrl(rawUrl: String): String {
        return when {
            rawUrl.startsWith("//") -> "https:$rawUrl"
            rawUrl.startsWith("http://") -> rawUrl.replaceFirst("http://", "https://")
            else -> rawUrl
        }
    }
}

internal data class DynamicEmoteCatalogSessionKey(
    val mid: Long,
    val authenticated: Boolean,
)

private data class DynamicEmoteCatalogLoadResult(
    val entries: Map<String, String>,
    val complete: Boolean,
)

private const val DYNAMIC_EMOTE_BUSINESS_COUNT = 2
internal const val DYNAMIC_EMOTE_RETRY_INTERVAL_MS = 30_000L

internal fun shouldReloadDynamicEmoteCatalog(
    requestedSessionKey: DynamicEmoteCatalogSessionKey,
    loadedSessionKey: DynamicEmoteCatalogSessionKey?,
    fullyLoaded: Boolean,
    lastAttemptMs: Long,
    nowMs: Long,
): Boolean {
    if (loadedSessionKey != requestedSessionKey) return true
    if (fullyLoaded) return false
    return nowMs - lastAttemptMs >= DYNAMIC_EMOTE_RETRY_INTERVAL_MS
}
