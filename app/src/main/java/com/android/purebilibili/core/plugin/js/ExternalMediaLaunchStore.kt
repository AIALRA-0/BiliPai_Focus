package com.android.purebilibili.core.plugin.js

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ExternalMediaLaunchRequest(
    val launchId: String,
    val title: String,
    val coverUrl: String? = null,
    val streams: List<BiliPaiJsMediaStream>,
    val selectedStreamIndex: Int = 0
)

object ExternalMediaLaunchStore {
    private val requests = ConcurrentHashMap<String, ExternalMediaLaunchRequest>()

    fun put(
        title: String,
        coverUrl: String?,
        streams: List<BiliPaiJsMediaStream>,
        selectedStreamIndex: Int = 0
    ): String {
        val launchId = UUID.randomUUID().toString()
        requests[launchId] = ExternalMediaLaunchRequest(
            launchId = launchId,
            title = title,
            coverUrl = coverUrl,
            streams = streams,
            selectedStreamIndex = selectedStreamIndex.coerceIn(0, streams.lastIndex.coerceAtLeast(0))
        )
        return launchId
    }

    fun get(launchId: String): ExternalMediaLaunchRequest? {
        return requests[launchId]
    }

    fun remove(launchId: String) {
        requests.remove(launchId)
    }
}
