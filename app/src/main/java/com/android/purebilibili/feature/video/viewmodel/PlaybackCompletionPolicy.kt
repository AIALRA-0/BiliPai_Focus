package com.android.purebilibili.feature.video.viewmodel

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlayMode

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

internal fun resolvePlaybackEndAction(
    behavior: PlaybackCompletionBehavior,
    autoPlayEnabled: Boolean,
    isExternalPlaylist: Boolean,
    externalPlaylistAutoContinueEnabled: Boolean,
    hasNextPageOrSeasonTarget: Boolean = false
): PlaybackEndAction {
    return when (behavior) {
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT -> PlaybackEndAction.STOP
        PlaybackCompletionBehavior.PLAY_IN_ORDER -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST
        PlaybackCompletionBehavior.REPEAT_ONE -> PlaybackEndAction.REPEAT_CURRENT
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC -> {
            // “自动”只延续当前播放上下文：分P/合集受“自动播放下一个”控制，
            // 外部列表由自己的连续播放开关控制，普通单视频停住，避免误跳推荐视频。
            val shouldAutoContinue = if (isExternalPlaylist) {
                externalPlaylistAutoContinueEnabled
            } else {
                autoPlayEnabled && hasNextPageOrSeasonTarget
            }
            if (shouldAutoContinue) {
                PlaybackEndAction.AUTO_CONTINUE
            } else {
                PlaybackEndAction.STOP
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun resolvePlaybackEndActionForSession(
    behavior: PlaybackCompletionBehavior,
    autoPlayEnabled: Boolean,
    isExternalPlaylist: Boolean,
    externalPlaylistAutoContinueEnabled: Boolean,
    externalPlaylistSource: ExternalPlaylistSource,
    playMode: PlayMode,
    hasNextPageOrSeasonTarget: Boolean = false
): PlaybackEndAction {
    // 显式播放完成策略（暂停/顺序/单循/列表循）始终优先于来源特判，
    // 避免外部队列（如收藏夹听歌）出现“设置已选暂停但仍自动循环”的行为割裂。
    // NOTE: externalPlaylistSource/playMode 保留入参用于兼容现有调用面与后续扩展。
    return resolvePlaybackEndAction(
        behavior = behavior,
        autoPlayEnabled = autoPlayEnabled,
        isExternalPlaylist = isExternalPlaylist,
        externalPlaylistAutoContinueEnabled = externalPlaylistAutoContinueEnabled,
        hasNextPageOrSeasonTarget = hasNextPageOrSeasonTarget
    )
}

internal fun resolvePlaybackCompletionRepeatMode(
    behavior: PlaybackCompletionBehavior
): Int {
    return when (behavior) {
        PlaybackCompletionBehavior.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
        PlaybackCompletionBehavior.PLAY_IN_ORDER,
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> Player.REPEAT_MODE_OFF
    }
}
