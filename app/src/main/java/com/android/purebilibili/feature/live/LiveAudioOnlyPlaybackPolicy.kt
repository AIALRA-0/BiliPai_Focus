package com.android.purebilibili.feature.live

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters

internal fun shouldBindLivePlayerViewForAudioOnly(
    isAudioOnly: Boolean
): Boolean {
    return !isAudioOnly
}

internal fun shouldRenderLiveDanmakuOverlayForAudioOnly(
    isDanmakuEnabled: Boolean,
    isAudioOnly: Boolean
): Boolean {
    return isDanmakuEnabled && !isAudioOnly
}

internal fun shouldUseTextureSurfaceForLivePlayer(
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return hasSharedTransitionScope && hasAnimatedVisibilityScope
}

internal fun resolveLiveTrackSelectionParametersForAudioOnly(
    currentTrackSelectionParameters: TrackSelectionParameters,
    isAudioOnly: Boolean
): TrackSelectionParameters {
    if (!isAudioOnly) return currentTrackSelectionParameters
    return currentTrackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        .setMaxVideoSize(0, 0)
        .build()
}
