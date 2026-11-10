package com.android.purebilibili.feature.article

import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.feature.dynamic.components.LINK_CARD_TYPE_RESERVE
import com.android.purebilibili.feature.dynamic.components.resolveReserveActionEnabled

internal data class ArticleUnavailableTarget(val label: String, val url: String)

/** 失效卡片不提供内容类型；保留内容号，由用户明确选择入口。 */
internal fun resolveArticleUnavailableTargets(contentId: String): List<ArticleUnavailableTarget> {
    val id = contentId.trim()
    if (Regex("BV[a-zA-Z0-9]{10}").matches(id)) {
        return listOf(ArticleUnavailableTarget("视频", "https://www.bilibili.com/video/$id"))
    }
    fun positiveNumber(value: String): Long? = value.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?.toLongOrNull()?.takeIf { it > 0L }
    val prefix = id.take(2).lowercase()
    if (prefix == "av" || prefix == "cv") {
        val number = positiveNumber(id.drop(2)) ?: return emptyList()
        return if (prefix == "av") listOf(ArticleUnavailableTarget("视频", "https://www.bilibili.com/video/av$number"))
        else listOf(ArticleUnavailableTarget("专栏", "https://www.bilibili.com/read/cv$number"))
    }
    val number = positiveNumber(id) ?: return emptyList()
    return listOf(
        ArticleUnavailableTarget("视频", "https://www.bilibili.com/video/av$number"),
        ArticleUnavailableTarget("动态 / 图文", "https://www.bilibili.com/opus/$number"),
        ArticleUnavailableTarget("专栏", "https://www.bilibili.com/read/cv$number"),
    )
}

internal fun resolveArticleUnavailableContentId(raw: String): String? = raw.trim().takeUnless {
    it.isEmpty() || it == "0" || it.equals("undefined", true) || it.equals("null", true)
}

internal fun resolveArticleReserveNotice(card: OpusLinkCard, dynamicId: String): String? {
    if (card.type != LINK_CARD_TYPE_RESERVE) return null
    val reserve = card.reserve ?: return "预约信息暂不可用"
    if (reserve.state == -1) return "预约已结束"
    if (reserve.rid <= 0L) return "预约信息暂不可用"
    val button = reserve.button ?: return "预约信息暂不可用"
    if (button.jump_url.isNotBlank()) return null
    if (!resolveReserveActionEnabled(card)) {
        val style = if (button.status == button.type) button.check ?: button.uncheck else button.uncheck
        return style?.toast?.takeIf(String::isNotBlank) ?: "当前预约不可操作"
    }
    return if (dynamicId.isBlank()) "当前专栏暂不支持直接预约" else null
}
