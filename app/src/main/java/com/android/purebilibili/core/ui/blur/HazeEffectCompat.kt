package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * 保留旧的调用形态：给 [hazeBlur] 附加 recoverable 后台门控后再重放 [style]。
 */
@Composable
fun Modifier.hazeEffectCompat(
    state: HazeState,
    style: HazeBlurStyle,
    blurEnabled: Boolean = recoverableBlurEnabled(state),
): Modifier = hazeBlur(
    input = HazeInput.Sources(state),
    style = HazeBlurStyle { blurEnabled(blurEnabled) }.then(style),
)
