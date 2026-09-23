package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.AppUiStyle

internal enum class TopicParticipateChrome {
    MATERIAL_EXTENDED_FAB,
    MIUIX_COMPACT_BUTTON,
    LIQUID_GLASS_DOCK,
}

internal const val TOPIC_PARTICIPATE_BUTTON_WIDTH_DP = 148
internal const val TOPIC_SORT_ITEM_WIDTH_DP = 72
internal const val TOPIC_TAG_CAPSULE_SIZE_DP = 54
internal const val TOPIC_TAG_CAPSULE_ICON_SIZE_DP = 28

internal fun resolveTopicParticipateChrome(
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean,
): TopicParticipateChrome = when {
    liquidGlassEnabled -> TopicParticipateChrome.LIQUID_GLASS_DOCK
    uiStyle == AppUiStyle.MIUIX -> TopicParticipateChrome.MIUIX_COMPACT_BUTTON
    else -> TopicParticipateChrome.MATERIAL_EXTENDED_FAB
}

internal fun resolveTopicSortControlWidthDp(optionCount: Int): Int =
    TOPIC_SORT_ITEM_WIDTH_DP * optionCount.coerceIn(1, 4)

internal fun shouldShowTopicInitialSkeleton(
    isLoading: Boolean,
    hasDetails: Boolean,
    itemCount: Int,
): Boolean = isLoading && !hasDetails && itemCount == 0
