package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.AppSignUtils
import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.Dolby
import com.android.purebilibili.data.model.response.Flac
import com.android.purebilibili.data.model.response.PlayUrlData

internal const val TV_CAST_DEFAULT_QUALITY = 80

internal fun buildTvCastPlayUrlParams(
    aid: Long,
    cid: Long,
    qn: Int,
    accessToken: String?
): MutableMap<String, String> {
    val targetQn = qn.takeIf { it > 0 } ?: TV_CAST_DEFAULT_QUALITY
    val params = mutableMapOf(
        "actionKey" to "appkey",
        "cid" to cid.toString(),
        "fourk" to "1",
        "is_proj" to "1",
        "object_id" to aid.toString(),
        "mobi_app" to "android",
        "platform" to "android",
        "playurl_type" to "1",
        "protocol" to "0",
        "qn" to targetQn.toString(),
        "appkey" to AppSignUtils.TV_APP_KEY,
        "ts" to AppSignUtils.getTimestamp().toString()
    )

    if (!accessToken.isNullOrBlank()) {
        params["access_key"] = accessToken
        params["mobile_access_key"] = accessToken
    }
    return params
}

internal fun extractTvCastPlayableUrl(data: PlayUrlData?): String? {
    if (data == null) return null
    data.durl.orEmpty().forEach { segment ->
        if (segment.url.isNotBlank()) {
            return segment.url
        }
        val backup = segment.backupUrl?.firstOrNull { it.isNotBlank() }
        if (!backup.isNullOrBlank()) {
            return backup
        }
    }
    return null
}

internal fun selectCastDashVideo(videos: List<DashVideo>, targetQuality: Int): DashVideo? {
    val validVideos = videos.filter { it.getValidUrl().isNotBlank() }
    if (validVideos.isEmpty()) return null

    fun codecFamily(codecs: String): Int = when {
        codecs.startsWith("avc1", ignoreCase = true) -> 0
        codecs.startsWith("hev1", ignoreCase = true) || codecs.startsWith("hvc1", ignoreCase = true) -> 1
        codecs.startsWith("av01", ignoreCase = true) -> 2
        else -> 3
    }

    fun bestInQuality(candidates: List<DashVideo>): DashVideo? {
        return candidates.minByOrNull { it.bandwidth.coerceAtLeast(1) }
    }

    fun pickBestInFamily(candidates: List<DashVideo>): DashVideo? {
        val byQuality: Map<Int, List<DashVideo>> = candidates.groupBy { it.id }

        byQuality[targetQuality]?.let { return bestInQuality(it) }

        val lowerIds = byQuality.keys.filter { it <= targetQuality }
        if (lowerIds.isNotEmpty()) {
            return bestInQuality(byQuality[lowerIds.max()]!!)
        }

        val higherIds = byQuality.keys.filter { it > targetQuality }
        if (higherIds.isNotEmpty()) {
            return bestInQuality(byQuality[higherIds.min()]!!)
        }

        return null
    }

    val families: Map<Int, List<DashVideo>> = validVideos.groupBy { codecFamily(it.codecs) }
    for (family in listOf(0, 1, 2, 3)) {
        families[family]?.let { familyVideos ->
            pickBestInFamily(familyVideos)?.let { return it }
        }
    }

    return null
}

internal fun selectCastDashAudio(audios: List<DashAudio>, dolby: Dolby?, flac: Flac?): DashAudio? {
    val aacTracks = audios.filter { it.codecs.startsWith("mp4a", ignoreCase = true) && it.getValidUrl().isNotBlank() }
    if (aacTracks.isEmpty()) return null
    return aacTracks.maxByOrNull { it.bandwidth }
}

internal fun isCastDashManifestAvailable(dash: Dash): Boolean {
    val hasPlayableVideo = dash.video.any { it.getValidUrl().isNotBlank() }
    if (!hasPlayableVideo) return false
    return selectCastDashAudio(dash.audio.orEmpty(), dash.dolby, dash.flac) != null
}
