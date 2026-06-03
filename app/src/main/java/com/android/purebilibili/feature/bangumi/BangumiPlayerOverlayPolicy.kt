package com.android.purebilibili.feature.bangumi

import androidx.media3.common.Player
import com.android.purebilibili.data.model.response.BangumiEpisode
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.feature.video.ui.overlay.PlaybackDebugInfo

internal enum class BangumiOverlayUnsupportedAction {
    LIKE,
    DISLIKE,
    COIN
}

internal fun resolveBangumiOverlayQualityLabel(
    currentQuality: Int,
    acceptQuality: List<Int>,
    acceptDescription: List<String>
): String {
    val currentIndex = acceptQuality.indexOf(currentQuality)
    return acceptDescription.getOrNull(currentIndex)
        ?.takeIf { it.isNotBlank() }
        ?: "自动"
}

internal fun resolveBangumiOverlaySwitchableQualityIds(
    acceptQuality: List<Int>
): List<Int> {
    return acceptQuality
        .filter { it > 0 }
        .distinct()
}

internal fun buildBangumiOverlayPages(
    episodes: List<BangumiEpisode>
): List<Page> {
    return episodes.mapIndexed { index, episode ->
        Page(
            cid = episode.cid,
            page = index + 1,
            from = "bangumi",
            part = buildBangumiOverlayEpisodeLabel(episode),
            duration = (episode.duration / 1000L).coerceAtLeast(0L)
        )
    }
}

internal fun resolveBangumiOverlayCurrentPageIndex(
    episodes: List<BangumiEpisode>,
    currentEpisodeId: Long
): Int {
    return episodes.indexOfFirst { it.id == currentEpisodeId }
        .takeIf { it >= 0 }
        ?: 0
}

internal fun resolveBangumiEpisodeForPageSelection(
    episodes: List<BangumiEpisode>,
    selectedPageIndex: Int
): BangumiEpisode? {
    return episodes.getOrNull(selectedPageIndex)
}

internal fun resolveBangumiUnsupportedOverlayActionMessage(
    action: BangumiOverlayUnsupportedAction
): String {
    return when (action) {
        BangumiOverlayUnsupportedAction.LIKE -> "番剧暂不支持点赞操作"
        BangumiOverlayUnsupportedAction.DISLIKE -> "番剧暂不支持点踩操作"
        BangumiOverlayUnsupportedAction.COIN -> "番剧暂不支持投币操作"
    }
}

internal fun resolveBangumiOverlayShareTitle(
    title: String,
    subtitle: String
): String {
    return listOf(title.trim(), subtitle.trim())
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
        .ifBlank { "番剧" }
}

internal fun shouldShowBangumiOverlayDislikeAction(): Boolean = false

internal data class BangumiPlaybackDebugSnapshot(
    val episodeId: Long = 0L,
    val cid: Long = 0L,
    val playbackState: Int = Player.STATE_IDLE,
    val playWhenReady: Boolean = false,
    val isPlaying: Boolean = false,
    val firstFrameRendered: Boolean = false,
    val lastVideoEvent: String = ""
) {
    fun resetForEpisode(
        episodeId: Long,
        cid: Long
    ): BangumiPlaybackDebugSnapshot {
        return BangumiPlaybackDebugSnapshot(
            episodeId = episodeId,
            cid = cid,
            lastVideoEvent = "waiting first frame"
        )
    }
}

internal fun resolveBangumiPlaybackDebugInfo(
    snapshot: BangumiPlaybackDebugSnapshot
): PlaybackDebugInfo {
    return PlaybackDebugInfo(
        playbackState = resolveBangumiPlaybackStateName(snapshot.playbackState),
        playWhenReady = snapshot.playWhenReady.toString(),
        isPlaying = snapshot.isPlaying.toString(),
        firstFrame = if (snapshot.firstFrameRendered) "rendered" else "",
        lastVideoEvent = buildBangumiPlaybackDebugEvent(snapshot)
    )
}

private fun resolveBangumiPlaybackStateName(playbackState: Int): String {
    return when (playbackState) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN"
    }
}

private fun buildBangumiPlaybackDebugEvent(
    snapshot: BangumiPlaybackDebugSnapshot
): String {
    val event = snapshot.lastVideoEvent.ifBlank {
        if (snapshot.firstFrameRendered) "first frame rendered" else "waiting first frame"
    }
    return "episode=${snapshot.episodeId} cid=${snapshot.cid} $event"
}

internal fun updateBangumiSuccessInteractionState(
    state: BangumiPlayerState.Success,
    isLiked: Boolean,
    coinCount: Int
): BangumiPlayerState.Success {
    return state.copy(
        isLiked = isLiked,
        coinCount = coinCount.coerceIn(0, 2)
    )
}

internal fun applyBangumiCoinResult(
    state: BangumiPlayerState.Success,
    coinDelta: Int,
    alsoLike: Boolean
): BangumiPlayerState.Success {
    return updateBangumiSuccessInteractionState(
        state = state,
        isLiked = state.isLiked || alsoLike,
        coinCount = state.coinCount + coinDelta
    )
}

private fun buildBangumiOverlayEpisodeLabel(
    episode: BangumiEpisode
): String {
    val primary = episode.title.trim()
    val secondary = episode.longTitle.trim()
    return listOf(primary, secondary)
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
        .ifBlank { "第${episode.id}集" }
}
