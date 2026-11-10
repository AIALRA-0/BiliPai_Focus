package com.android.purebilibili.core.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypographyTokens {
    val ZeroLetterSpacing = 0.sp

    /** 等宽数字（tabular figures），用于统计数、时长等会刷新的数字，避免宽度抖动。 */
    const val TabularNumerals = "tnum"
}

/**
 * 视频卡片信息区文字角色规则（行高倍数、字重、tabular 数字）。
 *
 * 规则在此唯一维护：手机端经 design-system 的 `feedContentTypography` 用 Material 字阶装配，
 * TV 端在 app-tv 用 tv-material 字阶装配同一规则，两端卡片文字保持同一排版结构。
 * 传入的 `base` 由各端自选（10 英尺阅读距离允许不同字号），规则只约束倍数与字重。
 */
object FeedContentTextRoles {

    /** 紧凑卡片标题：行高 1.38。 */
    fun titleCompact(base: TextStyle): TextStyle =
        base.copy(lineHeight = base.fontSize * 1.38f)

    /** 横向卡片标题：行高 1.42、字距 0.3。 */
    fun titleStandard(base: TextStyle): TextStyle = base.copy(
        lineHeight = base.fontSize * 1.42f,
        letterSpacing = 0.3.sp,
    )

    /** 突出标题：SemiBold、行高 1.38。 */
    fun titleProminent(base: TextStyle): TextStyle = base.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeight = base.fontSize * 1.38f,
    )

    /** UP 主名称：行高 1.5。 */
    fun author(base: TextStyle): TextStyle =
        base.copy(lineHeight = base.fontSize * 1.5f)

    /** 统计数：tabular 数字。 */
    fun statistic(base: TextStyle): TextStyle = base.copy(
        fontFeatureSettings = AppTypographyTokens.TabularNumerals,
    )

    /** 封面角标 / pill 文字：Medium 字重 + tabular 数字。 */
    fun coverBadge(base: TextStyle): TextStyle = base.copy(
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = AppTypographyTokens.TabularNumerals,
    )
}
