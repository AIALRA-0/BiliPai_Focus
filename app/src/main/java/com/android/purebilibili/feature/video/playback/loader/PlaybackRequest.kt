package com.android.purebilibili.feature.video.playback.loader

internal data class PlaybackRequest(
    val bvid: String,
    val aid: Long = 0L,
    val cid: Long = 0L,
    val force: Boolean = false,
    val autoPlay: Boolean? = null,
    val ignoreSavedProgress: Boolean = false,
    val audioLang: String? = null,
    val videoCodecOverride: String? = null
) {
    fun matchesInFlight(other: PlaybackRequest, resolvedCid: Long): Boolean =
        !other.force && bvid == other.bvid && (bvid.isNotBlank() || aid == other.aid) &&
            (cid == other.cid || other.cid <= 0L || (cid <= 0L && resolvedCid > 0L && resolvedCid == other.cid)) &&
            audioLang == other.audioLang && videoCodecOverride == other.videoCodecOverride &&
            ignoreSavedProgress == other.ignoreSavedProgress


    fun resolveProgressCid(
        currentBvid: String,
        currentCid: Long,
        uiBvid: String?,
        uiCid: Long
    ): Long {
        return when {
            ignoreSavedProgress -> 0L
            cid > 0L -> cid
            currentBvid == bvid && currentCid > 0L -> currentCid
            uiBvid == bvid && uiCid > 0L -> uiCid
            else -> 0L
        }
    }

    companion object {
        fun create(
            bvid: String,
            aid: Long = 0L,
            cid: Long = 0L,
            force: Boolean = false,
            autoPlay: Boolean? = null,
            ignoreSavedProgress: Boolean = false,
            audioLang: String? = null,
            videoCodecOverride: String? = null
        ): PlaybackRequest {
            return PlaybackRequest(
                bvid = bvid,
                aid = aid,
                cid = cid,
                force = force,
                autoPlay = autoPlay,
                ignoreSavedProgress = ignoreSavedProgress,
                audioLang = audioLang?.trim()?.takeIf { it.isNotEmpty() },
                videoCodecOverride = videoCodecOverride?.trim()?.takeIf { it.isNotEmpty() }
            )
        }
    }
}
