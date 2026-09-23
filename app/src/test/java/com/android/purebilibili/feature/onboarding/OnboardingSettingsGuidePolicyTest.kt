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

        assertTrue(preset.bottomBarFloating)
        assertTrue(preset.androidNativeLiquidGlassEnabled)
        assertTrue(preset.bottomBarLiquidGlassEnabled)
        assertEquals(SettingsManager.TopTabLabelMode.TEXT_ONLY, preset.topTabLabelMode)
        assertEquals(
            listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME"),
            preset.topTabOrderIds
        )
        assertEquals(preset.topTabOrderIds.toSet(), preset.topTabVisibleIds)
        assertEquals(5, preset.topTabVisibleIds.size)
        assertTrue(OnboardingSettingsProfile.RECOMMENDED.subtitle.contains("五个"))
        assertEquals(HomeTopLayoutOrder.SEARCH_THEN_TABS, preset.homeTopLayoutOrder)
    }

    @Test
    fun performanceProfileKeepsCoreVideoTransition() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.PERFORMANCE)

        assertTrue(preset.cardTransitionEnabled)
        assertFalse(preset.lowQualityHomeCoverInDataSaver)
        assertTrue(preset.summaryLines.contains("保留核心视频过渡"))
    }

    @Test
    fun dataSaverProfileUsesLowQualityHomeCoverOnlyInDataSaver() {
        val preset = resolveOnboardingSettingsGuidePreset(OnboardingSettingsProfile.DATA_SAVER)

        assertEquals(SettingsManager.DataSaverMode.MOBILE_ONLY, preset.dataSaverMode)
        assertTrue(preset.lowQualityHomeCoverInDataSaver)
        assertTrue(preset.cardTransitionEnabled)
    }
}
