package com.android.purebilibili.feature.video.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.viewmodel.PlayerToastMessage
import com.android.purebilibili.feature.video.viewmodel.PlayerToastPresentation
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.delay

@Stable
internal class VideoDetailPlaybackEventState(
    val danmakuManager: DanmakuManager,
) {
    var popupMessage by mutableStateOf<PlayerToastMessage?>(null)
        internal set
    var resumeMessage by mutableStateOf<PlayerToastMessage?>(null)
        internal set
    // Owned by the screen so changing player layout/orientation cannot replay the notice.
    var resumeMessageDeadlineMs by mutableStateOf<Long?>(null)
        internal set
}

internal val LocalPlaybackResumeFeedback = compositionLocalOf<VideoDetailPlaybackEventState?> { null }

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
    LaunchedEffect(viewModel, state, "resume-identity") {
        viewModel.subjectSnapshot.collect { subject ->
            val pending = state.resumeMessage
            if (subject != null && pending != null &&
                pending.mediaKey != "${subject.bvid}:${subject.cid}"
            ) {
                state.resumeMessage = null
                state.resumeMessageDeadlineMs = null
            }
        }
    }
    LaunchedEffect(viewModel, state) {
        viewModel.toastEvent.collect { message ->
            if (message.mediaKey != null) {
                val current = viewModel.subjectSnapshot.value
                if (current == null || message.mediaKey != "${current.bvid}:${current.cid}") {
                    return@collect
                }
            }
            if (message.presentation == PlayerToastPresentation.PlaybackResume) {
                state.resumeMessageDeadlineMs = null
                state.resumeMessage = message
                return@collect
            }
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
                isVipGradualColor = danmakuData.isVipGradualColor,
            )
        }
    }
    LaunchedEffect(viewModel, context.applicationContext) {
        viewModel.initWithContext(context)
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("VideoDetailScreen")
    }
}
