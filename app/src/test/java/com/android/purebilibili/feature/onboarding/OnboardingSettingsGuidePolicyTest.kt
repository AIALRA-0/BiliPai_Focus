package com.android.purebilibili.feature.onboarding

import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingSettingsGuidePolicyTest {

    @Test
    fun recommendedProfileAppliesRequestedFirstInstallDefaults() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.RECOMMENDED)

        assertFalse(preset.bottomBarFloating)
        assertFalse(preset.androidNativeLiquidGlassEnabled)
        assertFalse(preset.bottomBarLiquidGlassEnabled)
        assertEquals(SettingsManager.TopTabLabelMode.TEXT_ONLY, preset.topTabLabelMode)
        assertEquals(
            listOf("FOLLOW", "SUBSCRIPTIONS"),
            preset.topTabOrderIds
        )
        assertEquals(preset.topTabOrderIds.toSet(), preset.topTabVisibleIds)
        assertEquals(2, preset.topTabVisibleIds.size)
        assertTrue(OnboardingSettingsProfile.RECOMMENDED.subtitle.contains("关注标签"))
        assertEquals(HomeTopLayoutOrder.SEARCH_THEN_TABS, preset.homeTopLayoutOrder)
    }

    @Test
    fun allProfilesKeepExpensiveTransitionsOptIn() {
        OnboardingSettingsProfile.entries.forEach { profile ->
            val preset = resolveOnboardingSettingsGuidePreset(profile)
            assertFalse(preset.cardTransitionEnabled, profile.name)
            assertFalse(preset.androidNativeLiquidGlassEnabled, profile.name)
            assertFalse(preset.bottomBarLiquidGlassEnabled, profile.name)
        }
    }

    @Test
    fun dataSaverProfileUsesLowQualityHomeCoverOnlyInDataSaver() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.DATA_SAVER)

        assertEquals(SettingsManager.DataSaverMode.MOBILE_ONLY, preset.dataSaverMode)
        assertTrue(preset.lowQualityHomeCoverInDataSaver)
    }
}
