package com.android.bilipai.tv.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * 毛玻璃面板（Apple TV 式）：悬浮在氛围背景/内容之上，背面采样内容做高斯模糊。
 *
 * 用法：根布局经 [LocalTvHazeState] 提供 [HazeState]，被采样的背景层（氛围背景、页面内容）
 * 标记 `Modifier.hazeSource(state)`，根级浮层调用 [tvGlass]。与被采样内容同层的面板必须传
 * `sampleBackdrop = false`（工具栏、设置面板等），`Dialog` 独立窗口采样不到主窗口图层同样置 false。
 * 全局「模糊效果」开关（[LocalTvSimpleEffects]）关闭时，所有表面回退官方 MD3 纯色容器，无描边。
 * 全部 Haze API 调用收敛在本文件，便于对齐上游 API 变化。
 * （按 haze-blur-materials 的 thick 预设复刻同等参数：
 * 24dp 模糊 + 容器色底 + 容器色 tint。）
 */
internal val LocalTvHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Apple TV 式发丝描边：玻璃面板边缘与背景分离（仅模糊开启时使用）。 */
private val GlassBorderAlpha = 0.14f

@Composable
internal fun Modifier.tvGlass(
    shape: Shape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    /** 浮层与被采样内容同层（工具栏/设置面板等）时置 false，避免自采样；根级浮层保持 true。 */
    sampleBackdrop: Boolean = true,
): Modifier {
    val hazeState = LocalTvHazeState.current
    val simpleEffects = LocalTvSimpleEffects.current
    return if (hazeState != null && !simpleEffects && sampleBackdrop) {
        this
            .clip(shape)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    blurRadius(24.dp)
                    backgroundColor(containerColor)
                    colorEffects(
                        listOf(
                            HazeColorEffect.tint(
                                containerColor.copy(alpha = if (containerColor.luminance() >= 0.5f) 0.83f else 0.9f),
                            )
                        )
                    )
                },
            )
            .border(1.dp, Color.White.copy(alpha = GlassBorderAlpha), shape)
    } else {
        // 全局模糊关闭或不可采样时回退官方 MD3 表面：完全不透明容器色、无描边装饰。
        this.background(containerColor, shape)
    }
}
