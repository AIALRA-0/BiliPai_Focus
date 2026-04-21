package com.android.purebilibili.feature.video.danmaku

private const val DANMAKU_WHITE_RGB = 0x00FFFFFF

data class DanmakuTypeFilterSettings(
    val allowScroll: Boolean = true,
    val allowTop: Boolean = true,
    val allowBottom: Boolean = true,
    val allowColorful: Boolean = true,
    val allowSpecial: Boolean = true,
)

fun shouldDisplayStandardDanmaku(
    danmakuType: Int,
    color: Int,
    settings: DanmakuTypeFilterSettings,
    isVipGradualColor: Boolean = false,
): Boolean {
    val typeAllowed = when (danmakuType) {
        4 -> settings.allowBottom
        5 -> settings.allowTop
        else -> settings.allowScroll
    }
    if (!typeAllowed) {
        return false
    }

    if (!settings.allowColorful && isColorfulDanmaku(color, isVipGradualColor)) {
        return false
    }

    return true
}

fun shouldDisplayAdvancedDanmaku(
    color: Int,
    settings: DanmakuTypeFilterSettings,
): Boolean {
    if (!settings.allowSpecial) {
        return false
    }
    if (!settings.allowColorful && isColorfulDanmaku(color)) {
        return false
    }
    return true
}

internal fun shouldDisplayMergedAdvancedDanmaku(
    content: String,
    color: Int,
    settings: DanmakuTypeFilterSettings,
    blockedMatchers: List<DanmakuBlockRuleMatcher>
): Boolean {
    if (!shouldDisplayAdvancedDanmaku(color = color, settings = settings)) {
        return false
    }
    if (shouldBlockDanmakuByMatchers(content = content, matchers = blockedMatchers)) {
        return false
    }
    return true
}

fun isColorfulDanmaku(color: Int, isVipGradualColor: Boolean = false): Boolean {
    if (isVipGradualColor) return true
    val rgb = color and DANMAKU_WHITE_RGB
    return rgb != DANMAKU_WHITE_RGB
}
