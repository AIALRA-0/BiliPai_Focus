package com.android.purebilibili.feature.aicu

internal const val AICU_DISCLAIMER_VERSION = 1
internal const val AICU_DISCLAIMER_DELAY_MS = 5000L
internal const val AICU_DISCLAIMER_TEXT = "查询数据由 Aicu 提供，可能不完整、过时或错误，不代表 BiliPai 的判断。\n\n" +
    "查询会将目标 UID 和筛选条件发送给 Aicu，不发送你的 B 站登录凭据。\n\n" +
    "请勿将查询结果用于骚扰、曝光私人信息或其他侵害权益的行为。\n\n" +
    "服务可能排队、限流或不可用，查询结果不保证持续可获取。"

/** Counts only visible foreground time. Retained by the ViewModel, never persisted. */
internal class AicuConsentTimer(private val nowMs: () -> Long) {
    private var visibleSince: Long? = null
    private var elapsedMs = 0L
    fun setVisible(visible: Boolean) {
        if (visible && visibleSince == null) visibleSince = nowMs()
        if (!visible) {
            visibleSince?.let { elapsedMs += (nowMs() - it).coerceAtLeast(0) }
            visibleSince = null
        }
    }
    fun remainingMs(): Long = (AICU_DISCLAIMER_DELAY_MS - elapsedMs -
        (visibleSince?.let { (nowMs() - it).coerceAtLeast(0) } ?: 0L)).coerceAtLeast(0)
    fun remainingSeconds(): Int = ((remainingMs() + 999) / 1000).toInt()
}

internal fun shouldUseAicuLiquidTabs(
    enabled: Boolean, sdkInt: Int, availableWidthDp: Float, fontScale: Float, hasBackdrop: Boolean,
): Boolean = enabled && sdkInt >= 33 && hasBackdrop && fontScale <= 1.3f &&
    availableWidthDp >= 300f * fontScale.coerceAtLeast(1f)
