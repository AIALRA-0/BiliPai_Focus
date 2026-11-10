package com.android.purebilibili.feature.video.player

import java.util.IdentityHashMap

/** Page references outlive session replacement; only an unretained player can be released by it. */
internal class PlaybackPageOwnership {
    private val references = IdentityHashMap<Any, Int>()

    fun retain(player: Any) {
        references[player] = (references[player] ?: 0) + 1
    }

    fun release(player: Any) {
        val count = references[player] ?: return
        if (count == 1) {
            references.remove(player)
        } else {
            references[player] = count - 1
        }
    }

    fun isRetained(player: Any): Boolean = references.containsKey(player)
}

internal fun shouldReclaimVideoPlaybackSession(
    isVisible: Boolean,
    isSessionActive: Boolean,
    currentPlayer: Any?,
    currentBvid: String?,
    currentCid: Long,
    pagePlayer: Any,
    pageBvid: String,
    pageCid: Long,
    isLeavingByNavigation: Boolean = false,
): Boolean = isVisible && (
    isLeavingByNavigation ||
        !isSessionActive ||
        currentPlayer !== pagePlayer ||
        currentBvid != pageBvid ||
        currentCid != pageCid
    )
