package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem

internal data class HomeNotInterestedAction(
    val bvid: String,
    val shouldBlockCreator: Boolean,
    val shouldSyncCreatorToBilibiliBlockedList: Boolean,
    val creatorMid: Long,
    val creatorName: String,
    val creatorFace: String
)

internal data class HomeNotInterestedVisualTransition(
    val shouldStartDissolve: Boolean,
    val shouldRemoveImmediately: Boolean
)

internal fun resolveHomeNotInterestedVisualTransition(
    isFeedbackRecorded: Boolean,
    isDissolveAnimationAvailable: Boolean
): HomeNotInterestedVisualTransition {
    return HomeNotInterestedVisualTransition(
        shouldStartDissolve = isFeedbackRecorded && isDissolveAnimationAvailable,
        shouldRemoveImmediately = isFeedbackRecorded && !isDissolveAnimationAvailable
    )
}

internal fun resolveHomeDismissVisualTransition(
    isFeedbackRecorded: Boolean,
    cardAnimationEnabled: Boolean
): HomeNotInterestedVisualTransition {
    return resolveHomeNotInterestedVisualTransition(
        isFeedbackRecorded = isFeedbackRecorded,
        isDissolveAnimationAvailable = cardAnimationEnabled
    )
}

internal fun resolveHomeNotInterestedAction(video: VideoItem): HomeNotInterestedAction {
    val creatorMid = video.owner.mid
    return HomeNotInterestedAction(
        bvid = video.bvid,
        shouldBlockCreator = creatorMid > 0L,
        shouldSyncCreatorToBilibiliBlockedList = creatorMid > 0L,
        creatorMid = creatorMid,
        creatorName = video.owner.name.ifBlank {
            if (creatorMid > 0L) "UP主$creatorMid" else ""
        },
        creatorFace = video.owner.face
    )
}

internal fun filterHomeVideosByNotInterestedFeedback(
    videos: List<VideoItem>,
    dislikedBvids: Set<String> = emptySet(),
    dislikedCreatorMids: Set<Long> = emptySet(),
    dislikedKeywords: Set<String> = emptySet()
): List<VideoItem> {
    if (videos.isEmpty()) return videos
    val normalizedBvids = dislikedBvids
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()
    val normalizedCreators = dislikedCreatorMids.filter { it > 0L }.toSet()
    val normalizedKeywords = dislikedKeywords
        .map { it.trim().lowercase() }
        .filter { it.length >= 2 }
        .toSet()

    if (normalizedBvids.isEmpty() && normalizedCreators.isEmpty() && normalizedKeywords.isEmpty()) {
        return videos
    }

    return videos.filter { video ->
        video.bvid !in normalizedBvids &&
            video.owner.mid !in normalizedCreators &&
            !shouldFilterByDislikedKeywords(video.title, normalizedKeywords)
    }
}

private fun shouldFilterByDislikedKeywords(
    title: String,
    dislikedKeywords: Set<String>
): Boolean {
    if (title.isBlank() || dislikedKeywords.isEmpty()) return false
    val normalizedTitle = title.lowercase()
    val hitKeywords = dislikedKeywords.filter { keyword ->
        normalizedTitle.contains(keyword)
    }
    if (hitKeywords.isEmpty()) return false

    // 单个短词容易误伤，例如“日常”；长短语或多个命中才认为是同类型内容。
    return hitKeywords.any { it.length >= 4 } || hitKeywords.size >= 2
}
