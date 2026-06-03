package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiSection

/**
 * 在通过 ep_id 进入详情时，路由中的 seasonId 可能是 0 或无效值。
 * 交互动作（追番、跳转播放）应优先使用详情接口返回的真实 seasonId。
 */
fun resolveBangumiActionSeasonId(routeSeasonId: Long, detailSeasonId: Long): Long {
    return if (detailSeasonId > 0) detailSeasonId else routeSeasonId
}

fun resolveBangumiDetailMetaChips(detail: BangumiDetail): List<String> {
    val chips = mutableListOf<String>()
    detail.seasonTypeName.takeIf { it.isNotBlank() }?.let(chips::add)
    detail.publish?.pubTimeShow?.takeIf { it.isNotBlank() }?.let(chips::add)
        ?: detail.publish?.pubTime?.takeIf { it.isNotBlank() }?.let(chips::add)
    detail.areas.orEmpty()
        .mapNotNull { it.name.takeIf(String::isNotBlank) }
        .take(2)
        .forEach(chips::add)
    detail.styles.orEmpty()
        .filter { it.isNotBlank() }
        .take(3)
        .forEach(chips::add)
    detail.newEp?.desc?.takeIf { it.isNotBlank() }?.let(chips::add)
    return chips.distinct()
}

fun resolveBangumiRestrictionLabels(detail: BangumiDetail): List<String> {
    val labels = mutableListOf<String>()
    val rights = detail.rights
    if (rights?.isPreview == 1) labels += "试看"
    if (rights?.areaLimit == 1) labels += "地区受限"
    detail.payment?.let { payment ->
        when {
            payment.tip.isNotBlank() -> labels += payment.tip
            payment.price.isNotBlank() -> labels += "付费 ${payment.price}"
            payment.vipPrice.isNotBlank() -> labels += "大会员 ${payment.vipPrice}"
            payment.promotion.isNotBlank() -> labels += payment.promotion
            payment.vipPromotion.isNotBlank() -> labels += payment.vipPromotion
        }
    }
    if (detail.userStatus?.vip == 1) labels += "大会员"
    return labels.filter { it.isNotBlank() }.distinct()
}

fun resolveBangumiSectionTitle(section: BangumiSection, fallbackIndex: Int): String {
    return section.title.takeIf { it.isNotBlank() } ?: "扩展内容 ${fallbackIndex + 1}"
}
