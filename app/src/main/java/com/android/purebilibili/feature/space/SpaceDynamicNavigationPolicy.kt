package com.android.purebilibili.feature.space

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.data.model.response.SpaceArticleItem
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction

internal sealed interface SpaceDynamicClickAction {
    data class OpenVideo(val bvid: String) : SpaceDynamicClickAction
    data class OpenArticle(val articleId: Long, val title: String) : SpaceDynamicClickAction
    data class OpenDynamicDetail(val dynamicId: String) : SpaceDynamicClickAction
    data object None : SpaceDynamicClickAction
}

internal fun resolveSpaceDynamicClickAction(dynamic: SpaceDynamicItem): SpaceDynamicClickAction {
    return when (val action = resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))) {
        is DynamicCardPrimaryAction.OpenVideo -> SpaceDynamicClickAction.OpenVideo(action.bvid)
        is DynamicCardPrimaryAction.OpenArticle -> SpaceDynamicClickAction.OpenArticle(action.articleId, action.title)
        is DynamicCardPrimaryAction.OpenDynamicDetail -> SpaceDynamicClickAction.OpenDynamicDetail(action.dynamicId)
        else -> SpaceDynamicClickAction.None
    }
}

internal fun resolveSpaceArticleClickAction(article: SpaceArticleItem): SpaceDynamicClickAction {
    when (val target = BilibiliNavigationTargetParser.parse(article.jump_url)) {
        is BilibiliNavigationTarget.Dynamic -> {
            return SpaceDynamicClickAction.OpenDynamicDetail(target.dynamicId)
        }
        is BilibiliNavigationTarget.Article -> {
            return SpaceDynamicClickAction.OpenArticle(
                articleId = target.articleId,
                title = article.title
            )
        }
        else -> Unit
    }

    val articleId = article.id.takeIf { it > 0L } ?: return SpaceDynamicClickAction.None
    return if (articleId >= OPUS_ID_MIN_VALUE) {
        SpaceDynamicClickAction.OpenDynamicDetail(articleId.toString())
    } else {
        SpaceDynamicClickAction.OpenArticle(articleId, article.title)
    }
}

private const val OPUS_ID_MIN_VALUE = 1_000_000_000_000_000L
