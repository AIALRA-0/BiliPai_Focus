package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTopContentSpacingPolicyTest {
    @Test
    fun scrollDistanceMatchesSearchVisibilityAndHiddenTabs() {
        assertEquals(59.dp, resolveHomeTopSearchScrollDistance(48.dp, 6.dp, 5.dp, false))
        assertEquals(53.dp, resolveHomeTopSearchScrollDistance(48.dp, 6.dp, 5.dp, true))
        // 顶部搜索已迁到底栏时，旧的额外间距不再留下空收起区段。
        assertEquals(0.dp, resolveHomeTopSearchScrollDistance(0.dp, 0.dp, 5.dp, false))
    }

    @Test
    fun bottomBarSearchRemovesTheHiddenTopSearchReservation() {
        assertEquals(
            HomeTopSearchRowMetrics(0.dp, 0.dp),
            resolveHomeTopSearchRowMetrics(
                configuredHeight = 48.dp,
                configuredTabsSpacing = 6.dp,
                bottomBarSearchEnabled = true,
                hideTopTabs = false,
            ),
        )
    }

    @Test
    fun hiddenTopTabsReserveAvatarAndSettingsWithoutSearchSpacing() {
        assertEquals(
            HomeTopSearchRowMetrics(48.dp, 0.dp),
            resolveHomeTopSearchRowMetrics(
                configuredHeight = 48.dp,
                configuredTabsSpacing = 6.dp,
                bottomBarSearchEnabled = true,
                hideTopTabs = true,
            ),
        )
    }

    @Test
    fun controlsReservationCoversEverySearchAndTabsSwitchCombination() {
        for (bottomSearch in listOf(false, true)) {
            for (keepSearch in listOf(false, true)) {
                for (hideTabs in listOf(false, true)) {
                    val expected = if (!bottomSearch || keepSearch) {
                        HomeTopSearchRowMetrics(48.dp, 6.dp)
                    } else if (hideTabs) {
                        HomeTopSearchRowMetrics(48.dp, 0.dp)
                    } else HomeTopSearchRowMetrics(0.dp, 0.dp)
                    assertEquals(
                        expected,
                        resolveHomeTopSearchRowMetrics(48.dp, 6.dp, bottomSearch, hideTabs, keepSearch),
                    )
                }
            }
        }
    }

    @Test
    fun topSearchReservationRemainsWhenBottomBarSearchIsDisabled() {
        assertEquals(
            HomeTopSearchRowMetrics(48.dp, 6.dp),
            resolveHomeTopSearchRowMetrics(
                configuredHeight = 48.dp,
                configuredTabsSpacing = 6.dp,
                bottomBarSearchEnabled = false,
                hideTopTabs = false,
            ),
        )
    }


    @Test
    fun `md3 non glass tightens the reserved tabs to content padding`() {
        assertEquals(
            12.dp,
            resolveHomeTabsToContentTighteningDp(AppUiStyle.MATERIAL3, liquidGlassEnabled = false),
        )
    }

    @Test
    fun `glass dock and miuix keep the full reservation`() {
        assertEquals(0.dp, resolveHomeTabsToContentTighteningDp(AppUiStyle.MATERIAL3, liquidGlassEnabled = true))
        assertEquals(0.dp, resolveHomeTabsToContentTighteningDp(AppUiStyle.MIUIX, liquidGlassEnabled = false))
        assertEquals(0.dp, resolveHomeTabsToContentTighteningDp(AppUiStyle.MIUIX, liquidGlassEnabled = true))
    }
}
