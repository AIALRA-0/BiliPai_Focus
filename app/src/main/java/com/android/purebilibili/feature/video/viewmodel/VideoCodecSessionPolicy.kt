package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.DashVideo
import java.net.URI

internal const val AVC_CODEC_KEY = "avc1"
internal const val AV1_CODEC_KEY = "av01"
internal const val HEVC_CODEC_KEY = "hev1"

internal fun normalizeCodecFamilyKey(codec: String?): String? {
    val normalized = codec?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        normalized.startsWith("av01") -> AV1_CODEC_KEY
        normalized.startsWith("avc") || normalized.startsWith("h264") -> AVC_CODEC_KEY
        normalized.startsWith("hev") || normalized.startsWith("hvc") -> HEVC_CODEC_KEY
        else -> normalized.substringBefore('.')
    }
}

internal fun resolveEffectiveVideoCodecPreference(
    requestCodecOverride: String?,
    settingsCodecPreference: String,
    sessionBlockedCodecs: Set<String>
): String {
    val requestCodec = normalizeCodecFamilyKey(requestCodecOverride)
    if (requestCodec != null) {
        return requestCodec
    }

    val settingsCodec = normalizeCodecFamilyKey(settingsCodecPreference) ?: HEVC_CODEC_KEY
    return if (AV1_CODEC_KEY in sessionBlockedCodecs && settingsCodec == AV1_CODEC_KEY) {
        AVC_CODEC_KEY
    } else {
        settingsCodec
    }
}

internal fun resolveEffectiveVideoSecondCodecPreference(
    requestCodecOverride: String?,
    settingsSecondCodecPreference: String
): String {
    return if (normalizeCodecFamilyKey(requestCodecOverride) != null) {
        AVC_CODEC_KEY
    } else {
        normalizeCodecFamilyKey(settingsSecondCodecPreference) ?: AVC_CODEC_KEY
    }
}

internal fun resolveEffectiveAv1Support(
    deviceSupportsAv1: Boolean,
    sessionBlockedCodecs: Set<String>
): Boolean {
    return deviceSupportsAv1 && AV1_CODEC_KEY !in sessionBlockedCodecs
}

internal fun resolveNextVideoCodecFallback(
    failedCodec: String?,
    secondaryCodecPreference: String,
    isHevcSupported: Boolean,
    isAv1Supported: Boolean
): String? {
    val normalizedFailedCodec = normalizeCodecFamilyKey(failedCodec)
    return listOf(secondaryCodecPreference, AVC_CODEC_KEY)
        .mapNotNull(::normalizeCodecFamilyKey)
        .distinct()
        .firstOrNull { candidate ->
            candidate != normalizedFailedCodec && when (candidate) {
                HEVC_CODEC_KEY -> isHevcSupported
                AV1_CODEC_KEY -> isAv1Supported
                AVC_CODEC_KEY -> true
                else -> false
            }
        }
}

internal fun resolvePlaybackVideoCodec(
    videoUrl: String,
    cachedDashVideos: List<DashVideo>
): String? {
    if (videoUrl.isBlank()) return null
    val selectedResource = playbackResourceIdentity(videoUrl)
    val selectedVideo = cachedDashVideos.firstOrNull { video ->
        video.playbackUrls().any { candidateUrl ->
            candidateUrl == videoUrl ||
                (
                    selectedResource != null &&
                        playbackResourceIdentity(candidateUrl) == selectedResource
                    )
        }
    }
    return normalizeCodecFamilyKey(selectedVideo?.codecs)
}

private fun DashVideo.playbackUrls(): List<String> {
    return buildList {
        if (baseUrl.isNotBlank()) {
            add(baseUrl)
        }
        backupUrl.orEmpty()
            .filter { it.isNotBlank() }
            .let(::addAll)
    }
}

private fun playbackResourceIdentity(url: String): String? {
    return runCatching {
        val uri = URI(url)
        val path = uri.rawPath.orEmpty()
        if (path.isBlank()) return@runCatching null
        if (uri.rawQuery.isNullOrBlank()) path else "$path?${uri.rawQuery}"
    }.getOrNull()
}
