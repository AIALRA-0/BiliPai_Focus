package com.android.purebilibili.core.store

internal const val MIN_HOME_REFRESH_COUNT = 10
internal const val MAX_HOME_REFRESH_COUNT = 30

internal fun normalizeHomeRefreshCount(count: Int): Int =
    count.coerceIn(MIN_HOME_REFRESH_COUNT, MAX_HOME_REFRESH_COUNT)

data class FocusSettings(
    val enableFollowGroupFiltering: Boolean = true,
    val showVideoRelatedVideosSection: Boolean = false,
    val showHistoryClearAllAction: Boolean = true,
    val showSearchHistorySection: Boolean = false,
)

internal data class LegacyFocusHomeTabVisibility(
    val showRecommend: Boolean = false,
    val showFollow: Boolean = true,
    val showPopular: Boolean = false,
    val showLive: Boolean = false,
    val showAnime: Boolean = true,
    val showGame: Boolean = false,
    val showKnowledge: Boolean = true,
    val showTech: Boolean = true,
    val showPartition: Boolean = false,
)

internal data class FocusHomeTopTabMigrationResult(
    val orderIds: List<String>,
    val visibleIds: Set<String>,
)

/**
 * Collapses the legacy upstream visibility plus Focus filters into one persisted selection.
 * Preserve the old HomeTopCategoryPolicy ordering and empty-result fallback while doing so.
 */
internal fun resolveFocusHomeTopTabMigration(
    orderIds: List<String>?,
    visibleIds: Set<String>?,
    focusSettings: LegacyFocusHomeTabVisibility,
): FocusHomeTopTabMigrationResult {
    val legacyDefaultOrder = listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME")
    val legacyDefaultVisible = legacyDefaultOrder.toSet()
    val supportedIds = setOf(
        "RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "ANIME", "GAME", "KNOWLEDGE", "TECH",
        "PARTITION", "SUBSCRIPTIONS",
    )
    val defaultEntries = legacyDefaultOrder
    val customizableEntries = listOf(
        "RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "ANIME", "GAME", "KNOWLEDGE", "TECH",
        "PARTITION", "SUBSCRIPTIONS",
    )
    val normalizedOrder = (orderIds ?: legacyDefaultOrder)
        .map { it.trim().uppercase() }
        .filter { it in supportedIds }
        .distinct()
    val effectiveOrder = (normalizedOrder + defaultEntries + customizableEntries).distinct()
    val normalizedVisible = (visibleIds ?: legacyDefaultVisible)
        .map { it.trim().uppercase() }
        .filter { it in supportedIds }
        .toSet()
    val cappedLegacyVisible = if (normalizedVisible.size <= MAX_HOME_TOP_TABS) {
        normalizedVisible
    } else {
        normalizedOrder.filter { it in normalizedVisible }.take(MAX_HOME_TOP_TABS).toSet()
    }
    // The previous home resolver treated an empty or invalid configured set as the
    // upstream default, so preserve the screen users actually saw before migration.
    val effectiveVisible = cappedLegacyVisible.ifEmpty { legacyDefaultVisible }
    val oldEntries = effectiveOrder.filter { it in effectiveVisible }
        .ifEmpty { defaultEntries }
    val filtered = oldEntries.filter { id ->
        when (id) {
            "RECOMMEND" -> focusSettings.showRecommend
            "FOLLOW" -> focusSettings.showFollow
            "POPULAR" -> focusSettings.showPopular
            "LIVE" -> focusSettings.showLive
            "ANIME" -> focusSettings.showAnime
            "GAME" -> focusSettings.showGame
            "KNOWLEDGE" -> focusSettings.showKnowledge
            "TECH" -> focusSettings.showTech
            "PARTITION" -> focusSettings.showPartition
            "SUBSCRIPTIONS" -> true
            else -> false
        }
    }
    // Legacy Focus kept one fallback tab visible when all Focus switches were off.
    val actualVisible = filtered.ifEmpty {
        oldEntries.firstOrNull { it == "FOLLOW" }?.let(::listOf)
            ?: oldEntries.firstOrNull()?.let(::listOf)
            ?: defaultEntries
    }
    // The subscription plugin used to append its tab when the upstream list still
    // matched its default. Persist that implicit choice so plugin installation keeps
    // the same behavior after the two visibility layers are unified.
    val withDefaultSubscription = if (cappedLegacyVisible == legacyDefaultVisible) {
        (actualVisible + "SUBSCRIPTIONS").distinct()
    } else {
        actualVisible
    }
    val finalOrder = (effectiveOrder + withDefaultSubscription).distinct()
    val finalVisible = finalOrder.filter { it in withDefaultSubscription }
        .take(MAX_HOME_TOP_TABS)
        .toSet()
    return FocusHomeTopTabMigrationResult(
        orderIds = finalOrder,
        visibleIds = finalVisible.ifEmpty { setOf("FOLLOW") },
    )
}

internal fun resolveFocusHomeTopTabMigrationBoundary(
    orderIds: List<String>?,
    visibleIds: Set<String>?,
    focusSettings: LegacyFocusHomeTabVisibility,
    hasLegacyFocusVisibilitySettings: Boolean,
): FocusHomeTopTabMigrationResult {
    if (orderIds == null && visibleIds == null && !hasLegacyFocusVisibilitySettings) {
        val defaultOrder = DEFAULT_TOP_TAB_ORDER.split(",").filter { it.isNotBlank() }
        val defaultVisible = DEFAULT_TOP_TAB_VISIBLE.split(",").filter { it.isNotBlank() }.toSet()
        return FocusHomeTopTabMigrationResult(
            orderIds = defaultOrder,
            visibleIds = defaultVisible,
        )
    }
    return resolveFocusHomeTopTabMigration(orderIds, visibleIds, focusSettings)
}
