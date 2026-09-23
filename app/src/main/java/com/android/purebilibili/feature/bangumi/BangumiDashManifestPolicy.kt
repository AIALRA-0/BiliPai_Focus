package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.feature.video.playback.dash.buildLocalDashManifest

/**
 * Builds the small on-device MPD used for web/PUGV DASH URLs.
 *
 * PUGV audio representations do not always include a segment_base even though the
 * URL is a valid fMP4 representation. Media3 can treat such a representation as a
 * single progressive DASH segment, so dropping the whole MPD would make playback
 * fall back to merging two standalone m4s sources and often produce a black frame.
 */
internal fun buildBangumiDashManifest(
    dash: Dash,
    video: DashVideo?,
    videoUrl: String?,
    audio: DashAudio?,
    audioUrl: String?,
    durationMs: Long
): String? {
    val resolvedVideoUrl = videoUrl?.takeIf { it.isNotBlank() } ?: return null
    val manifestVideo = video?.copy(
        baseUrl = resolvedVideoUrl,
        backupUrl = emptyList()
    ) ?: return null

    // Keep the audio representation even when segment_base is absent. The DASH
    // parser supplies a single-segment base in that case and reads the fMP4 URL
    // through the same source/headers as the video representation.
    val manifestAudio = if (!audioUrl.isNullOrBlank() && audio != null) {
        listOf(audio.copy(baseUrl = audioUrl, backupUrl = emptyList()))
    } else {
        emptyList()
    }

    return buildLocalDashManifest(
        durationMs = durationMs.coerceAtLeast(0L),
        minBufferTimeMs = (dash.minBufferTime * 1000f).toLong().coerceAtLeast(1_500L),
        videoTracks = listOf(manifestVideo),
        audioTracks = manifestAudio
    )
}
