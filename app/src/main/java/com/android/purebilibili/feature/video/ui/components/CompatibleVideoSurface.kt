package com.android.purebilibili.feature.video.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import com.android.purebilibili.R
import com.android.purebilibili.feature.video.ui.section.requiresHdrSurfaceOutput
import com.android.purebilibili.feature.video.ui.section.shouldUseTextureSurfaceForFlip

/** Observe decoder format changes as well as the selected quality across player handoffs. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun rememberVideoSurfaceHdrRequired(player: ExoPlayer, currentQualityId: Int = 0): Boolean {
    var colorTransfer by remember(player) {
        mutableIntStateOf(player.videoFormat?.colorInfo?.colorTransfer ?: 0)
    }
    DisposableEffect(player) {
        val listener = object : AnalyticsListener {
            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?,
            ) {
                colorTransfer = format.colorInfo?.colorTransfer ?: 0
            }
        }
        player.addAnalyticsListener(listener)
        onDispose { player.removeAnalyticsListener(listener) }
    }
    return requiresHdrSurfaceOutput(currentQualityId, colorTransfer)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun rememberCompatibleVideoTextureSurface(player: ExoPlayer, currentQualityId: Int = 0): Boolean {
    return shouldUseTextureSurfaceForFlip(
        isFlippedHorizontal = false,
        isFlippedVertical = false,
        requiresHdrSurfaceOutput = rememberVideoSurfaceHdrRequired(player, currentQualityId),
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun createVideoPlayerView(context: Context, useTextureSurface: Boolean): PlayerView =
    if (useTextureSurface) {
        LayoutInflater.from(context).inflate(R.layout.view_player_texture, null, false) as PlayerView
    } else {
        PlayerView(context)
    }
