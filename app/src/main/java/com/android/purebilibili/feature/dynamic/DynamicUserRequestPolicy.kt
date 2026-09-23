package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem

internal enum class DynamicUserContentFilter(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    ARTICLE("图文专栏"),
}

internal fun filterSelectedUserDynamicItems(
    items: List<DynamicItem>,
    filter: DynamicUserContentFilter,
): List<DynamicItem> = when (filter) {
    DynamicUserContentFilter.ALL -> items
    DynamicUserContentFilter.VIDEO -> items.filter(::shouldIncludeDynamicItemInVideoTab)
    DynamicUserContentFilter.ARTICLE -> items.filter(::shouldIncludeDynamicItemInArticleTab)
}

internal fun shouldAutoLoadMoreForUserContentFilter(
    isSelectedUserFeed: Boolean,
    filter: DynamicUserContentFilter,
    visibleItemCount: Int,
): Boolean {
    if (!isSelectedUserFeed || filter == DynamicUserContentFilter.ALL) return true
    return visibleItemCount > 0
}

internal fun DynamicPagePresentation.withUserContentFilter(
    filter: DynamicUserContentFilter,
): DynamicPagePresentation {
    if (!isSelectedUserFeed || filter == DynamicUserContentFilter.ALL) return this
    return copy(items = filterSelectedUserDynamicItems(items, filter))
}

internal fun shouldApplyUserDynamicsResult(
    selectedUid: Long?,
    requestUid: Long,
    activeRequestToken: Long,
    requestToken: Long
): Boolean {
    return selectedUid == requestUid && activeRequestToken == requestToken
}

internal fun resolveSelectedUserVisibleItems(
    timelineItems: List<DynamicItem>,
    remoteUserItems: List<DynamicItem>,
    selectedUid: Long?
): List<DynamicItem> {
    if (selectedUid == null) return timelineItems

    val localMatches = timelineItems.filter { item ->
        item.modules.module_author?.mid == selectedUid
    }
    if (remoteUserItems.isEmpty()) {
        return localMatches
    }
    return appendDistinctByKey(
        existing = localMatches,
        incoming = remoteUserItems,
        keySelector = ::dynamicFeedItemKey
    )
}

internal fun shouldAutoLoadSelectedUserDynamics(
    previousUid: Long?,
    nextUid: Long,
    currentItems: List<DynamicItem>,
    userError: String?,
): Boolean {
    // The followed timeline is only a window of recent mixed-author content. A local
    // match is useful for immediate paint, but never proves that the user's space feed
    // is complete (it may contain just one video while their opus/articles are older).
    if (nextUid != previousUid) return true
    if (!userError.isNullOrBlank()) return true
    return currentItems.isEmpty()
}

internal fun shouldReloadSelectedUserDynamics(
    previousUid: Long?,
    nextUid: Long,
    currentItems: List<DynamicItem>,
    userError: String?,
): Boolean {
    return shouldAutoLoadSelectedUserDynamics(
        previousUid = previousUid,
        nextUid = nextUid,
        currentItems = currentItems,
        userError = userError,
    )
}
