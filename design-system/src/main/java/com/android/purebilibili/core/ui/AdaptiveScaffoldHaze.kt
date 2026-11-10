package com.android.purebilibili.core.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import com.android.purebilibili.core.ui.blur.BlurStyles

/**
 * 共享脚手架的顶栏模糊句柄。state 的创建、后台门控与回前台重建策略由 app 层
 * 通过 [LocalAdaptiveScaffoldHazeFactory] 注入；design-system 只负责采集与消费。
 */
class AdaptiveScaffoldHazeHandle(
    val state: HazeState,
    /** 回复「当前是否允许真实模糊」；进后台等场景为 false，模糊样式降级为仅染色。 */
    val blurEnabled: State<Boolean>,
)

/** app 层注入工厂；默认返回 null 表示该环境不提供模糊能力（预览、测试等）。 */
val LocalAdaptiveScaffoldHazeFactory = staticCompositionLocalOf<
    @Composable (userEnabled: Boolean) -> AdaptiveScaffoldHazeHandle?
    > { { null } }

/** 由 [AdaptiveScaffold] 提供给当前页顶部 chrome 的句柄。 */
val LocalAdaptiveScaffoldHaze = staticCompositionLocalOf<AdaptiveScaffoldHazeHandle?> { null }

internal fun shouldAllowScaffoldHazeEffect(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S

@Composable
internal fun rememberAdaptiveScaffoldHaze(userEnabled: Boolean): AdaptiveScaffoldHazeHandle? {
    if (!userEnabled || !shouldAllowScaffoldHazeEffect(Build.VERSION.SDK_INT)) return null
    return LocalAdaptiveScaffoldHazeFactory.current(userEnabled)
}

/**
 * 顶栏模糊：句柄存在时把模糊背景绘制在顶栏节点内，容器色由调用方置为透明。
 * 固定 Quality 档——顶栏面积小，滚动中降采样反而引入明暗波动。
 */
@Composable
internal fun Modifier.adaptiveTopBarHazeBlur(handle: AdaptiveScaffoldHazeHandle): Modifier {
    val gate = handle.blurEnabled.value
    val config = LocalAppThemeConfig.current
    val baseStyle = BlurStyles.getBlurStyle(config.blurIntensity)
    val style = remember(baseStyle, gate) {
        HazeBlurStyle { blurEnabled(gate) }.then(baseStyle)
    }
    // 脚手架先绘制正文、再绘制顶栏，背景与消费节点属于同一窗口。
    // 官方背景渲染与玻璃材质开关独立，不改变各主题的布局与材质参数。
    // 渲染器负责版本及硬件检查，失败时回退同一份正文录制。
    val useWindowBackdrop = gate
    val input = remember(handle.state, useWindowBackdrop) {
        val source = HazeInput.Sources(handle.state)
        if (useWindowBackdrop) HazeInput.Backdrop(fallback = source) else source
    }
    return hazeBlur(
        input = input,
        style = style,
        performanceMode = HazePerformanceMode.Quality,
    )
}
