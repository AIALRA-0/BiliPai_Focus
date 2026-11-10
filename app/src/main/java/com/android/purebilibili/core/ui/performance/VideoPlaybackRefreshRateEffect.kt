package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.delay

private const val VIDEO_REFRESH_IDLE_DELAY_MS = 2500L

private fun Context.videoRefreshActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.videoRefreshActivity()
    else -> null
}

/** 仅撤销自己仍持有的请求，避免覆盖随后由其他页面写入的窗口模式。 */
private class VideoDisplayModeRequest(private val window: Window) {
    private var originalMode = 0
    private var originalRate = 0f
    private var requestedMode: Int? = null

    fun apply(modeId: Int) {
        val attributes = window.attributes
        if (requestedMode == modeId && attributes.preferredDisplayModeId == modeId) return
        release()
        originalMode = window.attributes.preferredDisplayModeId
        originalRate = window.attributes.preferredRefreshRate
        requestedMode = modeId
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = modeId
            preferredRefreshRate = 0f
        }
    }

    fun release() {
        val ownedMode = requestedMode ?: return
        requestedMode = null
        val attributes = window.attributes
        if (attributes.preferredDisplayModeId != ownedMode || attributes.preferredRefreshRate != 0f) return
        window.attributes = attributes.apply {
            preferredDisplayModeId = originalMode
            preferredRefreshRate = originalRate
        }
    }
}

/**
 * 无弹幕且控件隐藏时临时请求视频所需档位；触摸、键盘、失焦、暂停和退出时释放。
 * 与播放器材质无关，不限制解码帧率，也不切换显示分辨率。
 */
@Composable
internal fun VideoPlaybackRefreshRateEffect(player: ExoPlayer, eligible: Boolean) {
    if (!eligible) return
    val activity = LocalContext.current.videoRefreshActivity() ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val window = activity.window
    val request = remember(window) { VideoDisplayModeRequest(window) }
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    var frameRate by remember(player) { mutableFloatStateOf(player.videoFormat?.frameRate ?: 0f) }
    var speed by remember(player) { mutableFloatStateOf(player.playbackParameters.speed) }
    var interaction by remember(window) { mutableIntStateOf(0) }
    var pointerDown by remember(window) { mutableStateOf(false) }
    var focused by remember(window) { mutableStateOf(window.decorView.hasWindowFocus()) }
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(player, window, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playing = player.isPlaying
                speed = player.playbackParameters.speed
                frameRate = (player as ExoPlayer).videoFormat?.frameRate ?: 0f
                if (!playing || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                    events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)
                ) {
                    request.release()
                    interaction++
                }
            }
        }
        val analytics = object : AnalyticsListener {
            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                request.release()
                frameRate = format.frameRate
                interaction++
            }
        }
        val observer = LifecycleEventObserver { owner, _ ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (!resumed) request.release()
        }
        val originalCallback = window.callback ?: activity
        val callback = object : Window.Callback by originalCallback {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                request.release()
                pointerDown = event.actionMasked != MotionEvent.ACTION_UP &&
                    event.actionMasked != MotionEvent.ACTION_CANCEL
                if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                    event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) interaction++
                return originalCallback.dispatchTouchEvent(event)
            }

            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                request.release()
                interaction++
                return originalCallback.dispatchKeyEvent(event)
            }

            override fun onWindowFocusChanged(hasFocus: Boolean) {
                focused = hasFocus
                if (!hasFocus) request.release()
                originalCallback.onWindowFocusChanged(hasFocus)
            }
        }
        window.callback = callback
        player.addListener(listener)
        player.addAnalyticsListener(analytics)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            request.release()
            player.removeListener(listener)
            player.removeAnalyticsListener(analytics)
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (window.callback === callback) window.callback = originalCallback
        }
    }

    LaunchedEffect(activity, playing, frameRate, speed, interaction, pointerDown, focused, resumed) {
        request.release()
        if (!playing || pointerDown || !focused || !resumed) return@LaunchedEffect
        delay(VIDEO_REFRESH_IDLE_DELAY_MS)
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return@LaunchedEffect
        val current = display.mode
        val mode = resolveIdleVideoDisplayMode(
            modes = activity.supportedAppDisplayModes(),
            width = current.physicalWidth,
            height = current.physicalHeight,
            frameRate = frameRate,
            speed = speed,
        ) ?: return@LaunchedEffect
        // 系统已经处于更低档位时，不为节能策略反向拉高刷新率。
        if (mode.refreshRate <= current.refreshRate + 0.1f) request.apply(mode.modeId)
    }
}
