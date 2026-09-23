package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.FullscreenAspectRatio
import com.android.purebilibili.core.store.FullscreenMode
import com.android.purebilibili.core.store.player.DEFAULT_AUDIO_QUALITY_FOLLOW_LAST
import com.android.purebilibili.core.store.HomeFeedCardWidthPreset
import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TabletCommentPanelWidthPreset
import com.android.purebilibili.core.store.TabletSecondaryDefaultTab
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppTagChipSize
import com.android.purebilibili.feature.screenshot.AppScreenshotCaptureMode
import com.android.purebilibili.feature.screenshot.AppScreenshotGestureMode

internal fun <T> resolveSelectionIndex(
    options: List<AppSegmentOption<T>>,
    selectedValue: T
): Int {
    if (options.isEmpty()) return 0
    val index = options.indexOfFirst { it.value == selectedValue }
    return if (index >= 0) index else 0
}

internal fun <T> resolveSelectionLabel(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    fallbackLabel: String
): String {
    return options.find { it.value == selectedValue }?.label ?: fallbackLabel
}

internal fun resolveEffectiveMobileQuality(
    rawMobileQuality: Int,
    isDataSaverActive: Boolean,
    maxQualityWhenSaverActive: Int = 32
): Int {
    if (!isDataSaverActive) return rawMobileQuality
    return rawMobileQuality.coerceAtMost(maxQualityWhenSaverActive)
}

internal fun resolveSegmentedSwipeTargetIndex(
    currentIndex: Int,
    totalDragPx: Float,
    optionCount: Int,
    thresholdPx: Float = 30f
): Int {
    if (optionCount <= 0) return 0
    val boundedCurrent = currentIndex.coerceIn(0, optionCount - 1)
    return when {
        totalDragPx >= thresholdPx -> (boundedCurrent + 1).coerceAtMost(optionCount - 1)
        totalDragPx <= -thresholdPx -> (boundedCurrent - 1).coerceAtLeast(0)
        else -> boundedCurrent
    }
}

internal fun resolveDefaultPlaybackQualityOptions(): List<AppSegmentOption<Int>> {
    return listOf(
        AppSegmentOption(126, "杜比视界"),
        AppSegmentOption(125, "4K HDR"),
        AppSegmentOption(116, "1080P60"),
        AppSegmentOption(80, "1080P"),
        AppSegmentOption(64, "720P"),
        AppSegmentOption(32, "480P"),
        AppSegmentOption(16, "360P")
    )
}

internal fun resolveDefaultAudioQualityOptions(): List<AppSegmentOption<Int>> {
    return listOf(
        AppSegmentOption(DEFAULT_AUDIO_QUALITY_FOLLOW_LAST, "跟随上次"),
        AppSegmentOption(30251, "Hi-Res 无损"),
        AppSegmentOption(30250, "杜比全景声"),
        AppSegmentOption(-1, "AAC")
    )
}

internal fun normalizeDefaultAudioQualityOption(value: Int): Int {
    return when (value) {
        DEFAULT_AUDIO_QUALITY_FOLLOW_LAST,
        -1,
        30250,
        30251 -> value
        else -> -1
    }
}

internal fun resolveDefaultQualitySubtitle(
    rawQuality: Int,
    fallbackSubtitle: String,
    isLoggedIn: Boolean,
    isVip: Boolean
): String {
    return when {
        !isVip && isLoggedIn && rawQuality > 80 -> "非大会员将自动以 1080P 起播"
        !isLoggedIn && rawQuality > 64 -> "未登录时将自动以 720P 起播"
        else -> fallbackSubtitle
    }
}

internal fun resolveFeedApiSegmentOptions(
    entries: List<SettingsManager.FeedApiType> = SettingsManager.FeedApiType.entries
): List<AppSegmentOption<SettingsManager.FeedApiType>> {
    return entries.map { type ->
        AppSegmentOption(
            value = type,
            label = type.label
        )
    }
}

internal fun resolveFullscreenModeSegmentOptions(): List<AppSegmentOption<FullscreenMode>> {
    return listOf(
        AppSegmentOption(FullscreenMode.AUTO, "自动"),
        AppSegmentOption(FullscreenMode.NONE, "不改"),
        AppSegmentOption(FullscreenMode.VERTICAL, "竖屏"),
        AppSegmentOption(FullscreenMode.HORIZONTAL, "横屏")
    )
}

internal fun resolveFullscreenAspectRatioSegmentOptions(): List<AppSegmentOption<FullscreenAspectRatio>> {
    return listOf(
        AppSegmentOption(FullscreenAspectRatio.FIT, "适应"),
        AppSegmentOption(FullscreenAspectRatio.FILL, "填充"),
        AppSegmentOption(FullscreenAspectRatio.RATIO_16_9, "16:9"),
        AppSegmentOption(FullscreenAspectRatio.RATIO_4_3, "4:3"),
        AppSegmentOption(FullscreenAspectRatio.STRETCH, "拉伸")
    )
}

internal fun resolvePortraitPlayerCollapseModeSegmentOptions(): List<AppSegmentOption<PortraitPlayerCollapseMode>> {
    return listOf(
        AppSegmentOption(PortraitPlayerCollapseMode.OFF, "关闭"),
        AppSegmentOption(PortraitPlayerCollapseMode.INTRO_ONLY, "竖屏"),
        AppSegmentOption(PortraitPlayerCollapseMode.COMMENT_ONLY, "横屏"),
        AppSegmentOption(PortraitPlayerCollapseMode.BOTH, "全部"),
        AppSegmentOption(PortraitPlayerCollapseMode.PAUSED_ONLY, "暂停时")
    )
}

internal fun resolveHomeFeedCardWidthPresetSegmentOptions(): List<AppSegmentOption<HomeFeedCardWidthPreset>> {
    return listOf(
        AppSegmentOption(HomeFeedCardWidthPreset.AUTO, "自动"),
        AppSegmentOption(HomeFeedCardWidthPreset.COMPACT, "紧凑"),
        AppSegmentOption(HomeFeedCardWidthPreset.BALANCED, "均衡"),
        AppSegmentOption(HomeFeedCardWidthPreset.WIDE, "宽"),
        AppSegmentOption(HomeFeedCardWidthPreset.ULTRA_WIDE, "超宽")
    )
}

internal fun resolveTabletCommentPanelWidthSegmentOptions(): List<AppSegmentOption<TabletCommentPanelWidthPreset>> {
    return listOf(
        AppSegmentOption(TabletCommentPanelWidthPreset.COMPACT, "窄"),
        AppSegmentOption(TabletCommentPanelWidthPreset.STANDARD, "标准"),
        AppSegmentOption(TabletCommentPanelWidthPreset.WIDE, "宽"),
        AppSegmentOption(TabletCommentPanelWidthPreset.ULTRA_WIDE, "超宽")
    )
}

internal fun resolveVideoTagSizeSegmentOptions(): List<AppSegmentOption<AppTagChipSize>> {
    return listOf(
        AppSegmentOption(AppTagChipSize.STANDARD, "标准"),
        AppSegmentOption(AppTagChipSize.COMPACT, "紧凑"),
        AppSegmentOption(AppTagChipSize.SMALL, "更小")
    )
}

internal fun resolveTabletSecondaryDefaultTabOptions(): List<AppSegmentOption<TabletSecondaryDefaultTab>> =
    listOf(
        AppSegmentOption(TabletSecondaryDefaultTab.RELATED, "推荐"),
        AppSegmentOption(TabletSecondaryDefaultTab.COMMENTS, "评论"),
    )

internal fun resolveAppScreenshotGestureModeSegmentOptions(): List<AppSegmentOption<AppScreenshotGestureMode>> {
    return listOf(
        AppSegmentOption(AppScreenshotGestureMode.TOP_RIGHT_TWO_FINGER_LONG_PRESS, "右上角"),
        AppSegmentOption(AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN, "三指下滑"),
        AppSegmentOption(AppScreenshotGestureMode.DISABLED, "关闭")
    )
}

internal fun resolveAppScreenshotCaptureModeSegmentOptions(): List<AppSegmentOption<AppScreenshotCaptureMode>> {
    return listOf(
        AppSegmentOption(AppScreenshotCaptureMode.FULL_WINDOW, "全屏"),
        AppSegmentOption(AppScreenshotCaptureMode.SELECT_REGION, "手选")
    )
}
