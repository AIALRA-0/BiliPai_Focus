package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.usecase.resolveVideoLoadDurationMs

data class VideoDetailUiState(
    val requestBvid: String = "",
    val requestCid: Long = 0L,
    val requestToken: Long = 0L,
    val info: ViewInfo? = null,
    val related: List<RelatedVideo> = emptyList(),
    val relatedLoadState: VideoPlaybackUiState.RelatedLoadState = VideoPlaybackUiState.RelatedLoadState.LOADING,
    val isLoggedIn: Boolean = false,
    val isVip: Boolean = false
) {
    /** 同一内容重建播放请求时保留已到达的正文；换视频或分 P 时清空。 */
    fun forNewRequest(
        bvid: String,
        cid: Long,
        token: Long,
        playbackState: VideoPlaybackUiState
    ): VideoDetailUiState {
        val content = contentSnapshot(playbackState, bvid, cid)
        return VideoDetailUiState(
            requestBvid = bvid,
            requestCid = cid,
            requestToken = token,
            info = content?.info,
            related = content?.related.orEmpty(),
            relatedLoadState = content?.relatedLoadState ?: VideoPlaybackUiState.RelatedLoadState.LOADING,
            isLoggedIn = content?.isLoggedIn ?: false,
            isVip = content?.isVip ?: false
        )
    }

    fun contentSnapshot(
        playbackState: VideoPlaybackUiState,
        bvid: String,
        cid: Long
    ): VideoPlaybackUiState.Success? {
        val playback = (playbackState as? VideoPlaybackUiState.Success)?.takeIf {
            val matchesVideo = it.info.bvid == bvid ||
                (requestBvid == bvid && info?.bvid == it.info.bvid)
            matchesVideo && (cid <= 0L || it.info.cid == cid)
        }
        if (playback != null) return playback
        if (requestBvid != bvid) return null
        val detail = info ?: return null
        // 未指定分 P 的请求解析出 CID 后仍属于同一次详情，不能退回骨架。
        if (cid > 0L && requestCid != cid && detail.cid != cid) return null
        return VideoPlaybackUiState.Success(
            info = detail,
            playUrl = "",
            related = related,
            relatedLoadState = relatedLoadState,
            isLoggedIn = isLoggedIn,
            isVip = isVip,
            videoDurationMs = resolveVideoLoadDurationMs(0L, detail)
        )
    }
}
