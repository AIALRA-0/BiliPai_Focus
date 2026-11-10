package com.android.purebilibili.feature.video.state

import androidx.media3.common.PlaybackException

internal typealias PlayerErrorRecoveryAction = com.android.purebilibili.core.player.PlayerErrorRecoveryAction
internal fun decidePlayerErrorRecovery(errorCode: Int, hasCdnAlternatives: Boolean, retryCount: Int, maxRetries: Int,
    cdnSwitchCount: Int, maxCdnSwitches: Int, isDecoderLikeFailure: Boolean, isPremiumAudioFailure: Boolean = false) =
    com.android.purebilibili.core.player.decidePlayerErrorRecovery(errorCode, hasCdnAlternatives, retryCount, maxRetries,
        cdnSwitchCount, maxCdnSwitches, isDecoderLikeFailure, isPremiumAudioFailure)
internal fun isNetworkPlaybackError(errorCode: Int) = com.android.purebilibili.core.player.isNetworkPlaybackError(errorCode)
internal fun isDecoderLikeFailure(errorMessage: String?, causeClassName: String?) =
    com.android.purebilibili.core.player.isDecoderLikeFailure(errorMessage, causeClassName)

internal fun shouldRunDelayedPlayerErrorRecovery(
    reportedError: PlaybackException,
    activePlayerError: PlaybackException?
): Boolean = activePlayerError === reportedError
