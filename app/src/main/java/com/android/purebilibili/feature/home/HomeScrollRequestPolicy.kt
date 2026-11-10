package com.android.purebilibili.feature.home

/** 回顶属于发起时的页面与切换批次，离开后再返回也不重放旧请求。 */
internal data class HomeScrollRequestIdentity(
    val entry: HomeTopTabEntry?,
    val popularSubCategory: PopularSubCategory?,
    val navigationRevision: Long,
)

internal fun shouldApplyHomeScrollRequest(
    requested: HomeScrollRequestIdentity,
    current: HomeScrollRequestIdentity,
    isTopLevelActive: Boolean,
    pagerScrolling: Boolean,
): Boolean = isTopLevelActive && !pagerScrolling &&
    requested.entry != null && requested == current
