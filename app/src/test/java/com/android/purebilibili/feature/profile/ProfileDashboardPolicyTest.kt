package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileDashboardPolicyTest {
    @Test
    fun levelProgress_usesCurrentLevelRangeAndClamps() {
        assertEquals(0.5f, resolveProfileLevelProgress(1_000, 1_500, 2_000).progress)
        assertEquals(0f, resolveProfileLevelProgress(1_000, 900, 2_000).progress)
        assertEquals(1f, resolveProfileLevelProgress(1_000, 2_500, 2_000).progress)
    }

    @Test
    fun dashboardShortcutOrder_matchesPersonalCenterInformationHierarchy() {
        assertEquals(
            listOf(
                ProfileDashboardShortcut.DOWNLOAD,
                ProfileDashboardShortcut.HISTORY,
                ProfileDashboardShortcut.SUBSCRIPTION,
                ProfileDashboardShortcut.WATCH_LATER,
            ),
            defaultProfileDashboardShortcuts(),
        )
    }
}
