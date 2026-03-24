package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenPolicyTest {

    @Test
    fun blockedListSubscreen_consumesBackBeforeLeavingSettings() {
        assertTrue(shouldConsumeSettingsBack(showBlockedList = true))
        assertFalse(shouldConsumeSettingsBack(showBlockedList = false))
    }

    @Test
    fun topLevelSettings_bottomPaddingIncludesVisibleBottomBarHeight() {
        val padding = resolveSettingsContentBottomPadding(
            navigationBarsBottom = 16.dp,
            bottomBarVisible = true,
            isBottomBarFloating = true,
            bottomBarLabelMode = 0,
            isTablet = false
        )

        assertEquals(142.dp, padding)
    }

    @Test
    fun secondarySettingsLayout_keepsLegacyBottomPaddingWhenBottomBarHidden() {
        val padding = resolveSettingsContentBottomPadding(
            navigationBarsBottom = 16.dp,
            bottomBarVisible = false,
            isBottomBarFloating = true,
            bottomBarLabelMode = 0,
            isTablet = false
        )

        assertEquals(44.dp, padding)
    }
}
