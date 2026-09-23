package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.RecommendationFeedbackLocalAction
import com.android.purebilibili.data.model.response.RecommendationFeedbackReason
import com.android.purebilibili.data.model.response.RecommendationFeedbackType
import com.android.purebilibili.data.model.response.RelatedVideo

/**
 * Official-style related-video ⋮ sheet sections (watch later + feedback chips + dislike chips).
 */
internal data class RelatedVideoActionSheetModel(
    val feedbackReasons: List<RecommendationFeedbackReason>,
    val dislikeReasons: List<RecommendationFeedbackReason>
)

internal fun resolveRelatedVideoActionSheetModel(
    video: RelatedVideo
): RelatedVideoActionSheetModel {
    val ownerLabel = video.owner.name.ifBlank {
        if (video.owner.mid > 0L) "UP主${video.owner.mid}" else "UP主"
    }
    return RelatedVideoActionSheetModel(
        feedbackReasons = listOf(
            RecommendationFeedbackReason(
                name = "恐怖血腥",
                toast = "将优化此类推荐",
                type = RecommendationFeedbackType.FEEDBACK,
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            ),
            RecommendationFeedbackReason(
                name = "色情低俗",
                toast = "将优化此类推荐",
                type = RecommendationFeedbackType.FEEDBACK,
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            ),
            RecommendationFeedbackReason(
                name = "封面恶心",
                toast = "将优化此类推荐",
                type = RecommendationFeedbackType.FEEDBACK,
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            ),
            RecommendationFeedbackReason(
                name = "标题党/封面党",
                toast = "将优化此类推荐",
                type = RecommendationFeedbackType.FEEDBACK,
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            )
        ),
        dislikeReasons = listOf(
            RecommendationFeedbackReason(
                name = "UP主：$ownerLabel",
                toast = "将减少该UP主的推荐",
                type = RecommendationFeedbackType.DISLIKE,
                localAction = RecommendationFeedbackLocalAction.CREATOR
            ),
            RecommendationFeedbackReason(
                name = "和当前视频无关",
                toast = "将减少相似推荐",
                type = RecommendationFeedbackType.DISLIKE,
                localAction = RecommendationFeedbackLocalAction.SIMILAR_CONTENT
            ),
            RecommendationFeedbackReason(
                name = "不感兴趣",
                toast = "将减少此类推荐",
                type = RecommendationFeedbackType.DISLIKE,
                localAction = RecommendationFeedbackLocalAction.VIDEO_ONLY
            )
        )
    )
}

internal fun shouldRemoveRelatedVideoAfterFeedback(
    reason: RecommendationFeedbackReason
): Boolean {
    // Feedback chips optimize ranking; dislike chips hide the card immediately.
    return reason.type == RecommendationFeedbackType.DISLIKE
}

internal fun resolveRelatedFeedbackToast(reason: RecommendationFeedbackReason): String {
    return reason.toast.ifBlank {
        when (reason.type) {
            RecommendationFeedbackType.FEEDBACK -> "将优化此类推荐"
            RecommendationFeedbackType.DISLIKE -> "将减少相似推荐"
        }
    }
}

internal fun filterRelatedVideosByHiddenBvids(
    videos: List<RelatedVideo>,
    hiddenBvids: Set<String>
): List<RelatedVideo> {
    if (hiddenBvids.isEmpty()) return videos
    val normalized = hiddenBvids.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    if (normalized.isEmpty()) return videos
    return videos.filter { it.bvid.trim() !in normalized }
}

internal fun extractRelatedFeedbackKeywords(title: String): Set<String> {
    if (title.isBlank()) return emptySet()
    val normalized = title.lowercase()
    val stopWords = setOf("视频", "合集", "最新", "一个", "我们", "你们", "今天", "真的", "这个")
    val zhTokens = Regex("[\\u4e00-\\u9fa5]{2,6}")
        .findAll(normalized)
        .map { it.value }
        .filter { it !in stopWords }
        .take(6)
        .toList()
    val enTokens = Regex("[a-z0-9]{3,}")
        .findAll(normalized)
        .map { it.value }
        .take(4)
        .toList()
    return (zhTokens + enTokens).toSet()
}
