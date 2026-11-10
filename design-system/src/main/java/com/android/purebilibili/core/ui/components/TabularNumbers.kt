package com.android.purebilibili.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import com.android.purebilibili.core.ui.currentAppTextStyle

private const val TABULAR_NUMBERS = "tnum"

/**
 * 等宽数字（tabular figures）：各数字字形宽度一致，计数在 9→10、99→100 等
 * 进位时文本宽度只按位数增长，徽标与计数条不再随数字内容左右抖动，
 * 也让按当前文本测量的槽位在计数变化时保持稳定。
 * 字体不支持该特性时设置被忽略，行为退回比例数字；与已有特性设置拼接，不整串覆盖。
 */
fun TextStyle.withTabularNumbers(): TextStyle {
    val features = fontFeatureSettings?.trim().orEmpty()
    if (features.isEmpty()) return copy(fontFeatureSettings = TABULAR_NUMBERS)
    if (features.split(Regex("\\s+")).any { it == TABULAR_NUMBERS }) return this
    return copy(fontFeatureSettings = "$features $TABULAR_NUMBERS")
}

/**
 * 当前 UI 路径（MD3/Miuix 各自的正文样式）叠加等宽数字特性，
 * 供徽标、计数等数字随数据变化的文本入口使用；两条路径共用同一入口。
 */
@Composable
@ReadOnlyComposable
fun appTabularNumberTextStyle(): TextStyle = currentAppTextStyle().withTabularNumbers()
