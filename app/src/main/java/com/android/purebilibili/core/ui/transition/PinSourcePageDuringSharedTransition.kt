package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.LocalSharedTransitionScope

/**
 * 在共享过渡 / 预测返回 seek 期间，将来源页钉在 lookahead 位置，
 * 避免整页被 AnimatedContent / SharedTransition 布局带动产生横向位移。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.pinSourcePageDuringSharedTransition(): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    return with(sharedTransitionScope) {
        this@pinSourcePageDuringSharedTransition.skipToLookaheadPosition(
            enabled = { isTransitionActive },
        )
    }
}
