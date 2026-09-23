@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.plugin.js

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.plugin.js.ExternalMediaLaunchStore
import com.android.purebilibili.core.ui.rememberAppBackIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalMediaPlayerScreen(
    launchId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(launchId) { ExternalMediaLaunchStore.get(launchId) }
    var selectedIndex by remember(request) { mutableIntStateOf(request?.selectedStreamIndex ?: 0) }
    val stream = request?.streams?.getOrNull(selectedIndex)
    val dataSourceFactory = remember(stream?.headers) {
        DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(stream?.headers.orEmpty())
        )
    }
    val player = remember(context, dataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(stream?.url) {
        val current = stream ?: return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(current.url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(request?.title.orEmpty())
                    .build()
            )
            .setMimeType(resolveExternalMediaMimeType(current.url, current.contentType))
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = request?.title ?: "外部媒体",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (request == null || request.streams.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "播放请求已失效，请从插件内容重新打开",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@AppScaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = player
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                request.streams.forEachIndexed { index, mediaStream ->
                    AppFilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { AppText(mediaStream.title.ifBlank { "线路 ${index + 1}" }) }
                    )
                }
            }
        }
    }
}

private fun resolveExternalMediaMimeType(
    url: String,
    contentType: String?
): String? {
    val declared = contentType?.lowercase()?.takeIf { it.isNotBlank() }
    return when {
        declared?.contains("mpegurl") == true || declared?.contains("hls") == true -> MimeTypes.APPLICATION_M3U8
        url.substringBefore("?").endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        url.substringBefore("?").endsWith(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
        else -> null
    }
}
