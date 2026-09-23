package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryData
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.feature.list.VideoProgressDisplayState
import com.android.purebilibili.feature.list.resolveVideoDisplayProgressState
import com.android.purebilibili.feature.video.player.PlaylistItem

data class SpaceExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

data class SpaceWatchProgress(
    val bvid: String,
    val cid: Long,
    val title: String,
    val progressSec: Int,
    val durationSec: Int,
    val viewAt: Long
)

data class SpacePlaybackTarget(
    val cid: Long,
    val resumePositionMs: Long
)

internal sealed interface SpaceLocateTargetPageAction {
    data class Found(val index: Int) : SpaceLocateTargetPageAction
    data object Wait : SpaceLocateTargetPageAction
    data object LoadMore : SpaceLocateTargetPageAction
    data object LoadFailed : SpaceLocateTargetPageAction
    data object Missing : SpaceLocateTargetPageAction
}

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

internal fun shouldPromptToLocatePlayedVideo(
    targetBvid: String?,
    hasLoadedSpace: Boolean,
    promptEnabled: Boolean,
    promptHandled: Boolean
): Boolean {
    return promptEnabled && hasLoadedSpace && targetBvid?.isNotBlank() == true && !promptHandled
}

internal fun resolveSpaceWatchProgressByBvid(
    history: List<HistoryData>,
    upMid: Long
): Map<String, SpaceWatchProgress> {
    if (upMid <= 0L) return emptyMap()

    return buildMap {
        history.forEach { item ->
            val bvid = item.history?.bvid?.trim().orEmpty()
            if (
                item.author_mid != upMid ||
                bvid.isBlank() ||
                HistoryBusiness.fromValue(item.history?.business.orEmpty()) != HistoryBusiness.ARCHIVE ||
                containsKey(bvid)
            ) {
                return@forEach
            }
            put(
                bvid,
                SpaceWatchProgress(
                    bvid = bvid,
                    cid = item.history?.cid ?: 0L,
                    title = item.title.trim(),
                    progressSec = item.progress,
                    durationSec = item.duration,
                    viewAt = item.view_at
                )
            )
        }
    }
}

internal fun resolveSpaceLastWatchedVideo(
    progressByBvid: Map<String, SpaceWatchProgress>
): SpaceWatchProgress? {
    return progressByBvid.values.maxByOrNull { it.viewAt }
}

internal fun resolveSpaceVideoProgressState(
    video: SpaceVideoItem,
    localPositionMs: Long,
    syncedProgress: SpaceWatchProgress? = null
): VideoProgressDisplayState {
    val durationSec = parseSpaceVideoLengthToSeconds(video.length)
        .toInt()
        .takeIf { it > 0 }
        ?: syncedProgress?.durationSec.orZero()
    return resolveVideoDisplayProgressState(
        serverProgressSec = syncedProgress?.progressSec ?: 0,
        durationSec = durationSec,
        localPositionMs = if (syncedProgress == null) localPositionMs else 0L,
        viewAt = syncedProgress?.viewAt ?: if (localPositionMs > 0L) 1L else 0L
    )
}

internal fun resolveSpacePlaybackTarget(
    syncedProgress: SpaceWatchProgress?,
    localPositionMs: Long
): SpacePlaybackTarget {
    if (syncedProgress != null) {
        val resumePositionMs = syncedProgress.progressSec
            .takeIf { it > 0 }
            ?.times(1_000L)
            ?: 0L
        return SpacePlaybackTarget(
            cid = syncedProgress.cid.takeIf { resumePositionMs > 0L } ?: 0L,
            resumePositionMs = resumePositionMs
        )
    }
    return SpacePlaybackTarget(cid = 0L, resumePositionMs = localPositionMs.coerceAtLeast(0L))
}

internal fun resolveSpaceLocateTargetPageAction(
    targetBvid: String,
    videos: List<SpaceVideoItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    lastLoadFailed: Boolean = false,
): SpaceLocateTargetPageAction {
    val targetIndex = videos.indexOfFirst { it.bvid == targetBvid }
    return when {
        targetIndex >= 0 -> SpaceLocateTargetPageAction.Found(targetIndex)
        isLoading -> SpaceLocateTargetPageAction.Wait
        lastLoadFailed -> SpaceLocateTargetPageAction.LoadFailed
        hasMore -> SpaceLocateTargetPageAction.LoadMore
        else -> SpaceLocateTargetPageAction.Missing
    }
}

private fun Int?.orZero(): Int = this ?: 0

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
