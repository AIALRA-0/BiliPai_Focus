package com.android.purebilibili.feature.search

internal sealed interface SearchResultNavigationTarget {
    data class LiveRoom(
        val roomId: Long,
        val title: String,
        val uname: String
    ) : SearchResultNavigationTarget

    data class Space(val mid: Long) : SearchResultNavigationTarget

    data object None : SearchResultNavigationTarget
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
