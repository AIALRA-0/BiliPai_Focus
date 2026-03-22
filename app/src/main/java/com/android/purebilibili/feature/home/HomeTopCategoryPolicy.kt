package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusSettings

private val DEFAULT_HOME_TOP_CATEGORIES = listOf(
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.POPULAR,
    HomeCategory.LIVE,
    HomeCategory.GAME
)

private val HOME_TOP_CUSTOMIZABLE_CATEGORIES = listOf(
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.POPULAR,
    HomeCategory.LIVE,
    HomeCategory.ANIME,
    HomeCategory.GAME,
    HomeCategory.KNOWLEDGE,
    HomeCategory.TECH
)

fun resolveHomeTopTabId(category: HomeCategory): String = category.name

private fun resolveHomeTopCategoryById(id: String): HomeCategory? {
    val normalized = id.trim().uppercase()
    val category = HomeCategory.entries.find { it.name == normalized } ?: return null
    return category.takeIf { it in HOME_TOP_CUSTOMIZABLE_CATEGORIES }
}

fun resolveDefaultHomeTopTabIds(): List<String> {
    return DEFAULT_HOME_TOP_CATEGORIES.map(::resolveHomeTopTabId)
}

fun resolveHomeTopCategories(
    customOrderIds: List<String>? = null,
    visibleIds: Set<String>? = null
): List<HomeCategory> {
    if (customOrderIds == null && visibleIds == null) {
        return DEFAULT_HOME_TOP_CATEGORIES
    }

    val resolvedVisible = visibleIds
        ?.mapNotNull(::resolveHomeTopCategoryById)
        ?.toSet()
        .orEmpty()
    val effectiveVisible = if (resolvedVisible.isEmpty()) {
        DEFAULT_HOME_TOP_CATEGORIES.toSet()
    } else {
        resolvedVisible
    }

    val resolvedOrder = customOrderIds
        ?.mapNotNull(::resolveHomeTopCategoryById)
        .orEmpty()

    val ordered = linkedSetOf<HomeCategory>()
    resolvedOrder.forEach { category ->
        if (category in effectiveVisible) ordered += category
    }
    DEFAULT_HOME_TOP_CATEGORIES.forEach { category ->
        if (category in effectiveVisible) ordered += category
    }
    HOME_TOP_CUSTOMIZABLE_CATEGORIES.forEach { category ->
        if (category in effectiveVisible) ordered += category
    }

    if (ordered.isEmpty()) return DEFAULT_HOME_TOP_CATEGORIES
    return ordered.toList()
}

private fun isFocusHomeCategoryVisible(
    category: HomeCategory,
    settings: FocusSettings
): Boolean = when (category) {
    HomeCategory.RECOMMEND -> settings.showHomeRecommendTab
    HomeCategory.POPULAR -> settings.showHomePopularTab
    HomeCategory.LIVE -> settings.showHomeLiveTab
    HomeCategory.GAME -> settings.showHomeGameTab
    HomeCategory.FOLLOW,
    HomeCategory.ANIME,
    HomeCategory.KNOWLEDGE,
    HomeCategory.TECH -> true
}

fun applyFocusHomeTopCategories(
    categories: List<HomeCategory>,
    settings: FocusSettings,
    fallbackCategory: HomeCategory = HomeCategory.FOLLOW
): List<HomeCategory> {
    val filtered = categories.filter { category ->
        isFocusHomeCategoryVisible(category, settings)
    }
    if (filtered.isNotEmpty()) return filtered
    return listOf(fallbackCategory)
}

fun resolveHomeTopTabIndex(
    category: HomeCategory,
    topCategories: List<HomeCategory> = resolveHomeTopCategories()
): Int {
    return topCategories.indexOf(category).takeIf { it >= 0 } ?: 0
}

fun resolveHomeCategoryForTopTab(
    index: Int,
    topCategories: List<HomeCategory> = resolveHomeTopCategories()
): HomeCategory {
    val safeCategories = if (topCategories.isEmpty()) DEFAULT_HOME_TOP_CATEGORIES else topCategories
    return safeCategories.getOrNull(index) ?: safeCategories.first()
}

fun resolveHomeTopCategoryOrNull(
    topCategories: List<HomeCategory>,
    index: Int
): HomeCategory? {
    if (topCategories.isEmpty()) return null
    return topCategories.getOrNull(index)
}

fun resolveHomeTopCategoryKey(
    topCategories: List<HomeCategory>,
    index: Int
): Int {
    return resolveHomeTopCategoryOrNull(topCategories, index)?.ordinal ?: index
}

