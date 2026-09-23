package com.android.purebilibili.core.ui.wallpaper

import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.asDrawable
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.android.purebilibili.R

internal fun isVideoWallpaper(uri: String): Boolean =
    uri.substringBefore('?').substringBefore('#').substringAfterLast('.').lowercase() in
        setOf("mp4", "m4v", "webm", "mkv", "mov", "3gp", "video")

/** Shared image/GIF/video renderer. Video uses a TextureView so Compose clipping works. */
@Composable
internal fun WallpaperMedia(
    uri: String,
    modifier: Modifier = Modifier,
    imageModel: Any = uri,
    alignment: Alignment = Alignment.Center,
    playbackEnabled: Boolean = true,
    video: Boolean = isVideoWallpaper(uri),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val playing = foreground && playbackEnabled
    val resolvedVideo by produceState(video, uri, video) {
        value = video || withContext(Dispatchers.IO) {
            if (uri.startsWith("content:")) {
                runCatching { context.contentResolver.getType(Uri.parse(uri))?.startsWith("video/") == true }
                    .getOrDefault(false)
            } else false
        }
    }
    if (resolvedVideo) {
        val player = remember(context, uri) {
            ExoPlayer.Builder(context).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                prepare()
            }
        }
        DisposableEffect(player) { onDispose { player.release() } }
        SideEffect { player.playWhenReady = playing }
        AndroidView(
            modifier = modifier,
            factory = {
                (LayoutInflater.from(it).inflate(R.layout.wallpaper_video, null) as PlayerView).apply {
                    useController = false
                    this.player = player
                    isFocusable = false
                    isClickable = false
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            },
            update = { it.player = player },
            onRelease = { it.player = null },
        )
    } else {
        var animation by remember(uri) { mutableStateOf<Animatable?>(null) }
        DisposableEffect(animation, playing) {
            val current = animation
            if (playing) current?.start() else current?.stop()
            onDispose { current?.stop() }
        }
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            modifier = modifier,
            onSuccess = { animation = it.result.image.asDrawable(context.resources) as? Animatable },
        )
    }
}
