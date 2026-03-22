package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

internal fun filterHomeFollowVideosByFocusFollowGroups(
    videos: List<VideoItem>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): List<VideoItem> {
    if (!filterEnabled) return videos
    return videos.filter { video ->
        isFocusFollowUserVisible(config, video.owner.mid)
    }
}

internal fun resolveHomeFollowEmptyMessage(
    visibleVideoCount: Int
): String? {
    if (visibleVideoCount > 0) return null
    return "没有可用关注对象"
}
