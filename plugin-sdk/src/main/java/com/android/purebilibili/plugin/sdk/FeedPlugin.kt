package com.android.purebilibili.plugin.sdk

/** Filters feed items before any registered transforms run. */
interface FeedPlugin<in T> : Plugin {
    fun shouldShowItem(item: T, feedKind: FeedKind = FeedKind.GENERIC): Boolean
}

/** Reorders or decorates feed items after all filters have run. */
interface FeedTransformPlugin<T> : Plugin {
    fun transformFeedItems(items: List<T>, feedKind: FeedKind): List<T>
}

/** Origin of a feed item, allowing source-specific filtering rules. */
enum class FeedKind {
    GENERIC,
    HOME_RECOMMEND,
    HOME_POPULAR,
    HOME_RANK,
    HOME_REGION,
    SEARCH
}
