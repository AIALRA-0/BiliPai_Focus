package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicAdditional

internal data class DynamicAdditionalCardModel(
    val title: String,
    val subtitle: String,
    val cover: String,
    val jumpUrl: String,
    val kindLabel: String,
    val voteId: Long = 0L,
    val actionLabel: String = "",
    val enabled: Boolean = true,
    val reserveId: Long = 0L,
    val reserveTotal: Long = 0L,
    val reserveButtonStatus: Int = 0,
    val reserveButtonType: Int = 0,
    val reserveButtonDisabled: Boolean = false,
    val reserveCheckedLabel: String = "已预约",
    val reserveUncheckedLabel: String = "预约",
    val reserveDescriptionPrefix: String = "",
    val reserveActionJumpUrl: String = "",
    val reserveDescriptionJumpUrl: String = "",
)

data class DynamicReserveAction(
    val dynamicId: String,
    val reserveId: Long,
    val currentButtonStatus: Int,
    val reserveTotal: Long,
)

data class DynamicReserveResult(
    val description: String,
    val reserveTotal: Long,
    val buttonStatus: Int,
)

internal fun resolveDynamicAdditionalCard(additional: DynamicAdditional?): DynamicAdditionalCardModel? {
    if (additional == null) return null
    return when (additional.type) {
        "ADDITIONAL_TYPE_COMMON" -> additional.common?.takeIf { it.title.isNotBlank() }?.let {
            DynamicAdditionalCardModel(
                title = it.title,
                subtitle = listOf(it.desc1, it.desc2).filter(String::isNotBlank).joinToString(" · "),
                cover = it.cover,
                jumpUrl = it.jump_url.ifBlank { it.button?.jump_url.orEmpty() },
                kindLabel = it.head_text.ifBlank { "相关内容" },
                actionLabel = resolveDynamicCardButtonLabel(it.button),
            )
        }
        "ADDITIONAL_TYPE_UGC" -> additional.ugc?.takeIf { it.title.isNotBlank() }?.let {
            DynamicAdditionalCardModel(
                title = it.title,
                subtitle = it.desc_second,
                cover = it.cover,
                jumpUrl = it.jump_url,
                kindLabel = "投稿"
            )
        }
        "ADDITIONAL_TYPE_RESERVE" -> additional.reserve?.takeIf { it.state != -1 && it.title.isNotBlank() }?.let {
            DynamicAdditionalCardModel(
                title = it.title,
                subtitle = listOfNotNull(it.desc1?.text, it.desc2?.text, it.desc3?.text)
                    .filter { text -> text.isNotBlank() }
                    .joinToString("  "),
                cover = "",
                jumpUrl = it.jump_url.ifBlank { it.button?.jump_url.orEmpty() },
                kindLabel = "预约",
                actionLabel = it.button?.let { button ->
                    if (button.jump_url.isNotBlank()) {
                        button.jump_style?.text.orEmpty().ifBlank { "打开" }
                    } else if (button.status == button.type) {
                        button.check?.text.orEmpty().ifBlank { "已预约" }
                    } else {
                        button.uncheck?.text.orEmpty().ifBlank { "预约" }
                    }
                }.orEmpty(),
                reserveId = it.rid,
                reserveTotal = it.reserve_total,
                reserveButtonStatus = it.button?.status ?: 0,
                reserveButtonType = it.button?.type ?: 0,
                reserveButtonDisabled = it.button?.jump_url.isNullOrBlank() &&
                    it.button?.uncheck?.disable == 1,
                reserveCheckedLabel = it.button?.check?.text.orEmpty().ifBlank { "已预约" },
                reserveUncheckedLabel = it.button?.uncheck?.text.orEmpty().ifBlank { "预约" },
                reserveDescriptionPrefix = it.desc1?.text.orEmpty(),
                reserveActionJumpUrl = it.button?.jump_url.orEmpty(),
                reserveDescriptionJumpUrl = it.desc3?.jump_url.orEmpty(),
            )
        }
        "ADDITIONAL_TYPE_GOODS" -> additional.goods?.items?.firstOrNull()?.let { goods ->
            DynamicAdditionalCardModel(
                title = goods.name.ifBlank { additional.goods.head_text.ifBlank { "商品" } },
                subtitle = goods.brief,
                cover = goods.cover,
                jumpUrl = goods.jump_url,
                kindLabel = "商品"
            )
        }
        "ADDITIONAL_TYPE_VOTE" -> additional.vote?.takeIf { it.desc.isNotBlank() || it.vote_id > 0L }?.let {
            DynamicAdditionalCardModel(
                title = it.desc.ifBlank { "投票" },
                subtitle = if (it.join_num > 0) "${it.join_num} 人参与" else "投票",
                cover = "",
                jumpUrl = "",
                kindLabel = "投票",
                voteId = it.vote_id
            )
        }
        "ADDITIONAL_TYPE_MATCH" -> additional.match?.takeIf { it.title.isNotBlank() }?.let {
            DynamicAdditionalCardModel(
                title = it.title,
                subtitle = it.sub_title,
                cover = "",
                jumpUrl = it.jump_url,
                kindLabel = "赛事"
            )
        }
        "ADDITIONAL_TYPE_UPOWER_LOTTERY" -> additional.upower_lottery
            ?.takeIf { it.title.isNotBlank() }
            ?.let {
                DynamicAdditionalCardModel(
                    title = it.title,
                    subtitle = listOfNotNull(it.desc?.text, it.hint?.text)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    cover = "",
                    jumpUrl = it.jump_url.ifBlank { it.button?.jump_url.orEmpty() },
                    kindLabel = "充电专属抽奖",
                    actionLabel = resolveDynamicCardButtonLabel(it.button),
                )
            }
        else -> null
    }
}
