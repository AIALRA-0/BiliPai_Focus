package com.android.purebilibili.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 依据 [ColorScheme.surface] 亮度判断当前是否为深色主题。 */
fun isColorSchemeDark(colorScheme: ColorScheme): Boolean =
    colorScheme.surface.luminance() < 0.5f

/**
 * 填充按钮(Filled Button)容器色:深色主题用 primary(标准强调),
 * 浅色主题用 primaryContainer,避免浅色下深种子色 primary 按钮过重。
 */
fun resolveFilledButtonContainerColor(colorScheme: ColorScheme): Color =
    if (isColorSchemeDark(colorScheme)) colorScheme.primary else colorScheme.primaryContainer

/** 填充按钮内容色,与 [resolveFilledButtonContainerColor] 配对。 */
fun resolveFilledButtonContentColor(colorScheme: ColorScheme): Color =
    if (isColorSchemeDark(colorScheme)) colorScheme.onPrimary else colorScheme.onPrimaryContainer
