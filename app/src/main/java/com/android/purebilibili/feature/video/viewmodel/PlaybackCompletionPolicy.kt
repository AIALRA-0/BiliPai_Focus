package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlaybackCompletionBehavior

internal enum class PlaybackEndAction {
    STOP,
    PLAY_NEXT_IN_PLAYLIST,
    PLAY_NEXT_IN_PLAYLIST_LOOP,
    REPEAT_CURRENT,
    AUTO_CONTINUE
}

internal fun shouldSuppressPlaybackCompletionForCommentInteraction(
    commentInteractionActive: Boolean
): Boolean = commentInteractionActive

internal fun shouldStopAfterCurrentPlayback(
    armedIdentity: Pair<String, Long>?,
    currentBvid: String,
    currentCid: Long,
): Boolean = armedIdentity != null && armedIdentity == (currentBvid to currentCid)

internal fun resolvePlaybackEndAction(
    behavior: PlaybackCompletionBehavior,
    autoPlayEnabled: Boolean,
    isExternalPlaylist: Boolean,
    externalPlaylistAutoContinueEnabled: Boolean,
    hasNextPage: Boolean,
    hasNextSeasonEpisode: Boolean
): PlaybackEndAction {
    return when (behavior) {
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT -> PlaybackEndAction.STOP
        PlaybackCompletionBehavior.PLAY_IN_ORDER -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST
        PlaybackCompletionBehavior.REPEAT_ONE -> PlaybackEndAction.REPEAT_CURRENT
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC -> {
            // 分P先受“自动播放下一个”控制；只有视频边界才读取外部列表开关。
            val shouldAutoContinue = when {
                hasNextPage -> autoPlayEnabled
                isExternalPlaylist -> externalPlaylistAutoContinueEnabled
                else -> autoPlayEnabled && hasNextSeasonEpisode
            }
            if (shouldAutoContinue) {
                PlaybackEndAction.AUTO_CONTINUE
            } else {
                PlaybackEndAction.STOP
            }
        }
    }
}


internal fun resolvePlaybackCompletionRepeatMode(
    behavior: PlaybackCompletionBehavior,
    isInAudioMode: Boolean = false,
): Int {
    if (isInAudioMode) return Player.REPEAT_MODE_OFF
    return when (behavior) {
        PlaybackCompletionBehavior.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
        PlaybackCompletionBehavior.PLAY_IN_ORDER,
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> Player.REPEAT_MODE_OFF
    }
}
