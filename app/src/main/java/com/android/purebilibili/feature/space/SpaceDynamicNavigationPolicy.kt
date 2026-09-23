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
    data class OpenUser(val mid: Long) : SpaceDynamicClickAction
    data object None : SpaceDynamicClickAction
}

/**
 * Prefer the forward post itself when opening from the outer card; nested [orig]
 * media still resolves via DynamicCard's own handlers.
 */
internal fun resolveSpaceDynamicClickAction(dynamic: SpaceDynamicItem): SpaceDynamicClickAction {
    val mapped = resolveSpaceDynamicCardItem(dynamic)
    val action = resolveDynamicCardPrimaryAction(mapped)
    return when (action) {
        is DynamicCardPrimaryAction.OpenVideo -> SpaceDynamicClickAction.OpenVideo(action.bvid)
        is DynamicCardPrimaryAction.OpenArticle ->
            SpaceDynamicClickAction.OpenArticle(action.articleId, action.title)
        is DynamicCardPrimaryAction.OpenDynamicDetail -> {
            // Primary action for FORWARD often targets orig.id; for space list entry keep
            // the outer dynamic id so the repost itself opens when requested.
            val outerId = dynamic.id_str.trim()
            if (
                dynamic.type.equals("DYNAMIC_TYPE_FORWARD", ignoreCase = true) &&
                outerId.isNotEmpty()
            ) {
                SpaceDynamicClickAction.OpenDynamicDetail(outerId)
            } else {
                SpaceDynamicClickAction.OpenDynamicDetail(action.dynamicId)
            }
        }
        is DynamicCardPrimaryAction.OpenUser ->
            SpaceDynamicClickAction.OpenUser(action.mid).takeIf { action.mid > 0L }
                ?: SpaceDynamicClickAction.None
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
