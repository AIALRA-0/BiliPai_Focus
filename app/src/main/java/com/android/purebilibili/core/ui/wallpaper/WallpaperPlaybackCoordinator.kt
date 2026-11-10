package com.android.purebilibili.core.ui.wallpaper

import androidx.compose.runtime.staticCompositionLocalOf

/** Main-thread leases for decorative motion. Stop the previous owner before starting another. */
internal class WallpaperPlaybackCoordinator {
    private data class Request(val priority: Int, val order: Long, val setPlaying: (Boolean) -> Unit)
    private val requests = linkedMapOf<Any, Request>()
    private var order = 0L
    private var owner: Any? = null

    fun acquire(token: Any, priority: Int, setPlaying: (Boolean) -> Unit) {
        release(token)
        setPlaying(false)
        requests[token] = Request(priority, ++order, setPlaying)
        selectOwner()
    }

    fun release(token: Any) {
        if (owner === token) {
            requests[token]?.setPlaying?.invoke(false)
            owner = null
        }
        requests.remove(token)
        selectOwner()
    }

    private fun selectOwner() {
        val next = requests.entries.maxWithOrNull(
            compareBy<Map.Entry<Any, Request>> { it.value.priority }.thenBy { it.value.order }
        )?.key
        if (next === owner) return
        owner?.let { requests[it]?.setPlaying?.invoke(false) }
        owner = next
        next?.let { requests[it]?.setPlaying?.invoke(true) }
    }
}

internal val LocalWallpaperPlaybackCoordinator = staticCompositionLocalOf<WallpaperPlaybackCoordinator?> { null }
