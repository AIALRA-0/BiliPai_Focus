// 文件路径: feature/video/ui/components/InputOverlayLayoutPolicy.kt
package com.android.purebilibili.feature.video.ui.components

/**
 * 播放页输入弹层（评论、弹幕）在宽窗口下的最大宽度策略。
 *
 * 紧凑窗口保持全宽；Medium 及以上限宽 640dp 并水平居中，与
 * resolveAppModalLayoutSpec 的限宽口径一致，避免输入面板横贯整个平板窗口。
 * 半开折叠时由 HingeSafeInputOverlayHost 先把弹层收进铰链安全区，
 * 本限宽在安全区内继续生效。
 */
internal fun resolveBottomInputOverlayMaxWidthDp(windowWidthDp: Int): Int {
    return if (windowWidthDp < 600) windowWidthDp else 640
}
