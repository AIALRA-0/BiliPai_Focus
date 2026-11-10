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

/**
 * 只有实际可见的直播页消费返回。推荐隐藏时回到首个可见非直播页；
 * 若只保留了直播页，则交还上层返回，不强行重新显示用户隐藏的推荐页。
 */
internal fun resolveHomeLiveTabBackTargetPage(
    entries: List<HomeTopTabEntry>,
    currentPage: Int,
    isTopLevelActive: Boolean,
): Int? {
    if (!isTopLevelActive) return null
    val currentEntry = entries.getOrNull(currentPage) as? HomeTopTabEntry.Category ?: return null
    if (!shouldEmbedLivePageInHomeTopTab(currentEntry.category)) return null

    val recommendIndex = entries.indexOfFirst {
        it is HomeTopTabEntry.Category && it.category == HomeCategory.RECOMMEND
    }
    if (recommendIndex >= 0) return recommendIndex
    return entries.indexOfFirst { it != currentEntry }.takeIf { it >= 0 }
}

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
