package com.android.purebilibili.feature.home.components

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Dock 标签的行盒策略：行盒贴近字形，等宽槽位的测量不随所选字体漂移。
 * Tight 模式下，行高小于字体度量时行盒仍按行高收紧（沿用既有槽位测量），
 * 行高大于度量时首末行不再保留行高带来的额外留白；字形自然高度超出行高的
 * 部分不会被裁剪，CJK 与自定义字体均不切顶。
 */
internal val DockLabelLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
    mode = LineHeightStyle.Mode.Tight,
)

/**
 * Dock 槽宽由容器宽度均分,不随系统字体缩放放宽;sp 字号却会随 fontScale 放大,
 * 因此标签渲染尺寸在 fontScale 超过 [DOCK_LABEL_FONT_SCALE_CAP] 后封顶
 * (与 TopBar 的 resolveMd3TopTabLayoutVisibleSlots 采用同一 1.15 阈值策略)。
 */
internal const val DOCK_LABEL_FONT_SCALE_CAP = 1.15f

/** 返回 Dock 标签的 TextUnit:fontScale ≤ cap 时等于基准值,超过后按反比抵消放大。 */
private fun dockLabelSp(baseSp: Float, fontScale: Float): TextUnit {
    val safeFontScale = fontScale.coerceAtLeast(0.01f)
    val cappedScale = safeFontScale.coerceAtMost(DOCK_LABEL_FONT_SCALE_CAP)
    return (baseSp * cappedScale / safeFontScale).sp
}

internal fun resolveBottomBarSkinDockLabelFontSize(fontScale: Float = 1f): TextUnit =
    dockLabelSp(baseSp = 12f, fontScale = fontScale)

internal fun resolveBottomBarSkinDockLabelLineHeight(fontScale: Float = 1f): TextUnit =
    dockLabelSp(baseSp = 18f, fontScale = fontScale)

/** 图标+文字：胶囊内当 caption，保持 labelSmall。 */
internal fun resolveFloatingDockIconAndTextLabelFontSize(fontScale: Float = 1f): TextUnit =
    dockLabelSp(baseSp = 11f, fontScale = fontScale)

/**
 * 液态玻璃仅文字：指示器是 52dp 胶囊，labelSmall 会显得空。
 * 15sp 能填满两字标签，四字如「插件中心」仍进得了 68dp 槽。
 */
internal fun resolveFloatingDockTextOnlyLabelFontSize(fontScale: Float = 1f): TextUnit =
    dockLabelSp(baseSp = 15f, fontScale = fontScale)

/** Keep top-dock line boxes close to their rendered glyphs so icon+label content fits its slot. */
internal fun resolveFloatingDockLabelLineHeight(
    showIcon: Boolean,
    fontScale: Float = 1f,
): TextUnit = dockLabelSp(baseSp = if (showIcon) 14f else 19f, fontScale = fontScale)

internal fun resolveFloatingDockLabelFontSize(
    showIcon: Boolean,
    showText: Boolean,
    fontScale: Float = 1f,
): TextUnit {
    if (!showText) return 0.sp
    return if (showIcon) {
        resolveFloatingDockIconAndTextLabelFontSize(fontScale)
    } else {
        resolveFloatingDockTextOnlyLabelFontSize(fontScale)
    }
}
