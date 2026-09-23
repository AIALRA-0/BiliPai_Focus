package com.android.purebilibili.feature.anime4k

internal const val ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS = 3_000L

/**
 * Anime4K 的输入 Surface 已接管播放器后，如果迟迟没有可见 GL 首帧，就回退到 PlayerView
 * 直出。慢网络也允许回退；直出只会失去本次播放的增强效果，不应让播放永久停在封面。
 */
internal fun shouldFallbackAnime4KBeforeFirstFrame(
    pipelineRequested: Boolean,
    inputSurfaceReady: Boolean,
    displayedFirstFrame: Boolean,
    playWhenReady: Boolean,
    mediaItemCount: Int,
    elapsedMs: Long,
): Boolean = pipelineRequested &&
    inputSurfaceReady &&
    !displayedFirstFrame &&
    playWhenReady &&
    mediaItemCount > 0 &&
    elapsedMs >= ANIME4K_FIRST_FRAME_FALLBACK_TIMEOUT_MS
