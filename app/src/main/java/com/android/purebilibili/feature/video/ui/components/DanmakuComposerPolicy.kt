package com.android.purebilibili.feature.video.ui.components

internal const val DANMAKU_SEND_VIP_GRADUAL_COLOR = -1

internal data class DanmakuSendOption<T>(
    val value: T,
    val label: String
)

internal fun shouldUseInlineDanmakuComposer(isFullscreenMode: Boolean): Boolean = isFullscreenMode

/**
 * Stretching control-bar send field. Short-wide cover screens (Pura X Max outer
 * 1848×1264 ≈ 616dp landscape) already enable 分集 at 600dp, so a 480dp gate left
 * only a sliver for `weight(1f)` — a vertical capsule. Require enough width that
 * the pill stays clearly horizontal.
 */
internal const val DANMAKU_CONTROL_BAR_INLINE_INPUT_MIN_WIDTH_DP = 720

/** Measured slot must be at least this wide or the capsule reads as a vertical bar. */
internal const val DANMAKU_CONTROL_BAR_INPUT_CAPSULE_MIN_WIDTH_DP = 96

internal const val DANMAKU_CONTROL_BAR_COMPACT_SEND_MAX_WIDTH_DP = 480

internal fun shouldShowDanmakuInputInControlBar(
    isFullscreen: Boolean,
    widthDp: Int
): Boolean = isFullscreen && widthDp >= DANMAKU_CONTROL_BAR_INLINE_INPUT_MIN_WIDTH_DP

internal fun shouldDrawDanmakuInputCapsule(availableWidthDp: Int): Boolean =
    availableWidthDp >= DANMAKU_CONTROL_BAR_INPUT_CAPSULE_MIN_WIDTH_DP

internal fun shouldShowCompactDanmakuSendAction(
    isFullscreen: Boolean,
    widthDp: Int
): Boolean {
    // Narrow fullscreen keeps a compact send chip; tablet inline cinema
    // only exposes it when there is plenty of room (≥720dp).
    // Tablet split-screen (typically 500-650dp) already has a "发弹幕" button
    // in the UP info section below the player, so keep the player control bar clean.
    return (isFullscreen && widthDp < DANMAKU_CONTROL_BAR_COMPACT_SEND_MAX_WIDTH_DP) ||
        (!isFullscreen && widthDp >= 720)
}

internal fun shouldShowDanmakuSendInMoreActions(
    isFullscreen: Boolean,
    showInlineDanmakuInput: Boolean
): Boolean = isFullscreen && !showInlineDanmakuInput

internal fun resolveDanmakuInputPlaceholder(isLoggedIn: Boolean): String {
    return if (isLoggedIn) {
        "发个友善的弹幕见证当下"
    } else {
        "登录后发送弹幕"
    }
}

internal fun resolveDanmakuComposerLoginBlockedMessage(): String = "请先登录后再发送弹幕"

internal fun danmakuSendColorOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(16777215, "白色"),
    DanmakuSendOption(16646914, "红色"),
    DanmakuSendOption(16740868, "橙色"),
    DanmakuSendOption(16755202, "金色"),
    DanmakuSendOption(52224, "绿色"),
    DanmakuSendOption(41430, "蓝色"),
    DanmakuSendOption(13369971, "紫色"),
    DanmakuSendOption(2236962, "黑色"),
    DanmakuSendOption(DANMAKU_SEND_VIP_GRADUAL_COLOR, "会员渐变")
)

internal fun danmakuSendModeOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(1, "滚动"),
    DanmakuSendOption(5, "顶部"),
    DanmakuSendOption(4, "底部")
)

internal fun danmakuSendFontSizeOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(18, "小"),
    DanmakuSendOption(25, "中"),
    DanmakuSendOption(36, "大")
)