package com.android.purebilibili.feature.onboarding

import android.content.Context
import com.android.purebilibili.core.store.BottomBarSearchAutoExpandMode
import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

enum class OnboardingSettingsProfile(
    val title: String,
    val subtitle: String
) {
    RECOMMENDED(
        title = "推荐默认",
        subtitle = "MD3、悬浮底栏、六个纯文字顶部标签"
    ),
    PERFORMANCE(
        title = "流畅优先",
        subtitle = "减少视觉负担，保留核心过渡"
    ),
    DATA_SAVER(
        title = "省流量",
        subtitle = "移动网络自动省流量，首页封面更克制"
    )
}

data class OnboardingSettingsGuidePreset(
    val profile: OnboardingSettingsProfile,
    val uiPreset: UiPreset,
    val androidNativeVariant: AndroidNativeVariant,
    val bottomBarFloating: Boolean,
    val bottomBarLiquidGlassEnabled: Boolean,
    val bottomBarSearchEnabled: Boolean,
    val topTabLabelMode: Int,
    val topTabOrderIds: List<String>,
    val topTabVisibleIds: Set<String>,
    val homeTopLayoutOrder: HomeTopLayoutOrder,
    val dataSaverMode: SettingsManager.DataSaverMode,
    val lowQualityHomeCoverInDataSaver: Boolean,
    val cardTransitionEnabled: Boolean,
    val videoTransitionRealtimeBlurEnabled: Boolean,
    val summaryLines: List<String>
)

private val DEFAULT_ONBOARDING_TOP_TAB_IDS = listOf(
    "RECOMMEND",
    "FOLLOW",
    "POPULAR",
    "LIVE",
    "GAME",
    "PARTITION"
)

fun resolveOnboardingSettingsGuidePreset(
    profile: OnboardingSettingsProfile
): OnboardingSettingsGuidePreset {
    val sharedSummary = listOf(
        "默认使用 MD3 / Material 3",
        "关闭液态玻璃，开启悬浮底栏",
        "首页顶部标签纯文字显示 6 个"
    )
    return when (profile) {
        OnboardingSettingsProfile.RECOMMENDED -> OnboardingSettingsGuidePreset(
            profile = profile,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            bottomBarFloating = true,
            bottomBarLiquidGlassEnabled = false,
            bottomBarSearchEnabled = false,
            topTabLabelMode = SettingsManager.TopTabLabelMode.TEXT_ONLY,
            topTabOrderIds = DEFAULT_ONBOARDING_TOP_TAB_IDS,
            topTabVisibleIds = DEFAULT_ONBOARDING_TOP_TAB_IDS.toSet(),
            homeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
            dataSaverMode = SettingsManager.DataSaverMode.MOBILE_ONLY,
            lowQualityHomeCoverInDataSaver = false,
            cardTransitionEnabled = true,
            videoTransitionRealtimeBlurEnabled = true,
            summaryLines = sharedSummary
        )

        OnboardingSettingsProfile.PERFORMANCE -> OnboardingSettingsGuidePreset(
            profile = profile,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            bottomBarFloating = true,
            bottomBarLiquidGlassEnabled = false,
            bottomBarSearchEnabled = false,
            topTabLabelMode = SettingsManager.TopTabLabelMode.TEXT_ONLY,
            topTabOrderIds = DEFAULT_ONBOARDING_TOP_TAB_IDS,
            topTabVisibleIds = DEFAULT_ONBOARDING_TOP_TAB_IDS.toSet(),
            homeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
            dataSaverMode = SettingsManager.DataSaverMode.MOBILE_ONLY,
            lowQualityHomeCoverInDataSaver = false,
            cardTransitionEnabled = true,
            videoTransitionRealtimeBlurEnabled = false,
            summaryLines = sharedSummary + "关闭视频转场实时模糊"
        )

        OnboardingSettingsProfile.DATA_SAVER -> OnboardingSettingsGuidePreset(
            profile = profile,
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            bottomBarFloating = true,
            bottomBarLiquidGlassEnabled = false,
            bottomBarSearchEnabled = false,
            topTabLabelMode = SettingsManager.TopTabLabelMode.TEXT_ONLY,
            topTabOrderIds = DEFAULT_ONBOARDING_TOP_TAB_IDS,
            topTabVisibleIds = DEFAULT_ONBOARDING_TOP_TAB_IDS.toSet(),
            homeTopLayoutOrder = HomeTopLayoutOrder.SEARCH_THEN_TABS,
            dataSaverMode = SettingsManager.DataSaverMode.MOBILE_ONLY,
            lowQualityHomeCoverInDataSaver = true,
            cardTransitionEnabled = true,
            videoTransitionRealtimeBlurEnabled = true,
            summaryLines = sharedSummary + "省流量时首页封面使用低清晰度"
        )
    }
}

suspend fun applyOnboardingSettingsGuidePreset(
    context: Context,
    profile: OnboardingSettingsProfile
) {
    val preset = resolveOnboardingSettingsGuidePreset(profile)
    SettingsManager.setUiPreset(context, preset.uiPreset)
    SettingsManager.setAndroidNativeVariant(context, preset.androidNativeVariant)
    SettingsManager.setBottomBarFloating(context, preset.bottomBarFloating)
    SettingsManager.setBottomBarLiquidGlassEnabled(context, preset.bottomBarLiquidGlassEnabled)
    SettingsManager.setBottomBarSearchEnabled(context, preset.bottomBarSearchEnabled)
    SettingsManager.setTopTabLabelMode(context, preset.topTabLabelMode)
    SettingsManager.setTopTabOrder(context, preset.topTabOrderIds)
    SettingsManager.setTopTabVisibleTabs(context, preset.topTabVisibleIds)
    SettingsManager.setHomeTopLayoutOrder(context, preset.homeTopLayoutOrder)
    SettingsManager.setDataSaverMode(context, preset.dataSaverMode)
    SettingsManager.setLowQualityHomeCoverInDataSaver(
        context = context,
        value = preset.lowQualityHomeCoverInDataSaver
    )
    SettingsManager.setCardTransitionEnabled(context, preset.cardTransitionEnabled)
    SettingsManager.setVideoTransitionRealtimeBlurEnabled(
        context = context,
        value = preset.videoTransitionRealtimeBlurEnabled
    )
}
