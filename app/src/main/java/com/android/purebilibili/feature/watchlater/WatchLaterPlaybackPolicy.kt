package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.video.player.PlaylistItem

internal enum class WatchLaterManagementAction {
    CLEAR_VIEWED,
    CLEAR_ALL
}

data class WatchLaterExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

internal data class WatchLaterPlaybackTarget(
    val bvid: String,
    val cid: Long,
    val resumePositionMs: Long
)

fun buildExternalPlaylistFromWatchLater(
    items: List<VideoItem>,
    clickedBvid: String? = null
): WatchLaterExternalPlaylist? {
    if (items.isEmpty()) return null

    val playlistItems = items.map { video ->
        PlaylistItem(
            bvid = video.bvid,
            title = video.title,
            cover = video.pic,
            owner = video.owner.name,
            duration = video.duration.toLong()
        )
    }

    val startIndex = clickedBvid
        ?.takeIf { it.isNotBlank() }
        ?.let { bvid -> items.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return WatchLaterExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}

internal fun resolveWatchLaterPlaybackTarget(
    items: List<VideoItem>,
    clickedBvid: String
): WatchLaterPlaybackTarget? {
    if (clickedBvid.isBlank()) return null
    val item = items.firstOrNull { it.bvid == clickedBvid } ?: return null
    return WatchLaterPlaybackTarget(
        bvid = item.bvid,
        cid = item.cid.coerceAtLeast(0L),
        resumePositionMs = item.progress
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
            ?: 0L
    )
}

internal fun isWatchLaterViewed(item: VideoItem): Boolean {
    return item.duration > 0 && item.progress >= item.duration
}

internal fun resolveWatchLaterItemsAfterManagementAction(
    items: List<VideoItem>,
    action: WatchLaterManagementAction
): List<VideoItem> {
    return when (action) {
        WatchLaterManagementAction.CLEAR_VIEWED -> items.filterNot(::isWatchLaterViewed)
        WatchLaterManagementAction.CLEAR_ALL -> emptyList()
    }
}

internal fun resolveWatchLaterManagementConfirmText(
    action: WatchLaterManagementAction,
    affectedCount: Int
): String {
    return when (action) {
        WatchLaterManagementAction.CLEAR_VIEWED ->
            "确认清空已看完的视频吗？本地预计影响 $affectedCount 个，实际以服务端记录为准。"
        WatchLaterManagementAction.CLEAR_ALL ->
            "确认清空全部稍后再看吗？本操作会移除当前账号的全部稍后再看视频。"
    }
}

internal fun resolveWatchLaterManagementSuccessMessage(
    action: WatchLaterManagementAction,
    affectedCount: Int
): String {
    return when (action) {
        WatchLaterManagementAction.CLEAR_VIEWED ->
            if (affectedCount > 0) "已清理 $affectedCount 个已看视频" else "已请求清理已看视频"
        WatchLaterManagementAction.CLEAR_ALL -> "已清空稍后再看"
    }
}
