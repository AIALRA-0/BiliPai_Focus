package com.android.purebilibili.feature.home

/**
 * 首页顶栏「直播 / 追番」作为首页 Pager 里的独立页停留，
 * 不再切走底栏或打开二级导航。底栏直播入口仍走 LiveList 全屏页。
 */
fun shouldOpenLiveListFromHomeTopTab(category: HomeCategory): Boolean = false

fun shouldOpenBangumiFromHomeTopTab(category: HomeCategory): Boolean = false

fun shouldEmbedLivePageInHomeTopTab(category: HomeCategory): Boolean =
    category == HomeCategory.LIVE

fun shouldEmbedBangumiPageInHomeTopTab(category: HomeCategory): Boolean =
    category == HomeCategory.ANIME

enum class HomeTopTabScrollTarget {
    FEED,
    LIVE,
    BANGUMI,
    PARTITION,
    SUBSCRIPTION,
}

fun resolveHomeTopTabScrollTarget(entry: HomeTopTabEntry?): HomeTopTabScrollTarget {
    return when (entry) {
        HomeTopTabEntry.Partition -> HomeTopTabScrollTarget.PARTITION
        HomeTopTabEntry.Subscriptions -> HomeTopTabScrollTarget.SUBSCRIPTION
        is HomeTopTabEntry.Category -> when {
            shouldEmbedLivePageInHomeTopTab(entry.category) -> HomeTopTabScrollTarget.LIVE
            shouldEmbedBangumiPageInHomeTopTab(entry.category) -> HomeTopTabScrollTarget.BANGUMI
            else -> HomeTopTabScrollTarget.FEED
        }
        null -> HomeTopTabScrollTarget.FEED
    }
}
