package com.android.purebilibili.feature.dynamic

internal data class DynamicTabSpec(
    val id: String,
    val title: String,
    val logicalIndex: Int
)

internal val allDynamicTabSpecs: List<DynamicTabSpec> = listOf(
    DynamicTabSpec(id = "all", title = "全部", logicalIndex = 0),
    DynamicTabSpec(id = "video", title = "投稿", logicalIndex = 1),
    DynamicTabSpec(id = "pgc", title = "番剧", logicalIndex = 2),
    DynamicTabSpec(id = "article", title = "专栏", logicalIndex = 3),
    DynamicTabSpec(id = "up", title = "UP", logicalIndex = 4)
)

internal val defaultDynamicTabVisibleIds: Set<String> = allDynamicTabSpecs.map { it.id }.toSet()

internal fun resolveDynamicVisibleTabs(
    visibleTabIds: Set<String>
): List<DynamicTabSpec> {
    val visibleTabs = allDynamicTabSpecs.filter { it.id in visibleTabIds }
    return if (visibleTabs.isNotEmpty()) {
        visibleTabs
    } else {
        listOf(allDynamicTabSpecs.first())
    }
}

internal fun resolveDynamicSelectedTabWithinVisibleTabs(
    selectedTab: Int,
    visibleTabs: List<DynamicTabSpec>
): Int {
    if (visibleTabs.isEmpty()) return 0
    return visibleTabs.firstOrNull { it.logicalIndex == selectedTab }?.logicalIndex
        ?: visibleTabs.first().logicalIndex
}

internal fun resolveDynamicSelectedVisibleTabIndex(
    selectedTab: Int,
    visibleTabs: List<DynamicTabSpec>
): Int {
    if (visibleTabs.isEmpty()) return 0
    val visibleIndex = visibleTabs.indexOfFirst { it.logicalIndex == selectedTab }
    return if (visibleIndex >= 0) visibleIndex else 0
}

internal fun resolveDynamicVisibleTabIdsAfterToggle(
    currentVisibleTabIds: Set<String>,
    targetTabId: String
): Set<String> {
    val normalizedCurrent = currentVisibleTabIds
        .filter { id -> allDynamicTabSpecs.any { it.id == id } }
        .toSet()
        .ifEmpty { defaultDynamicTabVisibleIds }

    return if (targetTabId in normalizedCurrent) {
        if (normalizedCurrent.size <= 1) {
            normalizedCurrent
        } else {
            normalizedCurrent - targetTabId
        }
    } else {
        normalizedCurrent + targetTabId
    }
}

internal fun shouldAllowDynamicTabVisibilityToggleOff(
    currentVisibleTabIds: Set<String>,
    targetTabId: String
): Boolean {
    return !(targetTabId in currentVisibleTabIds && currentVisibleTabIds.size <= 1)
}

internal fun isDynamicUserTabVisible(
    visibleTabs: List<DynamicTabSpec>
): Boolean {
    return visibleTabs.any { it.logicalIndex == 4 }
}
