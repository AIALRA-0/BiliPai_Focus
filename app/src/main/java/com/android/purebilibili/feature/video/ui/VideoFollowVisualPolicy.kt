package com.android.purebilibili.feature.video.ui

internal enum class FollowButtonTone {
    PRIMARY,
    PRIMARY_CONTAINER
}

internal enum class FollowTextTone {
    ON_PRIMARY,
    ON_PRIMARY_CONTAINER
}

internal enum class FollowBadgeTone {
    PRIMARY
}

internal data class VideoFollowVisualPolicy(
    val detailButtonTone: FollowButtonTone,
    val detailTextTone: FollowTextTone,
    val relatedBadgeTone: FollowBadgeTone?
)

internal fun resolveVideoFollowVisualPolicy(
    isFollowing: Boolean,
    darkTheme: Boolean,
): VideoFollowVisualPolicy {
    return if (isFollowing) {
        VideoFollowVisualPolicy(
            detailButtonTone = FollowButtonTone.PRIMARY_CONTAINER,
            detailTextTone = FollowTextTone.ON_PRIMARY_CONTAINER,
            relatedBadgeTone = FollowBadgeTone.PRIMARY
        )
    } else {
        // 未关注主按钮:深色主题用 primary(强调),浅色主题用 primaryContainer
        // 避免亮主题下 primary 种子色偏深导致按钮过重。
        if (darkTheme) {
            VideoFollowVisualPolicy(
                detailButtonTone = FollowButtonTone.PRIMARY,
                detailTextTone = FollowTextTone.ON_PRIMARY,
                relatedBadgeTone = null
            )
        } else {
            VideoFollowVisualPolicy(
                detailButtonTone = FollowButtonTone.PRIMARY_CONTAINER,
                detailTextTone = FollowTextTone.ON_PRIMARY_CONTAINER,
                relatedBadgeTone = FollowBadgeTone.PRIMARY
            )
        }
    }
}
