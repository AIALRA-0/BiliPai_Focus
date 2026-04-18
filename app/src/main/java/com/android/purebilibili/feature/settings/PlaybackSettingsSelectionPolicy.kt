package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.FullscreenAspectRatio
import com.android.purebilibili.core.store.FullscreenMode
import com.android.purebilibili.core.store.SettingsManager

internal data class PlaybackSegmentOption<T>(
    val value: T,
    val label: String
)

internal fun <T> resolveSelectionIndex(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T
): Int {
    if (options.isEmpty()) return 0
    val index = options.indexOfFirst { it.value == selectedValue }
    return if (index >= 0) index else 0
}

internal fun <T> resolveSelectionLabel(
    options: List<PlaybackSegmentOption<T>>,
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

internal fun resolveDefaultPlaybackQualityOptions(): List<PlaybackSegmentOption<Int>> {
    return listOf(
        PlaybackSegmentOption(125, "4K HDR"),
        PlaybackSegmentOption(116, "1080P60"),
        PlaybackSegmentOption(80, "1080P"),
        PlaybackSegmentOption(64, "720P"),
        PlaybackSegmentOption(32, "480P"),
        PlaybackSegmentOption(16, "360P")
    )
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
): List<PlaybackSegmentOption<SettingsManager.FeedApiType>> {
    return entries.map { type ->
        PlaybackSegmentOption(
            value = type,
            label = type.label
        )
    }
}

internal fun resolveFullscreenModeSegmentOptions(): List<PlaybackSegmentOption<FullscreenMode>> {
    return listOf(
        PlaybackSegmentOption(FullscreenMode.AUTO, "自动"),
        PlaybackSegmentOption(FullscreenMode.NONE, "不改"),
        PlaybackSegmentOption(FullscreenMode.VERTICAL, "竖屏"),
        PlaybackSegmentOption(FullscreenMode.HORIZONTAL, "横屏")
    )
}

internal fun resolveFullscreenAspectRatioSegmentOptions(): List<PlaybackSegmentOption<FullscreenAspectRatio>> {
    return listOf(
        PlaybackSegmentOption(FullscreenAspectRatio.FIT, "适应"),
        PlaybackSegmentOption(FullscreenAspectRatio.FILL, "填充"),
        PlaybackSegmentOption(FullscreenAspectRatio.RATIO_16_9, "16:9"),
        PlaybackSegmentOption(FullscreenAspectRatio.RATIO_4_3, "4:3"),
        PlaybackSegmentOption(FullscreenAspectRatio.STRETCH, "拉伸")
    )
}
