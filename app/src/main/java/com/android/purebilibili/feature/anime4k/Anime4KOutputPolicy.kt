@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.feature.anime4k

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.C

enum class Anime4KBypassReason {
    NONE,
    DISABLED,
    GL_UNAVAILABLE,
    HDR_OR_DOLBY_VISION,
    PICTURE_IN_PICTURE,
    AUDIO_ONLY,
    HOST_NOT_STARTED
}

data class Anime4KOutputDecision(
    val shouldUsePipeline: Boolean,
    val bypassReason: Anime4KBypassReason
)

fun resolveAnime4KOutputDecision(
    pluginEnabled: Boolean,
    glAvailable: Boolean,
    colorTransfer: Int,
    sampleMimeType: String?,
    isInPipMode: Boolean,
    isAudioOnly: Boolean,
    hostLifecycleStarted: Boolean
): Anime4KOutputDecision {
    val bypassReason = when {
        !pluginEnabled -> Anime4KBypassReason.DISABLED
        !glAvailable -> Anime4KBypassReason.GL_UNAVAILABLE
        isAnime4kHdrOrDolbyVision(colorTransfer, sampleMimeType) -> Anime4KBypassReason.HDR_OR_DOLBY_VISION
        isInPipMode -> Anime4KBypassReason.PICTURE_IN_PICTURE
        isAudioOnly -> Anime4KBypassReason.AUDIO_ONLY
        !hostLifecycleStarted -> Anime4KBypassReason.HOST_NOT_STARTED
        else -> Anime4KBypassReason.NONE
    }
    return Anime4KOutputDecision(
        shouldUsePipeline = bypassReason == Anime4KBypassReason.NONE,
        bypassReason = bypassReason
    )
}

fun isAnime4kHdrOrDolbyVision(colorTransfer: Int, sampleMimeType: String?): Boolean {
    return colorTransfer == C.COLOR_TRANSFER_ST2084 ||
        colorTransfer == C.COLOR_TRANSFER_HLG ||
        sampleMimeType.equals("video/dolby-vision", ignoreCase = true)
}

fun isAnime4KGles3Available(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return (activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0) >= 0x30000
}
