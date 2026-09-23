package com.android.purebilibili.feature.dynamic.components

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton

internal fun normalizeLivePhotoVideoUrl(value: String?): String? {
    val url = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("https://") -> url
        url.startsWith("http://") -> "https://" + url.removePrefix("http://")
        else -> null
    }
}

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/** A looping live photo video above the still image; uses a TextureView for Compose transforms. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun LivePhotoPlayback(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    isMuted: Boolean = false,
    playerRef: ((Player?) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var hasFrame by remember(videoUrl) { mutableStateOf(false) }
    var failed by remember(videoUrl) { mutableStateOf(false) }
    val player = remember(context, videoUrl) {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(BROWSER_USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.bilibili.com/",
                    "User-Agent" to BROWSER_USER_AGENT
                )
            )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build().apply {
                volume = if (isMuted) 0f else 1f
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasFrame = true
                failed = false
            }
            override fun onPlayerError(error: PlaybackException) {
                hasFrame = false
                failed = true
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    failed = false
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                player.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                player.play()
            }
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.prepare()
        player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        playerRef?.invoke(player)
        onDispose {
            playerRef?.invoke(null)
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, isPlaying) {
        if (isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }
    LaunchedEffect(player, isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }
    Box(
        modifier = modifier
            .pointerInput(videoUrl, onClick, onLongPress) {
                detectTapGestures(
                    onTap = { onClick?.invoke() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
    ) {
        AndroidView(
            factory = { viewContext ->
                val playerView = (LayoutInflater.from(viewContext).inflate(R.layout.live_photo_player, null) as PlayerView).apply {
                    this.player = player
                }
                object : FrameLayout(viewContext) {
                    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = false
                    override fun onTouchEvent(ev: MotionEvent?): Boolean = false
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    addView(playerView)
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (hasFrame) 1f else 0f },
            onRelease = { container ->
                ((container as? ViewGroup)?.getChildAt(0) as? PlayerView)?.player = null
                (container as? ViewGroup)?.removeAllViews()
            },
        )
        if (failed) {
            AppTextButton(
                onClick = {
                    failed = false
                    player.seekTo(0)
                    player.prepare()
                    player.play()
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            ) {
                AppText("实况加载失败 · 点击重试", color = Color.White)
            }
        }
    }
}
