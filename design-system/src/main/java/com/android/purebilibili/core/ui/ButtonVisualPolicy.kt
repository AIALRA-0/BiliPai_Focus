package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.opaqueCompositeOver
import com.android.purebilibili.core.theme.calculateContrastRatio

/** 依据 [ColorScheme.surface] 亮度判断当前是否为深色主题。 */
fun isColorSchemeDark(colorScheme: ColorScheme): Boolean =
    colorScheme.surface.luminance() < 0.5f

/** Component appearance follows the app palette, including a manually selected theme. */
@androidx.compose.runtime.Composable
fun isAppInDarkTheme(): Boolean = isColorSchemeDark(androidx.compose.material3.MaterialTheme.colorScheme)

/**
 * Material filled buttons use primary / onPrimary in both themes.
 */
fun resolveFilledButtonContainerColor(colorScheme: ColorScheme): Color =
    colorScheme.primary

/** 填充按钮内容色,与 [resolveFilledButtonContainerColor] 配对。 */
fun resolveFilledButtonContentColor(colorScheme: ColorScheme): Color =
    colorScheme.onPrimary

/** 按钮可独立覆盖底色，文字必须按实际底色检查，不能继续盲用 onPrimary。 */
fun resolveButtonContentColor(
    containerColor: Color,
    contentColor: Color,
    colorScheme: ColorScheme,
): Color {
    // Transparent controls do not own their backdrop (it may be artwork, glass or a card).
    // Both native renderers and glass preserve the supplied foreground here; filled
    // controls still receive contrast correction. Do not change native palette defaults.
    if (containerColor == Color.Unspecified || containerColor.alpha == 0f) return contentColor
    val background = opaqueCompositeOver(containerColor, colorScheme.surface)
    val candidates = listOf(contentColor, colorScheme.onSurface, Color.Black, Color.White)
    return candidates.firstOrNull {
        calculateContrastRatio(opaqueCompositeOver(it, background), background) >= ACCESSIBLE_TEXT_MIN_CONTRAST
    } ?: candidates.maxBy { calculateContrastRatio(opaqueCompositeOver(it, background), background) }
}

/** Keep the native component padding unless the caller explicitly supplies another value. */
fun resolveAppButtonContentPadding(
    uiStyle: AppUiStyle,
    requested: PaddingValues,
    defaultPadding: PaddingValues,
): PaddingValues = requested
