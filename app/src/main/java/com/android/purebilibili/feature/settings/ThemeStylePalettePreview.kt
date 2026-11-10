package com.android.purebilibili.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.ThemePresetPaletteSwatch
import com.android.purebilibili.core.ui.AppSurfaceTokens

/**
 * 色彩风格选项的三色小样。
 *
 * 与设置页色板同一个版式（圆内上半主色、左下第三色、右下主色容器），只是尺寸更小，
 * 用来说明「当前种子色套上这个风格会长什么样」。颜色全部来自已算好的
 * [ThemePresetPaletteSwatch]，不额外依赖主题分支或液态玻璃。
 */
@Composable
internal fun ThemeStyleSwatch(
    swatch: ThemePresetPaletteSwatch,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            // 浅色主题下主色容器可能接近底色，加一圈描边保证小样始终可辨。
            .border(AppSurfaceTokens.OutlineWidth, AppSurfaceTokens.divider(), CircleShape),
    ) {
        Column(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(swatch.primary),
            )
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(swatch.tertiary),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(swatch.primaryContainer),
                )
            }
        }
    }
}
