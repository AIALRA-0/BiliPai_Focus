package com.android.purebilibili.core.player.dash

import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.core.player.policy.PlaybackQualityMode

data class AdaptiveDashPlaybackSource(
    val manifest: String,
    val videoTracks: List<DashVideo>,
    val audioTracks: List<DashAudio>,
    val playbackQualityMode: PlaybackQualityMode
)
