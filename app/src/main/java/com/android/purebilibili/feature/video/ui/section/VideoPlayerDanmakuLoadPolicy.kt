package com.android.purebilibili.feature.video.ui.section

data class VideoPlayerDanmakuLoadPolicy(
    val shouldEnable: Boolean,
    val shouldLoadImmediately: Boolean,
    val durationHintMs: Long
) {
    val shouldLoad: Boolean get() = shouldLoadImmediately
}

fun resolveVideoPlayerDanmakuLoadPolicy(
    cid: Long,
    danmakuEnabled: Boolean,
    durationHintMs: Long = 0L
): VideoPlayerDanmakuLoadPolicy {
    val canLoad = cid > 0 && danmakuEnabled
    return VideoPlayerDanmakuLoadPolicy(
        shouldEnable = canLoad,
        shouldLoadImmediately = canLoad,
        durationHintMs = durationHintMs.coerceAtLeast(0L)
    )
}

enum class VideoPlayerDanmakuEngineSyncAction {
    Enable,
    DisableAndClear
}

fun resolveVideoPlayerDanmakuEngineSyncAction(
    danmakuEnabled: Boolean,
    cid: Long
): VideoPlayerDanmakuEngineSyncAction {
    return if (cid > 0L && danmakuEnabled) {
        VideoPlayerDanmakuEngineSyncAction.Enable
    } else {
        VideoPlayerDanmakuEngineSyncAction.DisableAndClear
    }
}

/**
 * Navigation keeps the outgoing detail entry composed during its transition. Both entries share
 * the singleton DanmakuManager, so only the foreground detail host may bind/load the engine.
 */
fun shouldRunVideoPlayerDanmakuHostEffects(
    danmakuHostActive: Boolean,
    hostLifecycleStarted: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = danmakuHostActive && hostLifecycleStarted && !isPortraitFullscreen
