package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.android.purebilibili.core.util.resolveSafeAndroidPipRational
import com.android.purebilibili.feature.video.player.buildPipPlaybackRemoteActions
import com.android.purebilibili.feature.video.ui.section.shouldKeepVideoPlaybackAwake

@Composable
internal fun VideoDetailPipParamsEffect(
    context: Context,
    activity: Activity?,
    playerBounds: Rect?,
    pipModeEnabled: Boolean,
    player: Player?,
) {
    var lastPipBounds by remember { mutableStateOf<Rect?>(null) }
    var lastPipModeEnabled by remember { mutableStateOf<Boolean?>(null) }
    var lastPipUpdateElapsedMs by remember { mutableLongStateOf(0L) }
    val latestPlayer by rememberUpdatedState(player)

    LaunchedEffect(activity, playerBounds, pipModeEnabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) return@LaunchedEffect
        val modeChanged = lastPipModeEnabled == null || lastPipModeEnabled != pipModeEnabled
        val boundsChanged = hasMeaningfulVideoPlayerBoundsChange(lastPipBounds, playerBounds)
        val now = android.os.SystemClock.elapsedRealtime()
        if (!shouldApplyPipParamsUpdate(
                pipModeEnabled = pipModeEnabled,
                modeChanged = modeChanged,
                boundsChanged = boundsChanged,
                elapsedSinceLastUpdateMs = now - lastPipUpdateElapsedMs,
            )
        ) return@LaunchedEffect

        lastPipBounds = playerBounds?.let(::Rect)
        lastPipModeEnabled = pipModeEnabled
        lastPipUpdateElapsedMs = now
        val videoWidth = latestPlayer?.videoSize?.width ?: 0
        val videoHeight = latestPlayer?.videoSize?.height ?: 0
        val params = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(resolveSafeAndroidPipRational(videoWidth, videoHeight))
            .setActions(buildPipPlaybackRemoteActions(context, latestPlayer))
            .apply {
                playerBounds?.let(::setSourceRectHint)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(pipModeEnabled)
                    setSeamlessResizeEnabled(pipModeEnabled)
                }
            }
            .build()
        activity.setPictureInPictureParams(params)
    }
}

@Composable
internal fun VideoDetailKeepScreenOnEffect(
    window: Window?,
    player: Player,
) {
    val shouldKeepAwake by produceState(
        initialValue = shouldKeepVideoPlaybackAwake(
            playWhenReady = player.playWhenReady,
            isPlaying = player.isPlaying,
            playbackState = player.playbackState,
        ),
        key1 = player,
    ) {
        fun update() {
            value = shouldKeepVideoPlaybackAwake(
                playWhenReady = player.playWhenReady,
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
            )
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = update()
            override fun onPlaybackStateChanged(playbackState: Int) = update()
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = update()
        }
        player.addListener(listener)
        awaitDispose { player.removeListener(listener) }
    }
    DisposableEffect(window, shouldKeepAwake) {
        if (shouldKeepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (shouldKeepAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
internal fun VideoDetailSystemBarsEffect(
    view: View,
    window: Window?,
    insetsController: WindowInsetsControllerCompat?,
    isScreenActive: Boolean,
    spec: VideoDetailSystemBarsApplySpec,
    reapplyGeneration: Int = 0,
) {
    // The generation re-commits the same policy after a cancelled return or a retained route
    // becomes topmost again.
    val effectiveSpec = remember(spec, reapplyGeneration) { spec }
    SideEffect {
        if (view.isInEditMode || !isScreenActive || window == null || insetsController == null) return@SideEffect
        applyVideoDetailSystemBarsSpec(window, insetsController, effectiveSpec)
    }
}
