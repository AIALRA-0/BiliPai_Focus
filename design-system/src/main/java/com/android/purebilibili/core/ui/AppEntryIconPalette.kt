package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.ThemeColors
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSCoral
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSRed
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSYellow

/**
 * 设置页同级入口的多彩图标色板。Miuix 主题按分组内声明顺序轮转取色，
 * Material 3 等预设仍使用中性内容色。
 */
val AppEntryIconPalette: List<Color> = listOf(
    iOSBlue,
    iOSOrange,
    iOSGreen,
    iOSPurple,
    iOSTeal,
    iOSPink,
    ThemeColors[8],
    iOSYellow,
    ThemeColors[22],
    iOSRed,
    iOSCoral,
    ThemeColors[9],
    ThemeColors[5],
    ThemeColors[6],
    ThemeColors[15],
    ThemeColors[17],
)

/** 按声明顺序取色，供没有偏好分组边界的列表手动分配入口色。 */
fun resolveAppEntryIconColor(index: Int): Color {
    require(index >= 0) { "Entry icon index must not be negative" }
    return AppEntryIconPalette[index.mod(AppEntryIconPalette.size)]
}

/**
 * 偏好分组内“中性图标请求”的顺序分配器：进入分组时重建，
 * 组内每个入口按声明顺序取得自己的色板颜色。
 */
@Stable
class AppEntryIconColorCursor {
    private var nextIndex = 0

    internal fun nextColor(): Color {
        val color = AppEntryIconPalette[nextIndex.mod(AppEntryIconPalette.size)]
        nextIndex++
        return color
    }
}

internal val LocalAppEntryIconColorCursor = compositionLocalOf<AppEntryIconColorCursor?> { null }

/** 在偏好分组内启用入口彩色分配（Miuix 生效，其余主题仍走中性语义色）。 */
@Composable
fun ProvideAppEntryIconColors(content: @Composable () -> Unit) {
    val cursor = remember { AppEntryIconColorCursor() }
    CompositionLocalProvider(LocalAppEntryIconColorCursor provides cursor, content = content)
}
