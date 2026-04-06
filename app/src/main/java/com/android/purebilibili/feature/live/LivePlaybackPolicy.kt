package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.CodecInfo
import com.android.purebilibili.data.model.response.LivePlayUrlData
import com.android.purebilibili.data.model.response.LiveQuality

internal data class LivePlaybackCandidate(
    val protocolName: String,
    val formatName: String,
    val codecName: String,
    val urls: List<String>
)

internal data class ResolvedLivePlayback(
    val requestedQuality: Int,
    val currentQuality: Int,
    val qualityList: List<LiveQuality>,
    val candidates: List<LivePlaybackCandidate>
) {
    val primaryUrl: String?
        get() = candidates.firstOrNull()?.urls?.firstOrNull()
}

internal sealed interface LiveAdvanceResult {
    data class NextSource(
        val candidateIndex: Int,
        val urlIndex: Int,
        val playUrl: String
    ) : LiveAdvanceResult

    data class ReloadCurrentQuality(
        val qualityQn: Int
    ) : LiveAdvanceResult
}

internal fun resolveLivePlayback(
    data: LivePlayUrlData,
    requestedQn: Int
): ResolvedLivePlayback? {
    val candidates = data.playurl_info?.playurl?.stream
        .orEmpty()
        .sortedBy { streamProtocolPriority(it.protocolName) }
        .flatMap { stream ->
            stream.format.orEmpty().flatMap { format ->
                format.codec.orEmpty().mapNotNull { codec ->
                    val urls = codec.url_info
                        .orEmpty()
                        .mapNotNull { urlInfo ->
                            val host = urlInfo.host.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val baseUrl = codec.baseUrl.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val extra = urlInfo.extra.orEmpty()
                            host + baseUrl + extra
                        }
                        .distinct()
                    if (urls.isEmpty()) {
                        null
                    } else {
                        LivePlaybackCandidate(
                            protocolName = stream.protocolName,
                            formatName = format.formatName,
                            codecName = codec.codecName,
                            urls = urls
                        )
                    }
                }
            }
        }

    if (candidates.isEmpty()) {
        return null
    }

    val codecs = data.playurl_info?.playurl?.stream
        .orEmpty()
        .flatMap { it.format.orEmpty() }
        .flatMap { it.codec.orEmpty() }
    val preferredCodec = codecs.firstOrNull { it.currentQn > 0 || !it.acceptQn.isNullOrEmpty() }
    val qualityList = resolveLiveQualityList(
        codec = preferredCodec,
        data = data
    )
    val currentQuality = preferredCodec?.currentQn
        ?.takeIf { it > 0 }
        ?: data.current_quality.takeIf { it > 0 }
        ?: qualityList.firstOrNull()?.qn
        ?: requestedQn

    return ResolvedLivePlayback(
        requestedQuality = requestedQn,
        currentQuality = currentQuality,
        qualityList = qualityList,
        candidates = candidates
    )
}

internal fun advanceLivePlayback(
    resolved: ResolvedLivePlayback,
    candidateIndex: Int,
    urlIndex: Int
): LiveAdvanceResult {
    val candidate = resolved.candidates.getOrNull(candidateIndex)
        ?: return LiveAdvanceResult.ReloadCurrentQuality(resolved.requestedQuality)

    val nextUrlIndex = urlIndex + 1
    val nextUrl = candidate.urls.getOrNull(nextUrlIndex)
    if (nextUrl != null) {
        return LiveAdvanceResult.NextSource(
            candidateIndex = candidateIndex,
            urlIndex = nextUrlIndex,
            playUrl = nextUrl
        )
    }

    val nextCandidateIndex = candidateIndex + 1
    val nextCandidate = resolved.candidates.getOrNull(nextCandidateIndex)
    if (nextCandidate != null) {
        return LiveAdvanceResult.NextSource(
            candidateIndex = nextCandidateIndex,
            urlIndex = 0,
            playUrl = nextCandidate.urls.first()
        )
    }

    return LiveAdvanceResult.ReloadCurrentQuality(resolved.requestedQuality)
}

private fun resolveLiveQualityList(
    codec: CodecInfo?,
    data: LivePlayUrlData
): List<LiveQuality> {
    val descriptions = linkedMapOf<Int, String>()
    data.playurl_info?.playurl?.gQnDesc.orEmpty().forEach { quality ->
        if (quality.qn > 0 && quality.desc.isNotBlank()) {
            descriptions[quality.qn] = quality.desc
        }
    }
    data.quality_description.orEmpty().forEach { quality ->
        if (quality.qn > 0 && quality.desc.isNotBlank() && quality.qn !in descriptions) {
            descriptions[quality.qn] = quality.desc
        }
    }

    val acceptQn = codec?.acceptQn.orEmpty().filter { it > 0 }
    if (acceptQn.isNotEmpty()) {
        return acceptQn.distinct().map { qn ->
            LiveQuality(
                qn = qn,
                desc = descriptions[qn] ?: qn.toString()
            )
        }
    }

    val combined = data.playurl_info?.playurl?.gQnDesc.orEmpty() + data.quality_description.orEmpty()
    return combined
        .filter { it.qn > 0 }
        .distinctBy { it.qn }
}

private fun streamProtocolPriority(protocolName: String): Int {
    return when (protocolName) {
        "http_hls" -> 0
        "http_stream" -> 1
        else -> 2
    }
}
