package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopicDetailVisualPolicyTest {

    @Test
    fun miuixWithoutLiquidGlassUsesCompactButton() {
        assertEquals(
            TopicParticipateChrome.MIUIX_COMPACT_BUTTON,
            resolveTopicParticipateChrome(
                uiStyle = AppUiStyle.MIUIX,
                liquidGlassEnabled = false,
            )
        )
        assertEquals(148, TOPIC_PARTICIPATE_BUTTON_WIDTH_DP)
    }

    @Test
    fun liquidGlassOverridesThemeWithReusableDock() {
        AppUiStyle.entries.forEach { uiStyle ->
            assertEquals(
                TopicParticipateChrome.LIQUID_GLASS_DOCK,
                resolveTopicParticipateChrome(uiStyle, liquidGlassEnabled = true),
            )
        }
    }

    @Test
    fun sortControlNeverExpandsBeyondFourCompactItems() {
        assertEquals(144, resolveTopicSortControlWidthDp(optionCount = 2))
        assertEquals(288, resolveTopicSortControlWidthDp(optionCount = 8))
    }

    @Test
    fun topicTagCapsuleGeometryTokensAreDefined() {
        assertEquals(54, TOPIC_TAG_CAPSULE_SIZE_DP)
        assertEquals(28, TOPIC_TAG_CAPSULE_ICON_SIZE_DP)
    }

    @Test
    fun skeletonOnlyAppearsForTheFirstEmptyLoad() {
        assertTrue(
            shouldShowTopicInitialSkeleton(
                isLoading = true,
                hasDetails = false,
                itemCount = 0,
            )
        )
        assertFalse(
            shouldShowTopicInitialSkeleton(
                isLoading = true,
                hasDetails = true,
                itemCount = 12,
            )
        )
    }
}
