package com.android.purebilibili.feature.video.ambient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

/** 竖屏留白复用全屏动态环境光，开关、强度与省电策略由同一设置来源控制。 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun rememberPortraitAmbientPresentation(
    player: Player,
    playerView: PlayerView?,
    identity: Any,
    quality: Int,
    width: Int,
    height: Int,
    enabled: Boolean,
    firstFrameReady: Boolean,
    hdr: Boolean,
    anime4k: Boolean,
): AmbientPresentation? {
    if (!enabled) return null
    val presentation = remember(player, identity) { AmbientPresentation() }
    val controller = remember(presentation) { AmbientFrameController(presentation) }
    CompositionLocalProvider(
        LocalAmbientPresentation provides presentation,
        LocalAmbientController provides controller,
    ) {
        BindPlayerAmbient(
            player = player,
            playerView = playerView,
            identity = identity,
            quality = quality,
            width = width,
            height = height,
            foreground = true,
            excluded = false,
            hdr = hdr,
            anime4k = anime4k,
            transitioning = false,
            firstFrameReady = firstFrameReady,
            statusBar = false,
            diagnosticLogging = false,
        )
    }
    return presentation
}
