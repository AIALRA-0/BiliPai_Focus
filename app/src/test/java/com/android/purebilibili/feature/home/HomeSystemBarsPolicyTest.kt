package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSystemBarsPolicyTest {

    @Test
    fun systemBars_areOnlyOwnedWhileHomeIsTopLevel() {
        assertTrue(shouldApplyHomeSystemBars(isTopLevelActive = true))
        assertFalse(shouldApplyHomeSystemBars(isTopLevelActive = false))
    }

    @Test
    fun topSkinArtworkControlsStatusBarIconContrast() {
        assertFalse(resolveHomeStatusBarDarkIcons(true, "dark", true, true))
        assertTrue(resolveHomeStatusBarDarkIcons(true, "light", false, false))
        assertTrue(resolveHomeStatusBarDarkIcons(true, null, true, false))
        assertFalse(resolveHomeStatusBarDarkIcons(false, "light", true, false))
    }
}
