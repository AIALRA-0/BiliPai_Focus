package com.android.purebilibili.feature.search

internal sealed interface SearchResultNavigationTarget {
    data class Video(val bvid: String) : SearchResultNavigationTarget

    data class Course(
        val seasonId: Long,
        val epId: Long = 0L
    ) : SearchResultNavigationTarget

    data class Web(
        val url: String,
        val title: String
    ) : SearchResultNavigationTarget

    data class LiveRoom(
        val roomId: Long,
        val title: String,
        val uname: String
    ) : SearchResultNavigationTarget

    data class Space(val mid: Long) : SearchResultNavigationTarget

    data object None : SearchResultNavigationTarget
}

internal fun parseCourseSearchNavigationTarget(url: String): SearchResultNavigationTarget.Course? {
    val ssMatch = Regex("""(?:ss|season/|season_id=)(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull()
    val epMatch = Regex("""(?:ep|ep/|ep_id=)(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull()
    if (ssMatch != null && ssMatch > 0L) {
        return SearchResultNavigationTarget.Course(seasonId = ssMatch, epId = epMatch ?: 0L)
    }
    if (epMatch != null && epMatch > 0L) {
        return SearchResultNavigationTarget.Course(seasonId = 0L, epId = epMatch)
    }
    return null
}

internal fun resolveVideoSearchNavigationTarget(
    bvid: String,
    contentType: String,
    navigationUrl: String,
    title: String,
    videoId: Long = 0L,
    ownerName: String = ""
): SearchResultNavigationTarget {
    val normalizedBvid = bvid.trim()
    if (normalizedBvid.isNotEmpty()) {
        return SearchResultNavigationTarget.Video(normalizedBvid)
    }

    // 综合搜索会把直播间混进 video 分类：contentType = "live_room"、bvid 为空、
    // id 即房间号。不识别的话会落到 None，表现为卡片点了完全没反应
    // （UP 主名是独立热区，所以进空间是通的）。
    if (contentType.equals("live_room", ignoreCase = true) && videoId > 0L) {
        return SearchResultNavigationTarget.LiveRoom(
            roomId = videoId,
            title = title.ifBlank { ownerName },
            uname = ownerName
        )
    }

    val normalizedUrl = navigationUrl.trim()
    val isSupportedWebUrl = normalizedUrl.startsWith("https://") ||
        normalizedUrl.startsWith("http://")
    if (contentType.equals("ketang", ignoreCase = true)) {
        val courseTarget = parseCourseSearchNavigationTarget(normalizedUrl)
        if (courseTarget != null) {
            return courseTarget
        }
        if (isSupportedWebUrl) {
            return SearchResultNavigationTarget.Web(
                url = normalizedUrl,
                title = title.trim().ifBlank { "课堂" }
            )
        }
    }
    return SearchResultNavigationTarget.None
}

internal fun resolveLiveUserSearchNavigationTarget(
    roomId: Long,
    uid: Long,
    isLive: Boolean,
    title: String,
    uname: String
): SearchResultNavigationTarget {
    return if (isLive && roomId > 0L) {
        SearchResultNavigationTarget.LiveRoom(
            roomId = roomId,
            title = title.ifBlank { uname },
            uname = uname
        )
    } else if (uid > 0L) {
        SearchResultNavigationTarget.Space(mid = uid)
    } else {
        SearchResultNavigationTarget.None
    }
}

internal fun resolvePhotoSearchNavigationTarget(): SearchResultNavigationTarget {
    return SearchResultNavigationTarget.None
}
