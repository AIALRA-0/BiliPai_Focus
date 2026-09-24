package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobalWallpaperDataSaverPolicyTest {

    @Test
    fun offModeNeverLimitsWallpaperLoading() {
        assertFalse(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.OFF, false))
        assertFalse(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.OFF, true))
    }

    @Test
    fun alwaysModeLimitsWallpaperLoadingOnEveryNetwork() {
        assertTrue(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.ALWAYS, false))
        assertTrue(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.ALWAYS, true))
    }

    @Test
    fun mobileOnlyModeFollowsCellularTransport() {
        assertFalse(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.MOBILE_ONLY, false))
        assertTrue(resolveGlobalWallpaperDataSaverActive(SettingsManager.DataSaverMode.MOBILE_ONLY, true))
    }
}
