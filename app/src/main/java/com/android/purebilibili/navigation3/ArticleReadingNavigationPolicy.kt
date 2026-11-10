package com.android.purebilibili.navigation3

internal fun continueArticleReading(
    stack: List<BiliPaiNavKey>,
    currentArticleId: Long,
    articleId: Long,
    title: String,
): List<BiliPaiNavKey> {
    val current = stack.lastOrNull() as? BiliPaiNavKey.ArticleDetail ?: return stack
    if (current.articleId != currentArticleId || articleId <= 0L || articleId == currentArticleId) return stack
    return pushBiliPaiNavKey(stack.dropLast(1), BiliPaiNavKey.ArticleDetail(articleId, title))
}

internal fun openReadingArticleCollection(
    stack: List<BiliPaiNavKey>,
    collectionId: Long,
    title: String,
    currentArticleId: Long,
): List<BiliPaiNavKey> {
    if (collectionId <= 0L) return stack
    val existing = stack.indexOfLast { it is BiliPaiNavKey.ArticleCollection && it.collectionId == collectionId }
    // 复用原文集条目，保留该条目的列表位置与页面状态。
    if (existing >= 0) return stack.take(existing + 1)
    return pushBiliPaiNavKey(stack, BiliPaiNavKey.ArticleCollection(collectionId, title, currentArticleId))
}

internal fun openArticleFromCollection(
    stack: List<BiliPaiNavKey>,
    collectionId: Long,
    articleId: Long,
    title: String,
): List<BiliPaiNavKey> {
    val collection = stack.lastOrNull() as? BiliPaiNavKey.ArticleCollection ?: return stack
    if (collection.collectionId != collectionId || articleId <= 0L) return stack
    val previous = stack.dropLast(1).lastOrNull() as? BiliPaiNavKey.ArticleDetail
    // 仅在真实存在的上一页就是目标文章时返回，不能只根据文集入口的阅读标记判断。
    if (previous?.articleId == articleId) return stack.dropLast(1)
    return pushBiliPaiNavKey(stack, BiliPaiNavKey.ArticleDetail(articleId, title))
}
