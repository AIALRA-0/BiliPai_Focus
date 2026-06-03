package com.android.purebilibili.data.repository

internal fun resolvePlaybackHeartbeatPlayType(
    playedTimeSec: Long,
    realPlayedTimeSec: Long
): Int {
    return if (playedTimeSec <= 0L && realPlayedTimeSec <= 0L) 1 else 0
}

internal fun buildPlaybackHeartbeatFields(
    bvid: String,
    aid: Long,
    cid: Long,
    epid: Long = 0L,
    sid: Long = 0L,
    mid: Long?,
    playedTimeSec: Long,
    realPlayedTimeSec: Long,
    startTsSec: Long,
    csrf: String,
    videoType: Int = 3,
    subType: Int? = null
): Map<String, String> {
    val safePlayedTimeSec = playedTimeSec.coerceAtLeast(0L)
    val safeRealPlayedTimeSec = realPlayedTimeSec.coerceAtLeast(0L)
    return buildMap {
        aid.takeIf { it > 0L }?.let { put("aid", it.toString()) }
        bvid.takeIf { it.isNotBlank() }?.let { put("bvid", it) }
        cid.takeIf { it > 0L }?.let { put("cid", it.toString()) }
        epid.takeIf { it > 0L }?.let { put("epid", it.toString()) }
        sid.takeIf { it > 0L }?.let { put("sid", it.toString()) }
        mid?.takeIf { it > 0L }?.let { put("mid", it.toString()) }
        put("played_time", safePlayedTimeSec.toString())
        put("real_played_time", safeRealPlayedTimeSec.toString())
        put("realtime", safeRealPlayedTimeSec.toString())
        put("start_ts", startTsSec.coerceAtLeast(0L).toString())
        put("type", videoType.toString())
        subType?.takeIf { it > 0 }?.let { put("sub_type", it.toString()) }
        put("dt", "2")
        put("outer", "0")
        put(
            "play_type",
            resolvePlaybackHeartbeatPlayType(
                playedTimeSec = safePlayedTimeSec,
                realPlayedTimeSec = safeRealPlayedTimeSec
            ).toString()
        )
        put("spmid", "333.788.0.0")
        put("from_spmid", "")
        put("refer_url", "")
        csrf.takeIf { it.isNotBlank() }?.let { put("csrf", it) }
    }
}
