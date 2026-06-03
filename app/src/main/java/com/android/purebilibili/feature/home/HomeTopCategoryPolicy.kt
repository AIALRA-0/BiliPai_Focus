package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusSettings

const val HOME_TOP_PARTITION_TAB_ID = "PARTITION"
private val LEGACY_DEFAULT_HOME_TOP_TAB_IDS = setOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME")

sealed interface HomeTopTabEntry {
    val id: String

    data class Category(val category: HomeCategory) : HomeTopTabEntry {
        override val id: String = resolveHomeTopTabId(category)
    }

    data object Partition : HomeTopTabEntry {
        override val id: String = HOME_TOP_PARTITION_TAB_ID
    }
}

private val DEFAULT_HOME_TOP_CATEGORIES = listOf(
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.POPULAR,
    HomeCategory.LIVE,
    HomeCategory.GAME
)

private val DEFAULT_HOME_TOP_ENTRIES = DEFAULT_HOME_TOP_CATEGORIES
    .map(HomeTopTabEntry::Category) + HomeTopTabEntry.Partition

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

private fun resolveHomeTopEntryById(id: String): HomeTopTabEntry? {
    val normalized = id.trim().uppercase()
    if (normalized == HOME_TOP_PARTITION_TAB_ID) return HomeTopTabEntry.Partition
    return resolveHomeTopCategoryById(normalized)?.let(HomeTopTabEntry::Category)
}

private fun resolveHomeTopCategoryById(id: String): HomeCategory? {
    val normalized = id.trim().uppercase()
    val category = HomeCategory.entries.find { it.name == normalized } ?: return null
    return category.takeIf { it in HOME_TOP_CUSTOMIZABLE_CATEGORIES }
}

fun resolveDefaultHomeTopTabIds(): List<String> {
    return DEFAULT_HOME_TOP_ENTRIES.map { it.id }
}

fun resolveHomeTopTabEntries(
    customOrderIds: List<String>? = null,
    visibleIds: Set<String>? = null
): List<HomeTopTabEntry> {
    if (customOrderIds == null && visibleIds == null) {
        return DEFAULT_HOME_TOP_ENTRIES
    }

    val normalizedVisibleIds = visibleIds
        ?.map { it.trim().uppercase() }
        ?.filter { it.isNotBlank() }
        ?.toSet()
    val migratedVisibleIds = if (normalizedVisibleIds == LEGACY_DEFAULT_HOME_TOP_TAB_IDS) {
        normalizedVisibleIds + HOME_TOP_PARTITION_TAB_ID
    } else {
        normalizedVisibleIds
    }
    val resolvedVisible = migratedVisibleIds
        ?.mapNotNull(::resolveHomeTopEntryById)
        ?.toSet()
        .orEmpty()
    val effectiveVisible = resolvedVisible.ifEmpty { DEFAULT_HOME_TOP_ENTRIES.toSet() }

    val normalizedOrderIds = customOrderIds
        ?.map { it.trim().uppercase() }
        ?.filter { it.isNotBlank() }
    val migratedOrderIds = if (normalizedOrderIds?.toSet() == LEGACY_DEFAULT_HOME_TOP_TAB_IDS) {
        normalizedOrderIds + HOME_TOP_PARTITION_TAB_ID
    } else {
        normalizedOrderIds
    }

    val resolvedOrder = migratedOrderIds
        ?.mapNotNull(::resolveHomeTopEntryById)
        .orEmpty()

    val customizableEntries = HOME_TOP_CUSTOMIZABLE_CATEGORIES
        .map(HomeTopTabEntry::Category) + HomeTopTabEntry.Partition

    val ordered = linkedSetOf<HomeTopTabEntry>()
    resolvedOrder.forEach { entry ->
        if (entry in effectiveVisible) ordered += entry
    }
    DEFAULT_HOME_TOP_ENTRIES.forEach { entry ->
        if (entry in effectiveVisible) ordered += entry
    }
    customizableEntries.forEach { entry ->
        if (entry in effectiveVisible) ordered += entry
    }

    return ordered.toList().ifEmpty { DEFAULT_HOME_TOP_ENTRIES }
}

fun resolveFocusHomeTopTabEntries(
    entries: List<HomeTopTabEntry>,
    focusSettings: FocusSettings
): List<HomeTopTabEntry> {
    val filtered = entries.filter { entry ->
        when (entry) {
            is HomeTopTabEntry.Category -> when (entry.category) {
                HomeCategory.RECOMMEND -> focusSettings.showHomeRecommendTab
                HomeCategory.FOLLOW -> focusSettings.showHomeFollowTab
                HomeCategory.POPULAR -> focusSettings.showHomePopularTab
                HomeCategory.LIVE -> focusSettings.showHomeLiveTab
                HomeCategory.ANIME -> focusSettings.showHomeAnimeTab
                HomeCategory.GAME -> focusSettings.showHomeGameTab
                HomeCategory.KNOWLEDGE -> focusSettings.showHomeKnowledgeTab
                HomeCategory.TECH -> focusSettings.showHomeTechTab
            }
            HomeTopTabEntry.Partition -> focusSettings.showHomePartitionButton
        }
    }
    return filtered.ifEmpty {
        entries.filterIsInstance<HomeTopTabEntry.Category>()
            .firstOrNull { it.category == HomeCategory.FOLLOW }
            ?.let(::listOf)
            ?: entries.firstOrNull()?.let(::listOf)
            ?: DEFAULT_HOME_TOP_ENTRIES
    }
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
    val effectiveVisible = resolvedVisible.ifEmpty { DEFAULT_HOME_TOP_CATEGORIES.toSet() }

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

    return ordered.toList().ifEmpty { DEFAULT_HOME_TOP_CATEGORIES }
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

fun resolveHomeTopTabEntryOrNull(
    entries: List<HomeTopTabEntry>,
    index: Int
): HomeTopTabEntry? {
    if (entries.isEmpty()) return null
    return entries.getOrNull(index)
}

fun resolveHomeTopTabEntryKey(
    entries: List<HomeTopTabEntry>,
    index: Int
): Int {
    return when (val entry = resolveHomeTopTabEntryOrNull(entries, index)) {
        is HomeTopTabEntry.Category -> entry.category.ordinal
        HomeTopTabEntry.Partition -> HomeCategory.entries.size
        null -> HomeCategory.entries.size + index + 1
    }
}

fun resolveHomeTopTabEntryLabel(entry: HomeTopTabEntry): String {
    return when (entry) {
        is HomeTopTabEntry.Category -> entry.category.label
        HomeTopTabEntry.Partition -> "分区"
    }
}
