package com.android.purebilibili.feature.video.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.viewmodel.PlayerToastMessage
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.delay

@Stable
internal class VideoDetailPlaybackEventState(
    val danmakuManager: DanmakuManager,
) {
    var popupMessage by mutableStateOf<PlayerToastMessage?>(null)
        internal set
}

@Composable
internal fun rememberVideoDetailPlaybackEventState(playbackIdentity: String): VideoDetailPlaybackEventState {
    val danmakuManager = rememberDanmakuManager(playbackIdentity)
    return remember(danmakuManager) { VideoDetailPlaybackEventState(danmakuManager) }
}

@Composable
internal fun VideoDetailPlaybackEventEffects(
    context: Context,
    viewModel: VideoPlaybackViewModel,
    state: VideoDetailPlaybackEventState,
) {
    LaunchedEffect(viewModel, state) {
        viewModel.toastEvent.collect { message ->
            state.popupMessage = message
            delay(2_000)
            state.popupMessage = null
        }
    }
    LaunchedEffect(viewModel, state.danmakuManager) {
        viewModel.danmakuSentEvent.collect { danmakuData ->
            state.danmakuManager.addLocalDanmaku(
                text = danmakuData.text,
                color = danmakuData.color,
                mode = danmakuData.mode,
                fontSize = danmakuData.fontSize,
            )
        }
    }
    LaunchedEffect(viewModel, context.applicationContext) {
        viewModel.initWithContext(context)
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("VideoDetailScreen")
    }
}
