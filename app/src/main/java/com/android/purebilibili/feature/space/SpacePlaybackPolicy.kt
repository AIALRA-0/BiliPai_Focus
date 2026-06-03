package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.feature.list.VideoProgressDisplayState
import com.android.purebilibili.feature.list.resolveVideoDisplayProgressState
import com.android.purebilibili.feature.video.player.PlaylistItem

data class SpaceExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

enum class SpaceCollectionDetailType(val raw: String) {
    SEASON("season"),
    SERIES("series"),
    FAVORITE("favorite"),
    FAVORITE_SEASON("favorite_season");

    companion object {
        fun fromRaw(raw: String): SpaceCollectionDetailType? {
            return entries.firstOrNull { it.raw == raw.trim().lowercase() }
        }
    }
}

data class SpaceCollectionDetailRequest(
    val type: SpaceCollectionDetailType,
    val id: Long,
    val mid: Long,
    val title: String
)

data class SpacePriorityTabLoadState(
    val contribution: SpaceTabContentState,
    val dynamic: SpaceTabContentState,
    val collections: SpaceTabContentState
)

fun buildExternalPlaylistFromSpaceVideos(
    videos: List<SpaceVideoItem>,
    clickedBvid: String? = null
): SpaceExternalPlaylist? {
    if (videos.isEmpty()) return null

    val playlistItems = videos.map { video ->
        PlaylistItem(
            bvid = video.bvid,
            title = video.title,
            cover = video.pic,
            owner = video.author,
            duration = parseSpaceVideoLengthToSeconds(video.length)
        )
    }

    val startIndex = clickedBvid
        ?.takeIf { it.isNotBlank() }
        ?.let { bvid -> videos.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return SpaceExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}

fun resolveSpacePlayAllStartTarget(videos: List<SpaceVideoItem>): String? {
    return videos.firstOrNull()?.bvid
}

internal fun resolveSpaceVideoProgressState(
    video: SpaceVideoItem,
    localPositionMs: Long
): VideoProgressDisplayState {
    val durationSec = parseSpaceVideoLengthToSeconds(video.length).toInt()
    return resolveVideoDisplayProgressState(
        serverProgressSec = 0,
        durationSec = durationSec,
        localPositionMs = localPositionMs,
        viewAt = if (localPositionMs > 0L) 1L else 0L
    )
}

internal fun resolveSpaceResumePositionMs(localPositionMs: Long): Long {
    return localPositionMs.coerceAtLeast(0L)
}

fun resolveSpacePriorityTabLoadState(
    shell: SpaceTabShellState
): SpacePriorityTabLoadState {
    return SpacePriorityTabLoadState(
        contribution = shell.tabStates[SpaceMainTab.CONTRIBUTION] ?: SpaceTabContentState(),
        dynamic = shell.tabStates[SpaceMainTab.DYNAMIC] ?: SpaceTabContentState(),
        collections = shell.tabStates[SpaceMainTab.COLLECTIONS] ?: SpaceTabContentState()
    )
}

fun resolveSpaceCollectionDetailRequest(
    type: String,
    id: Long,
    mid: Long,
    title: String
): SpaceCollectionDetailRequest? {
    val detailType = SpaceCollectionDetailType.fromRaw(type) ?: return null
    if (id <= 0L) return null
    if (
        detailType != SpaceCollectionDetailType.FAVORITE &&
        detailType != SpaceCollectionDetailType.FAVORITE_SEASON &&
        mid <= 0L
    ) return null
    return SpaceCollectionDetailRequest(
        type = detailType,
        id = id,
        mid = mid,
        title = title.trim()
    )
}

internal fun parseSpaceVideoLengthToSeconds(length: String): Long {
    val normalized = length.trim()
    if (normalized.isEmpty()) return 0L
    val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
    if (parts.isEmpty()) return 0L

    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0L
    }
}
