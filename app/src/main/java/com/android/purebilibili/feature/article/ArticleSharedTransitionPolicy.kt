package com.android.purebilibili.feature.article

import com.android.purebilibili.core.ui.transition.BiliPaiSharedElementKey
import com.android.purebilibili.core.ui.transition.articleCoverSharedElementKey

internal enum class ArticleSharedElementSlot {
    CARD,
    COVER,
    TITLE
}

internal fun resolveHistoryArticleCoverAspectRatio(): Float = 16f / 10f

internal fun shouldEnableArticleSharedReturn(
    firstVisibleItemIndex: Int
): Boolean = firstVisibleItemIndex == 0

internal fun resolveArticleSharedTransitionKey(
    articleId: Long,
    slot: ArticleSharedElementSlot
): BiliPaiSharedElementKey {
    val normalizedId = articleId.coerceAtLeast(0L)
    return when (slot) {
        ArticleSharedElementSlot.CARD -> BiliPaiSharedElementKey.Raw("article_card", normalizedId.toString())
        ArticleSharedElementSlot.COVER -> articleCoverSharedElementKey(normalizedId)
        ArticleSharedElementSlot.TITLE -> BiliPaiSharedElementKey.Raw("article_title", normalizedId.toString())
    }
}

internal fun shouldUseArticleNoOpRouteTransition(
    cardTransitionEnabled: Boolean,
    sharedTransitionReady: Boolean
): Boolean {
    return cardTransitionEnabled &&
        sharedTransitionReady
}
