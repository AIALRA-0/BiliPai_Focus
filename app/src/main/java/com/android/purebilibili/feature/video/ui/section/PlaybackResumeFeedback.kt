package com.android.purebilibili.feature.video.ui.section

import android.os.SystemClock
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.video.screen.LocalPlaybackResumeFeedback
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.runtime.getValue

/** Media feedback lives inside the player in both native and glass layouts. */
@Composable
internal fun BoxScope.PlaybackResumeFeedback(
    mediaKey: String?,
    ready: Boolean,
    fullscreen: Boolean,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = LocalPlaybackResumeFeedback.current ?: return
    val message = state.resumeMessage ?: return
    val matchesMedia = mediaKey != null && message.mediaKey == mediaKey
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val canPresent = matchesMedia && ready && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(state, message, mediaKey, canPresent) {
        if (!canPresent) return@LaunchedEffect
        val deadline = state.resumeMessageDeadlineMs
            ?: (SystemClock.elapsedRealtime() + 2_000L).also {
                state.resumeMessageDeadlineMs = it
            }
        delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
        if (state.resumeMessage == message) {
            state.resumeMessage = null
            state.resumeMessageDeadlineMs = null
        }
    }
    if (canPresent && (state.resumeMessageDeadlineMs?.let { it > SystemClock.elapsedRealtime() } != false)) {
        AppSurface(
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    // 控制栏隐藏时靠近画面底边；显示时才预留控制栏空间。
                    bottom = if (!controlsVisible) 12.dp else if (fullscreen) 112.dp else 64.dp,
                )
                .widthIn(max = 320.dp)
                .zIndex(20f),
        ) {
            AppText(
                text = message.message,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
