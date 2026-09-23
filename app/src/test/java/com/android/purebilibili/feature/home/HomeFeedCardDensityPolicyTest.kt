package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedCardDensityPolicyTest {
    @Test
    fun extraLargeEightColumnGrid_usesDenseMetadataWithoutLosingSecondTitleLine() {
        val policy = resolveHomeFeedCardDensityPolicy(
            style = HomeFeedCardStyle.CURRENT,
            gridColumns = 8,
            widthSizeClass = WindowWidthSizeClass.ExtraLarge,
        )

        assertTrue(policy.compactMetadata)
        assertEquals(1, policy.titleMinLines)
        assertEquals(2, policy.titleMaxLines)
        assertTrue(policy.compactStatsOnCover)
    }

    @Test
    fun expandedFiveColumnGrid_doesNotForceDenseMode() {
        val policy = resolveHomeFeedCardDensityPolicy(
            style = HomeFeedCardStyle.CURRENT,
            gridColumns = 5,
            widthSizeClass = WindowWidthSizeClass.Expanded,
        )

        assertFalse(policy.compactMetadata)
        assertEquals(2, policy.titleMinLines)
        assertFalse(policy.compactStatsOnCover)
    }

    @Test
    fun compactOfficialGrid_keepsStyleOwnedCompactMetadata() {
        val policy = resolveHomeFeedCardDensityPolicy(
            style = HomeFeedCardStyle.OFFICIAL,
            gridColumns = 2,
            widthSizeClass = WindowWidthSizeClass.Compact,
        )

        assertTrue(policy.compactMetadata)
        assertEquals(2, policy.titleMinLines)
        assertFalse(policy.compactStatsOnCover)
    }
}
