package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.video.player.PlaylistItem

data class FavoriteExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

fun buildExternalPlaylistFromFavorite(
    items: List<VideoItem>,
    clickedBvid: String? = null
): FavoriteExternalPlaylist? {
    val playableItems = items.filter { !it.isCollectionResource && it.bvid.isNotBlank() }
    if (playableItems.isEmpty()) return null

    val playlistItems = playableItems.map { video ->
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
        ?.let { bvid -> playableItems.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return FavoriteExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}

internal fun shouldUseFavoriteExternalPlaylist(
    hasFavoriteViewModel: Boolean,
    isFavoriteDetail: Boolean
): Boolean {
    return hasFavoriteViewModel || isFavoriteDetail
}
