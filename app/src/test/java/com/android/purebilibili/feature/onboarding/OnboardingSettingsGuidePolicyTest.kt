package com.android.purebilibili.feature.onboarding

import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingSettingsGuidePolicyTest {

    @Test
    fun recommendedProfileAppliesRequestedFirstInstallDefaults() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.RECOMMENDED)

        assertEquals(UiPreset.MD3, preset.uiPreset)
        assertEquals(AndroidNativeVariant.MATERIAL3, preset.androidNativeVariant)
        assertTrue(preset.bottomBarFloating)
        assertFalse(preset.bottomBarLiquidGlassEnabled)
        assertEquals(SettingsManager.TopTabLabelMode.TEXT_ONLY, preset.topTabLabelMode)
        assertEquals(
            listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME", "PARTITION"),
            preset.topTabOrderIds
        )
        assertEquals(preset.topTabOrderIds.toSet(), preset.topTabVisibleIds)
        assertEquals(HomeTopLayoutOrder.SEARCH_THEN_TABS, preset.homeTopLayoutOrder)
    }

    @Test
    fun performanceProfileOnlyDisablesRealtimeTransitionBlur() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.PERFORMANCE)

        assertTrue(preset.cardTransitionEnabled)
        assertFalse(preset.videoTransitionRealtimeBlurEnabled)
        assertFalse(preset.lowQualityHomeCoverInDataSaver)
    }

    @Test
    fun dataSaverProfileUsesLowQualityHomeCoverOnlyInDataSaver() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.DATA_SAVER)

        assertEquals(SettingsManager.DataSaverMode.MOBILE_ONLY, preset.dataSaverMode)
        assertTrue(preset.lowQualityHomeCoverInDataSaver)
        assertTrue(preset.videoTransitionRealtimeBlurEnabled)
    }
}
