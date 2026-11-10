package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedGridColumnPreferencePolicyTest {
    @Test
    fun disablingCustomizationRestoresAutomaticLayoutWithoutLosingSavedValues() {
        val saved = HomeSettings(gridColumnCount = 6, gridColumnCountCompact = 3)
        val disabled = saved.copy(gridColumnCustomizationEnabled = false)
        assertEquals(0, disabled.effectiveGridColumnCount)
        assertEquals(0, disabled.effectiveGridColumnCountCompact)
        assertFalse(disabled.gridColumnPinchEnabled)
        assertEquals(2, resolveFeedGridColumnPreference(false, 2, 4, 1..4))
        val restored = disabled.copy(gridColumnCustomizationEnabled = true)
        assertEquals(6, restored.effectiveGridColumnCount)
        assertEquals(3, restored.effectiveGridColumnCountCompact)
        assertTrue(restored.gridColumnPinchEnabled)
    }

    @Test
    fun disablingPinchKeepsManualColumnPreferencesAvailable() {
        val settings = HomeSettings(gridColumnCountCompact = 3, pinchToChangeGridColumnsEnabled = false)
        assertEquals(3, settings.effectiveGridColumnCountCompact)
        assertFalse(settings.gridColumnPinchEnabled)
    }

    @Test
    fun changingWindowUsesItsOwnSavedCountAndLimitsToAvailableWidth() {
        val settings = HomeSettings(gridColumnCount = 7, gridColumnCountCompact = 4)
        val narrow = resolveHomeFeedStoredColumnCount(
            WindowWidthSizeClass.Compact, settings.gridColumnCountCompact, settings.gridColumnCount,
        )
        val wide = resolveHomeFeedStoredColumnCount(
            WindowWidthSizeClass.Expanded, settings.gridColumnCountCompact, settings.gridColumnCount,
        )
        assertEquals(3, resolveFeedGridColumnPreference(true, narrow, null, 1..3))
        assertEquals(7, resolveFeedGridColumnPreference(true, wide, null, 2..7))
        assertEquals(4, settings.gridColumnCountCompact)
    }

    @Test
    fun pluginPosterGridRetainsItsMinimumWhenSharedCountIsOne() {
        assertEquals(2, resolveFeedGridColumnPreference(true, 1, null, 2..4, minimumColumns = 2))
        assertEquals(4, resolveFeedGridColumnPreference(true, 7, null, 2..4, minimumColumns = 2))
    }

    @Test
    fun resizingTemporarilyLimitsBothDefaultAndCustomColumnsWithoutChangingPreference() {
        val widths = listOf(412, 340, 220, 340, 412)
        for (customized in listOf(false, true)) {
            val columns = widths.map { width ->
                resolveFeedGridColumnPreference(
                    customizationEnabled = customized,
                    defaultColumns = if (customized) 4 else 2,
                    pageColumns = if (customized) 4 else null,
                    bounds = resolveHomeFeedPinchColumnBounds(WindowWidthSizeClass.Compact, width),
                )
            }
            assertEquals(if (customized) listOf(4, 2, 1, 2, 4) else listOf(2, 2, 1, 2, 2), columns)
        }
    }

    @Test
    fun narrowWindowMaximumOverridesMinimumWithoutInvalidClampRange() {
        assertEquals(1, resolveFeedGridColumnPreference(true, 4, null, 1..1, minimumColumns = 2))
    }

    @Test
    fun singleColumnPresentationIgnoresEarlierGridGesture() {
        assertEquals(1, resolveFeedGridColumnPreference(false, 1, 4, 1..2))
        assertEquals(2, resolveFeedGridColumnPreference(false, 2, 4, 1..2))
    }
}
