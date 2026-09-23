package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

internal enum class VideoDetailReturnPhase {
    Idle,
    Previewing,
    Committed
}

@Stable
internal class VideoDetailPresentationState private constructor(
    initialBvid: String,
    initialCid: Long,
    initialSelectedTabIndex: Int,
    initialPortraitFullscreen: Boolean,
    initialPipMode: Boolean,
) {
    internal val currentBvidState = mutableStateOf(initialBvid)
    internal val currentCidState = mutableLongStateOf(initialCid)
    internal val selectedTabIndexState = mutableIntStateOf(initialSelectedTabIndex)
    internal val portraitFullscreenState = mutableStateOf(initialPortraitFullscreen)
    internal val pipModeState = mutableStateOf(initialPipMode)
    internal val navigatingToVideoState = mutableStateOf(false)
    internal val navigatingToAudioModeState = mutableStateOf(false)
    internal val navigatingToMiniModeState = mutableStateOf(false)

    var returnPhase = mutableStateOf(VideoDetailReturnPhase.Idle)
        private set

    fun switchVideo(bvid: String, cid: Long) {
        currentBvidState.value = bvid
        currentCidState.longValue = cid
        selectedTabIndexState.intValue = 0
    }

    fun syncPlaybackIdentity(bvid: String, cid: Long) {
        currentBvidState.value = bvid
        currentCidState.longValue = cid
    }

    fun selectTab(index: Int) {
        selectedTabIndexState.intValue = index
    }

    fun setPortraitFullscreen(enabled: Boolean) {
        portraitFullscreenState.value = enabled
    }

    fun syncPipMode(enabled: Boolean) {
        pipModeState.value = enabled
    }

    fun markNavigatingToVideo() {
        navigatingToVideoState.value = true
    }

    fun clearNavigatingToVideo() {
        navigatingToVideoState.value = false
    }

    fun markNavigatingToAudioMode() {
        navigatingToAudioModeState.value = true
    }

    fun clearNavigatingToAudioMode() {
        navigatingToAudioModeState.value = false
    }

    fun markNavigatingToMiniMode() {
        navigatingToMiniModeState.value = true
    }

    fun updateReturnPhase(phase: VideoDetailReturnPhase) {
        returnPhase.value = phase
    }

    companion object {
        val Saver = listSaver<VideoDetailPresentationState, Any>(
            save = {
                listOf(
                    it.currentBvidState.value,
                    it.currentCidState.longValue,
                    it.selectedTabIndexState.intValue,
                    it.portraitFullscreenState.value,
                )
            },
            restore = {
                VideoDetailPresentationState(
                    initialBvid = it[0] as String,
                    initialCid = it[1] as Long,
                    initialSelectedTabIndex = it[2] as Int,
                    initialPortraitFullscreen = it[3] as Boolean,
                    initialPipMode = false,
                )
            },
        )

        fun create(
            initialBvid: String,
            initialCid: Long,
            initialPortraitFullscreen: Boolean,
            initialPipMode: Boolean,
        ) = VideoDetailPresentationState(
            initialBvid = initialBvid,
            initialCid = initialCid,
            initialSelectedTabIndex = 0,
            initialPortraitFullscreen = initialPortraitFullscreen,
            initialPipMode = initialPipMode,
        )
    }
}

@Composable
internal fun rememberVideoDetailPresentationState(
    routeBvid: String,
    initialCid: Long,
    initialPortraitFullscreen: Boolean,
    initialPipMode: Boolean,
): VideoDetailPresentationState = rememberSaveable(
    routeBvid,
    // 与路由直达竖屏意图一起作为 key，避免上次详情态把 isPortraitFullscreen=false 复写回来。
    initialPortraitFullscreen,
    saver = VideoDetailPresentationState.Saver,
) {
    VideoDetailPresentationState.create(
        initialBvid = routeBvid,
        initialCid = initialCid,
        initialPortraitFullscreen = initialPortraitFullscreen,
        initialPipMode = initialPipMode,
    )
}
