package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewInfo

/** One notice belongs to the exact load that restored saved progress. */
internal data class PlaybackResumeNotice(
    val requestToken: Long,
    val bvid: String,
    val cid: Long,
    val positionMs: Long,
) {
    val mediaKey: String get() = "$bvid:$cid"
}

internal fun shouldAnnounceSavedPlaybackPosition(
    cachedPositionMs: Long,
    startPositionMs: Long,
    fallbackResumePositionMs: Long,
    ignoreSavedProgress: Boolean,
    skipPlayerPrepare: Boolean,
): Boolean = !ignoreSavedProgress && !skipPlayerPrepare && fallbackResumePositionMs <= 0L &&
    cachedPositionMs >= 1_000L && startPositionMs == cachedPositionMs

internal fun hasReachedSavedPlaybackPosition(
    targetPositionMs: Long,
    currentPositionMs: Long,
    playerReady: Boolean,
): Boolean = playerReady && targetPositionMs >= 1_000L &&
    currentPositionMs >= (targetPositionMs - 1_000L).coerceAtLeast(0L) &&
    currentPositionMs <= targetPositionMs + 5_000L

internal fun buildPlaybackResumeNoticeMessage(positionMs: Long): String =
    "欢迎回来，已为您定位至 ${FormatUtils.formatDuration(positionMs)}"

/** 「上次看到第 N P」提示：绑定本次加载请求与视频身份，切分P或换视频即失效。
 *  作为公开 UiState Success 的字段，必须保持 public 供 data class copy() 暴露。 */
data class PartResumeNotice(
    val requestToken: Long,
    val bvid: String,
    val currentCid: Long,
    val targetCid: Long,
    val partIndex: Int,
) {
    val partNumber: Int get() = partIndex + 1
    val mediaKey: String get() = "$bvid:$targetCid"
}

internal fun buildPartResumeNoticeMessage(partNumber: Int): String =
    "上次看到第${partNumber}P，点击跳转"

internal fun resolvePartResumeNotice(
    requestToken: Long,
    info: ViewInfo,
    lastPlayCid: Long,
    requestedCid: Long,
    noticeEnabled: Boolean,
): PartResumeNotice? {
    if (!noticeEnabled) return null
    if (lastPlayCid <= 0L || lastPlayCid == info.cid) return null
    if (info.pages.size <= 1) return null
    val index = info.pages.indexOfFirst { it.cid == lastPlayCid }
    if (index < 0) return null
    // 显式指定了别的分P（如从选集菜单进入）时不再提示。
    if (requestedCid > 0L && requestedCid != info.cid) return null
    return PartResumeNotice(
        requestToken = requestToken,
        bvid = info.bvid,
        currentCid = info.cid,
        targetCid = lastPlayCid,
        partIndex = index,
    )
}
